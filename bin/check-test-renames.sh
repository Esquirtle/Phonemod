#!/usr/bin/env bash
# F2 (plan sec.5, R-CORE-6/D-21/D-15): guards that the 25 tests that existed at
# the BASE tag survive the 3.0 extraction with, at most, mechanical renames —
# no behavioural drift smuggled in under a rename.
#
# Usage:
#   bin/check-test-renames.sh [BASE]        # BASE defaults to v2.0.0-alpha.13
#   bin/check-test-renames.sh --self-test
#
# Exit codes: 0 all 25 accounted for and normalized-identical, 1 otherwise.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ALLOW_FILE="$SCRIPT_DIR/test-renames.allow"

normalize() {
    # Drops package/import lines and any esq.phonemod.<...>. FQN prefix
    # immediately before an identifier, so a package move alone diffs clean.
    perl -pe '
        next if /^\s*package\s+[\w.]+\s*;/;
        next if /^\s*import\s+[\w.*]+\s*;/;
        s/\besq(?:\.phonemod)?(?:\.\w+)*\.(?=[A-Z])//g;
    '
}

list_base_tests() {
    local base="$1"
    git -C "$REPO_ROOT" ls-tree -r --name-only "$base" -- src/test/java \
        | grep -E '[A-Za-z0-9_]+Test\.java$'
}

# Finds the current path for a BASE-relative test file path.
# 1. Same path still exists -> use it.
# 2. A rename pair from `git diff -M` (committed) maps it -> use the new path.
# 3. A unique basename exists anywhere under src/test/java (covers
#    uncommitted moves) -> use it.
resolve_current_path() {
    local base="$1" base_path="$2"
    if git -C "$REPO_ROOT" cat-file -e "HEAD:$base_path" 2>/dev/null; then
        echo "$base_path"
        return 0
    fi

    local rename_line
    rename_line="$(git -C "$REPO_ROOT" diff -M --name-status "$base" -- src/test/java \
        | awk -v p="$base_path" '$1 ~ /^R/ && $2 == p {print $3}')"
    if [[ -n "$rename_line" ]]; then
        echo "$rename_line"
        return 0
    fi

    local basename
    basename="$(basename "$base_path")"
    local matches
    matches="$(find "$REPO_ROOT/src/test/java" -name "$basename" 2>/dev/null)"
    local count
    count="$(echo "$matches" | grep -c . || true)"
    if [[ "$count" -eq 1 ]]; then
        echo "${matches#"$REPO_ROOT"/}"
        return 0
    fi
    return 1
}

is_allowed() {
    local base_path="$1"
    [[ -f "$ALLOW_FILE" ]] || return 1
    grep -Fxq "$base_path" "$ALLOW_FILE"
}

run_check() {
    local base="${1:-v2.0.0-alpha.13}"
    cd "$REPO_ROOT"

    local base_tests
    base_tests="$(list_base_tests "$base")"
    local base_count
    base_count="$(echo "$base_tests" | grep -c . || true)"
    if [[ "$base_count" -ne 25 ]]; then
        echo "check-test-renames: expected exactly 25 *Test.java at $base, found $base_count" >&2
        return 1
    fi

    local failures=0
    local mapped=0
    while IFS= read -r base_path; do
        [[ -n "$base_path" ]] || continue
        if is_allowed "$base_path"; then
            mapped=$((mapped + 1))
            continue
        fi
        local current_path
        if ! current_path="$(resolve_current_path "$base" "$base_path")"; then
            echo "check-test-renames: no current file found for $base_path (BASE $base)" >&2
            failures=1
            continue
        fi
        if [[ ! -f "$REPO_ROOT/$current_path" ]]; then
            echo "check-test-renames: resolved path does not exist: $current_path" >&2
            failures=1
            continue
        fi

        local base_norm current_norm
        base_norm="$(git -C "$REPO_ROOT" show "$base:$base_path" | normalize)"
        current_norm="$(normalize <"$REPO_ROOT/$current_path")"
        if ! diff -u <(echo "$base_norm") <(echo "$current_norm") >/tmp/check-test-renames.diff.$$; then
            echo "check-test-renames: $base_path -> $current_path differs beyond package/import/FQN:" >&2
            cat /tmp/check-test-renames.diff.$$ >&2
            rm -f /tmp/check-test-renames.diff.$$
            failures=1
            continue
        fi
        rm -f /tmp/check-test-renames.diff.$$
        mapped=$((mapped + 1))
    done <<<"$base_tests"

    if [[ "$failures" -ne 0 ]]; then
        return 1
    fi
    echo "check-test-renames: $mapped/25 accounted for"
    return 0
}

run_self_test() {
    # list_base_tests / normalize are exercised directly against the real
    # BASE tag — no synthetic repo needed, since the tag is immutable.
    local n
    n="$(list_base_tests v2.0.0-alpha.13 | grep -c .)"
    if [[ "$n" -ne 25 ]]; then
        echo "self-test FAILED: expected 25 base tests, got $n" >&2
        return 1
    fi

    local sample
    sample="$(printf 'package a.b;\nimport c.d.E;\nclass Foo extends esq.phonemod.api.v3.ViewModel {}\n' | normalize)"
    if [[ "$sample" != *"class Foo extends ViewModel {}"* ]]; then
        echo "self-test FAILED: normalize did not strip package/import/FQN as expected: $sample" >&2
        return 1
    fi

    if run_check "nonexistent-tag-$$" >/dev/null 2>&1; then
        echo "self-test FAILED: a bogus BASE tag should fail (git rev lookup error)" >&2
        return 1
    fi

    echo "self-test OK"
    return 0
}

main() {
    if [[ "${1:-}" == "--self-test" ]]; then
        run_self_test
        exit $?
    fi
    run_check "${1:-v2.0.0-alpha.13}"
}

main "$@"
