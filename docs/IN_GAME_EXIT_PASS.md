# Phonemod — In-game exit-pass script (1.1 + Alpha)

The runnable acceptance checklist for the gates that **cannot be closed headlessly**. Every
1.1 automated test is already green (96 JUnit, `mvn -B test`); this document covers only the
live behaviors that need real clients, real voice, real disconnects, and real v1 data.

Cross-references `PROFESSIONALIZATION_PLAN.md` (the "1.1 exit gate", the "Calls and voice"
feature boxes, the "2.0 Alpha exit gate", and the "In-game multiplayer acceptance
checklist"). Record evidence (console lines / screenshots / clips) beside each row.

Build under test: `2.0.0-alpha.12` @ commit `41b803c` (app-open authority + calls/voice
test-harden + persistence/assets + bounded shutdown flush). `Common/` assets are unchanged
from the live jar, so **no client rejoin is required** after deploy — a restart reloads the
server-side logic.

---

## Part 0 — Setup

Prereqs:
- **Two accounts / two clients** (P1, P2) — calls, audio policy, and disconnect cases need both.
- **One admin** (may be P1) with the `/deviceframework` permission; **one non-admin** (P2) to prove permission gating.
- Deploy the build under test to hytest (see the deploy commands at the end / relayed separately) and confirm the boot log shows `Enabled plugin esq.phonemod:Phonemod`.
- Give each player a phone (server console) and confirm it opens.

Legend: **PASS** = observed expected result; **FAIL** = anything else (capture the console/exception). "Closes:" names the plan checkbox the row provides evidence for.

---

## Part 1 — Server-authoritative app-open (forged actions cannot open/mutate an unauthorized app)

> The pure packet-forging cases are already covered by `AppOpenAuthorizerTest` (10 cases).
> In-game we exercise the **same server code path** via the observable authority signals.
> Full packet forgery (arbitrary `open_app` with a bad id) needs a tampered client / packet
> harness — do that only if one is available; otherwise the unit tests stand for it and the
> rows below verify the live enforcement.

| # | Step | Expected | Closes |
|---|---|---|---|
| 1.1 | P1 opens an installed, permitted app | Opens normally | app-open happy path |
| 1.2 | Admin revokes P1's permission for a gated app, P1 tries to open it | Denied message (`phonemod.appstore.launch.denied`), app does **not** open, no session state changes | app-open authority (missing/revoked permission) |
| 1.3 | P1 tries to open an app **not installed** on their device | Rejected server-side, no open | app-open authority (installation) |
| 1.4 | (If a 2nd device type exists, e.g. PC) open a phone-only app on the PC | Rejected as incompatible device | app-open authority (device compatibility) |
| 1.5 | A v2 app declaring **two** permissions; grant only one, try to open | Denied (ALL required permissions must be held) | multi-permission fix |
| 1.6 | *(harness only)* Send a forged `open_app` for a hidden/nonexistent app id | Server rejects, acknowledges safely, **no crash**, no exception leaked to client | forged-action exit-gate |

---

## Part 2 — Calls & voice (two clients)

| # | Step | Expected | Closes |
|---|---|---|---|
| 2.1 | P1 calls P2; P2 answers | Active call; voice audible **both** directions | calls baseline |
| 2.2 | Mid-call, P1 **dismisses** the phone page | Call **stays alive**; voice continues | keep-calls-alive |
| 2.3 | P1 **reopens** the phone | Active-call UI is **restored** (not a dead/empty screen) | restore-on-reopen |
| 2.4 | P1 hangs up | **Both** ends terminate cleanly; UI returns to idle | terminate-on-hangup |
| 2.5 | New call P1→P2; mid-call P2 **disconnects** (closes client) | Call terminates on P1 (no stuck "in call"); P1 can place a new call | terminate-on-disconnect |
| 2.6 | P1 calls P2; P2 never answers | Auto-cancels after the ring timeout (~30s, `callRingTimeoutSeconds`) | terminate-on-timeout |
| 2.7 | **PRIVATE** device: during a call, a bystander P3 stands next to P1 | P3 hears **nothing** | CallAudioPolicy PRIVATE default |
| 2.8 | **SPEAKER** device: repeat 2.7 | P3 (within speaker radius) **hears** the call | CallAudioPolicy SPEAKER |
| 2.9 | **TOGGLEABLE** device: call starts, bystander P3 present | Starts **private** (P3 hears nothing); an **active endpoint** toggles speaker on → P3 now hears; a **non-participant** cannot toggle | CallAudioPolicy TOGGLEABLE + only-active-toggles |
| 2.10 | Mute P1's voice (server `VoiceModule` mute) mid-call | P2 hears nothing from P1; call state stays ACTIVE; unmute restores | muted-voice / voice snapshot |
| 2.11 | P1 tries to call **self**; and P1 already-in-call is called by P3 | Self-call rejected; overlap handled per rules (no double-active) | overlap/self-call (regression) |
| 2.12 | End a call, then trigger plugin/server shutdown | No lingering voice relay, **no exceptions**; boot log clean on next start | retain/deregister packet-filter handle |

---

## Part 3 — Persistence, reload & bounded shutdown

| # | Step | Expected | Closes |
|---|---|---|---|
| 3.1 | P1 messages P2 while P2 is **offline**; P2 logs in | P2 receives the message on join | offline mailbox persistence |
| 3.2 | Change synced state (e.g. a persistent room/channel), **restart** the server | State survives the restart | ordered sync persistence |
| 3.3 | Trigger a save and immediately shut the server down (ideally while a save/backup lock is contended) | Server shuts down **promptly** (no indefinite hang); pending write still lands on disk | **bounded shutdown-flush fix** |
| 3.4 | If an asset-reload path exists, reload device assets at runtime | Device shells refresh (no stale shell/handle); no errors | shell-cache invalidation |

---

## Part 4 — Alpha migration rehearsal (Alpha exit gate — needs copied v1 data)

Run against a **copy** of production-shaped v1 data in a disposable universe (never prod).

1. **Snapshot** the v1 universe copy (so you can prove rollback later).
2. `/deviceframework migrate dry-run` → reports every planned mutation + every conflict; **no data changed** (verify files untouched).
3. Run the real migration → confirm a timestamped `.pre-v2` **backup** is created on first mutation; PhoneNumber→UUID identity assigned; the **first legitimate claimant keeps** the old address; a **duplicate** legacy item gets a new address with its contacts/history/installs/settings remapped (no overwrite of another owner).
4. Run the migration **again** → **no-op**: byte-equivalent data, **no second backup**, no further mutation (idempotency).
5. **Restore** from the `.pre-v2` backup → environment returns to pre-migration state (rollback rehearsal).
6. `/deviceframework status` → `Schema: v2`, `conflicts=0`, `pendingSaves=0`.

Closes: Alpha exit-gate rows (dry-run reports all mutations; full rehearsal without loss; second run no-op; backup restoration).

---

## Part 5 — `/deviceframework` admin gating

| # | Step | Expected | Closes |
|---|---|---|---|
| 5.1 | **Admin** runs `status`, `migrate dry-run`, `conflicts`, `repair`, `transfer`, `release` | All execute; output is audit-logged | admin commands |
| 5.2 | **Non-admin** (P2) runs any `/deviceframework` subcommand | Denied (permission-gated) | admin gating |
| 5.3 | Admin performs an explicit **device transfer** to P2 | P2 gets clean personal state; **P1's history/app-data stays private**; identity UUID stable | ownership transfer |
| 5.4 | P2 tries to use **copied item metadata** to claim P1's device | Cannot steal ownership | ownership integrity |

---

## Evidence to capture

For each PASS: the relevant server console line(s) and/or a short clip (audio-policy and
call-lifecycle rows are best shown on video). For any FAIL: the full exception/stack from the
hytest console (`docker logs hyserver-hytest-1`) and the exact repro. File results back into
`PROFESSIONALIZATION_PLAN.md` beside each checkbox and summarize in
`docs/VERIFICATION_EVIDENCE.md` under a dated "in-game exit pass" section.

Once Parts 1–3 pass, the 1.1 exit gate's multiplayer row is satisfied and the calls/voice
feature boxes can be ticked. Part 4 satisfies the Alpha exit-gate rehearsal rows.
