#!/usr/bin/env bash
# F2 (plan sec.5, R-API-1/R-KIT): asserts that the backend-neutral source trees
# never import a Hytale UI type. Run from the Phonemod-v3 repo root.
#
# Usage:
#   bin/check-api-no-ui-imports.sh [dir...]     # dirs override the default TARGETS
#   bin/check-api-no-ui-imports.sh --self-test
#
# Exit codes: 0 clean, 1 violations found (printed as file:line:pattern),
# 2 a target directory does not exist.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# F2 targets (plan sec.5): the v3 API package (main + test) and the whole
# ui-kit main tree.
TARGETS=(
    "phonemod-api/src/main/java/esq/phonemod/api/v3"
    "phonemod-api/src/test/java/esq/phonemod/api/v3"
    "phonemod-ui-kit/src/main/java"
)

PATTERNS=(
    'com\.hypixel\.hytale\.server\.core\.ui\.'
    'com\.hypixel\.hytale\.server\.core\.modules\.ui\.'
    'protocol\.packets\.interface_\.'
    '\bInteractiveCustomUIPage\b'
    '\bPageManager\b'
)

strip_comments() {
    # Drops // line comments and /* */ block comments (naive but sufficient for
    # a grep-gate over our own small, consistently formatted trees).
    perl -0777 -pe 's{/\*.*?\*/}{}gs; s{//[^\n]*}{}g'
}

check_dir() {
    local dir="$1"
    local violations=0
    while IFS= read -r -d '' file; do
        local stripped
        stripped="$(strip_comments <"$file")"
        local lineno=0
        while IFS= read -r line; do
            lineno=$((lineno + 1))
            # Only import statements: this is an import ban, not a general text
            # scan, so reflective string checks of a UI type's name (e.g. in a
            # contract test) are not false positives.
            [[ "$line" =~ ^[[:space:]]*import[[:space:]] ]] || continue
            for pattern in "${PATTERNS[@]}"; do
                if [[ "$line" =~ $pattern ]]; then
                    echo "$file:$lineno:$pattern"
                    violations=1
                fi
            done
        done <<<"$stripped"
    done < <(find "$dir" -name '*.java' -print0)
    return $violations
}

run_self_test() {
    local tmp
    tmp="$(mktemp -d)"
    trap 'rm -rf "$tmp"' RETURN

    mkdir -p "$tmp/clean" "$tmp/dirty"
    cat >"$tmp/clean/Clean.java" <<'EOF'
package esq.phonemod.api.v3;
// com.hypixel.hytale.server.core.ui.builder.UICommandBuilder — only in a comment
public interface Clean {
    void doThing();
}
EOF
    cat >"$tmp/dirty/Dirty.java" <<'EOF'
package esq.phonemod.api.v3;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
public interface Dirty {
    void doThing(UICommandBuilder b);
}
EOF

    if ! check_dir "$tmp/clean"; then
        echo "self-test FAILED: clean fixture reported a violation" >&2
        return 1
    fi
    if check_dir "$tmp/dirty" >/dev/null; then
        echo "self-test FAILED: dirty fixture reported clean" >&2
        return 1
    fi

    local missing_rc=0
    (cd "$REPO_ROOT" && bash "$SCRIPT_DIR/check-api-no-ui-imports.sh" "does/not/exist-$$") >/dev/null 2>&1 || missing_rc=$?
    if [[ "$missing_rc" -ne 2 ]]; then
        echo "self-test FAILED: missing target directory exited $missing_rc, expected 2" >&2
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

    local targets=("${TARGETS[@]}")
    if [[ $# -gt 0 ]]; then
        targets=("$@")
    fi

    cd "$REPO_ROOT"

    local any_violation=0
    for target in "${targets[@]}"; do
        if [[ ! -d "$target" ]]; then
            echo "check-api-no-ui-imports: target directory does not exist: $target" >&2
            exit 2
        fi
        if ! check_dir "$target"; then
            any_violation=1
        fi
    done

    if [[ "$any_violation" -ne 0 ]]; then
        exit 1
    fi
    echo "check-api-no-ui-imports: clean (${targets[*]})"
}

main "$@"
