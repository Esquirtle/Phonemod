# 2.0.0-alpha verification evidence (current: alpha.12)

Verified through **2026-08-18** against the workspace Hytale server and asset tree with
JDK 25. (Build, test and consumer-compile sections were re-verified on 2026-08-18; the
hytest deployment and asset-validation sections below still date from 2026-08-14/15 and
are unchanged.)

## 1.1 calls + voice test-hardening — 2026-08-21 (implementer workflow)

The "Calls and voice" cluster was found (cited architect audit) to be **already
implemented and wired** in the committed tree; this slice makes its security-relevant
decisions unit-testable without Mockito and locks in current behavior. All three design
questions were answered **keep-current** by the user (RINGING-only call timeout;
caller's-device governs speaker policy; either active endpoint may toggle speaker).

Three new **pure, side-effect-free** helpers, with the existing runtime delegating to them
(behavior-preserving — only `DevicePage.java` and `CallRegistry.java` changed vs HEAD,
`CallSession.java` untouched):
- **`CallRestoreDecision`** — reopen-restore precedence (ACTIVE > pending-outgoing >
  pending-incoming > none); `DevicePage.buildContent` delegates.
- **`VoiceRelayDecision`** — the `VOICE_FILTER` gate ladder (`DROP_NO_ROUTE` /
  `CONSUME_NO_RELAY` / `RELAY_PRIVATE` / `RELAY_PRIVATE_AND_SPEAKER`); the Netty side keeps
  all channel I/O. (Benign deviation: `VoiceModule` mute/size accessors are now evaluated
  eagerly to pass as params — verified pure config reads, identical consume-vs-relay
  result; the `route==null` early-return stays before any `VoiceModule` call.)
- **`VoiceRouteSnapshot`** — the ACTIVE-only, symmetric route topology; `CallRegistry`
  keeps the side-effecting `receiverUuid`→`PlayerRef` wrapping outside the pure helper.

Seven JUnit suites (+28 tests): extended `CallSessionTest` (end reasons, speaker-clear-on-
end, speaker-after-end rejection, partnerOf/contains, speaker-default + who-may-toggle
matrix), new `CallAudioModeTest` (4 modes × 3 policies), `DeviceAssetDefaultsTest` (default
`PRIVATE`), `CallRestoreDecisionTest`, `VoiceRelayDecisionTest`, `VoiceRouteSnapshotTest`
(fake-UUID resolver lambda, no `PlayerRef`), `TokenBucketActionGuardTest`.

Verified: `mvn -B -f Phonemod/pom.xml package` exit 0, **`Tests run: 75, Failures: 0`** (up
from 47). Parity reviewed against HEAD `0a13bc9` — no runtime behavior changed. Packaged
jar booted alone in a fresh universe → `Enabled plugin esq.phonemod:Phonemod` → `Universe
ready!` → `Hytale Server Booted!`, clean shutdown, no plugin-attributable exception.

**Closed:** the `CallAudioPolicy` feature box. **Still open** (in-game exit pass, not
JUnit-reachable): live page open/dismiss/reopen, reconnect, real Netty voice relay +
speaker fan-out, and live packet-filter register/deregister.

## 1.1 app-open authorization — 2026-08-21 (implementer workflow)

Closed the two 1.1 server-authoritative app-open plan items + their authorization test.
Five edits on `feature/v2-refactor` (uncommitted at time of writing):

- **`security/AppOpenAuthorizer`** (new) — a pure, dependency-free decision function
  (`ALLOW / UNKNOWN_APP / INCOMPATIBLE_DEVICE / NOT_OWNER / NOT_INSTALLED /
  MISSING_PERMISSION`), fail-closed, first-failure-wins, order matching the pre-refactor
  inline checks. Empty supported-device set = any device; **all** required permissions must
  be granted.
- **`DevicePage.openApp`** now computes the seven inputs and delegates to the authorizer;
  the `ALLOW` branch falls through to the unchanged build/dispatch path. Behavior-parity
  reviewed against the pre-refactor diff: no session state mutated on any reject path;
  `currentApp`/`AppOpenEvent.Post` only on `ALLOW`; `phonemod.appstore.launch.denied` only
  on `MISSING_PERMISSION`.
- **`PhoneApp.getRequiredPermissions()`** (additive default) + **`DeviceAppAdapter`**
  override returning the full `descriptor.permissions()` set — fixes a real bug where a v2
  app declaring multiple permissions only enforced the first (`.stream().findFirst()`).
- **`AppOpenAuthorizerTest`** (new) — 10 JUnit cases: hidden-UI-grants-nothing, hidden-UI
  legit holder, forged app id, incompatible device, missing ownership, missing permission,
  revoked permission, multi-permission (partial vs full grant), happy path, and
  first-failure-wins order.

Verified in `maven:3.9-eclipse-temurin-25` (JDK 25), `mvn -B test`, exit 0: **`Tests run:
47, Failures: 0, Errors: 0`** (up from 37). The packaged runtime jar booted alone in a
fresh isolated universe and reached `Enabled plugin esq.phonemod:Phonemod` → `Universe
ready!` → `Hytale Server Booted!` with no plugin-attributable exception (regression check
that the `openApp` refactor didn't break registration).

**Still open for these items:** the 1.1 exit-gate **multiplayer forged-action / audio-leak
acceptance pass** (in-game, can't be done headless), and the world-thread-dispatch
**runtime-proof** concurrency test (item is code-complete by cited audit — all 10
target-player ECS ops already hop — but not yet proven by a deterministic test).

## Re-verification — 2026-08-21 (alpha.12)

The version advanced `alpha.1 → alpha.12` since the sections below were written. **Every
commit in that range was phone model/animation iteration** (held chest-hold pose, on-call
ear pose, slimmed upright 30u model, split 1P/3P `_FPS` animation variants) plus the
matching pack-`Version` bumps to bust the client asset cache — see
[[phonemod-anim-wiring]]. **No professionalization-plan gate advanced**; the checkbox
state in `PROFESSIONALIZATION_PLAN.md` is unchanged.

Re-verified today in `maven:3.9-eclipse-temurin-25` (JDK 25), `mvn -B test`, exit 0:

```text
Building Phonemod 2.0.0-alpha.12
Building Phonemod Public API 2.0.0-alpha.12
Tests run: 37, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The 37 tests are the same 13 runtime suites as alpha.1 (no new tests landed with the art
work). **The reproducible-JAR hash recorded below (`cf164f68…`) is now superseded** — the
art commits changed bundled model/animation resources, so the runtime jar no longer hashes
to that value. A fresh `scripts/assert-release-jar.sh` hash must be captured at the next
package before it is cited as release evidence.

## Automated build and tests

The Maven reactor was run in `maven:3.9-eclipse-temurin-25`
(`mvn -B -f Phonemod/pom.xml test`, exit code 0) on 2026-08-18:

```text
Building Phonemod Public API 2.0.0-alpha.1
Compiling 138 source files with javac [debug target 25] to target/classes
Compiling 13 source files with javac [debug target 25] to target/test-classes
Tests run: 37, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The module split is 37 API source files (`phonemod-api`) and 138 runtime source files; the
API module emitted no `Compiling` line in this run because its outputs were already up to
date. The 37 tests are 13 JUnit classes of plain `@Test` methods (no parameterized or
repeated tests), all under the runtime module.

Covered suites include public API contracts, identity claims/transfers/shared identities,
idempotent component migration, UI payload validation, scalar BSON event decoding, rate
limiting, call-session transitions, configuration codec round-trips and runtime range
validation, call-audio override mapping, player-bound identity reuse, settings-view asset
separation, legacy player-setting defaults, player-setting round trips, and immutable
notification/routing preference snapshots. The remaining integration and multiplayer
cases stay open in the roadmap.

## Consumer compile matrix

Each known consumer built successfully with `phonemod-api:2.0.0-alpha.1` installed as its
compile-only API dependency:

| Consumer | Maven package |
|---|---:|
| AmazonMod | PASS |
| Banking | PASS |
| Boilerplate App | PASS |
| Computers | PASS |
| Pocket Tasks | PASS |
| Relay Ops | PASS |
| Signal Sweep | PASS |
| SocialCore | PASS |
| Trading App | PASS |
| Tuiter App | PASS |
| AuctionHouse Phone | PASS |
| Hello Device App (bundled example) | PASS |

Tuiter App (`tuiter-app`) and AuctionHouse Phone (`auctionhouse-phone`) were verified on
2026-08-18 with `mvn -B -f <consumer>/pom.xml package -DskipTests`; both returned exit
code 0 with `BUILD SUCCESS`. The bundled example
[`examples/hello-device-app`](../examples/hello-device-app) was verified the same way and
is the only consumer living inside this repository.

> Every one of these builds emits a non-fatal
> `'dependencies.dependency.systemPath' ... must specify an absolute path` model-validation
> warning, originating from the installed `phonemod-api-2.0.0-alpha.1.pom`, which carries a
> relative `systemPath` for the Hytale server jar. Maven prints it at `[ERROR]` level but
> still reports `BUILD SUCCESS` — judge these builds by exit code, not by grepping for
> `ERROR`.

This is compile evidence, not the runtime-combination or in-game smoke evidence required
by the RC gate.

## Production JAR

`scripts/assert-release-jar.sh` verified the runtime manifest identifier, public API
classes, both asset roots, phone item, phone UI, sounds, and language resources. Two
consecutive packages produced the same artifact hash:

```text
cf164f68b0d751aa2013bcec702ce09de9efbf0d2cc88ec286f8bb79d718c6ed
```

## Disposable-server boot

The production JAR was the only external mod in a fresh, isolated universe. The boot log
contained:

```text
[Phonemod] Setup complete!
Enabled plugin esq.phonemod:Phonemod
Universe ready!
Hytale Server Booted! [Multiplayer, Fresh Universe]
```

There was no plugin-specific exception. Hytale emitted its known native buffer
`SERR Reallocate` diagnostics while loading the base asset tree.

## Hytest deployment and integration boot

The candidate was deployed to the persistent pre-release hytest server on 2026-08-14.
The deployed dependency-complete subset used these production artifacts:

| Plugin | SHA-256 |
|---|---|
| Phonemod | `2e8e1713063503b42d90ec0e9bf8b4f2cba556905f6acd6ed2e17ce00c401fbc` |
| Banking | `ed5cab37a3427b3702f217de514ac0784c7ff30ce13ea2ae6a0f7fa935eb100a` |
| SocialCore | `126fe374699e480bf8aab86872545525e7a54351afdf26a0ddc4f2b786de77f1` |
| Character Framework | `a111020648e01587f18c59ecbe6ba1b20b0b6c13d0b3e254ca2b75ee8393a237` |
| AuctionHouse | `81468bce5f1c8120755e09ee26019a1170c42a832924de721af73178f027f36e` |
| Banking UI manifest | `a26919f5c2733cfd928c301bedac6dd39bcd2c558f1001cbdbd2858d4f9e2430` |

The first integration boot exposed two packaging facts and was not accepted: Hytale does
not include `2.0.0-alpha.1` in an old consumer's `>=1.0.0` range, and SocialCore's system-
scoped permission library was not included by Maven Shade. Migrated Banking/SocialCore
manifests were deployed, and SocialCore was corrected to shade its intended permission
library. The rebuilt production JAR contains nine `esq.permission` classes.

The corrected boot loaded the existing universe and reached:

```text
Enabled plugin esq.phonemod:Phonemod
Enabled plugin esq.banking:Banking
Enabled plugin socialcore:SocialCore
Enabled plugin esq:character-framework
Enabled plugin esq:AuctionHouse
Universe ready!
Hytale Server Booted! [Multiplayer]
```

Live console smoke checks reported:

```text
Phonemod API 2.0.0-alpha.1
Apps: 7 | Devices: 0
Pending saves: 0 | Migration conflicts: 0
Rejected: 0 | Rate limits: 0 | Save failures: 0
Device assets: 1 | Asset health: healthy
Dry run: 0 identities, 0 known conflicts; no data changed.
Schema: v2 | identities=0 conflicts=0 pendingSaves=0
```

The bundled `Phone` item was additionally given the vanilla `Tool` creative category and
type tag. Real hytest loaded `esq.phonemod:Phonemod` as an asset pack, and the live console
confirmed `You gave Esquirtle 1 of Phone`. The first phone interaction exposed that the
client legitimately emits numeric `State` values; the decoder now canonicalizes bounded
scalar BSON primitives while still rejecting arrays and documents. The player confirmed
the corrected phone works after redeployment.

Banking deliberately ships Java and assets separately. The required version-bumped
`banking:ui` directory pack was deployed with 35 files, including the phone app, ATM,
physical vault, currency assets, models, icons, interactions, and language file. A live
client joined after the pack loaded without a custom-UI parser error. Opening the ATM
physical vault with a metadata-bearing phone initially made the client reject
`#PlayerInventoryGrid.Slots`. Banking was corrected to strip server-only item metadata
from display copies while retaining the original stacks for deposits and withdrawals.
After the JDK 25 build passed all eight Banking tests and the replacement JAR reached
`ENABLED`, the player repeated the same ATM-vault action with the phone present and
confirmed the client error was gone.

Real hytest remained running behind the direct UDP 5521 endpoint. Rollback copies of the
replaced artifacts are outside the mod search path under
`/opt/hyserver/hytest/Server/mod-backups/phonemod-auction-20260814T232500Z/` and
`/opt/hyserver/hytest/Server/mod-backups/phonemod-ui-decoder-20260814T233000Z/`. The
pre-fix Banking JAR is retained under
`/opt/hyserver/hytest/Server/mod-backups/banking-itemgrid-20260814T234200Z/`.

## Framework configuration UI deployment

On 2026-08-15, Settings was split into a default Player tab and an admin-only Plugin tab,
then deployed to both the disposable server and persistent hytest. Plugin settings control
identity binding, call availability/privacy/reachability/timing/speaker radius, offline
messaging, and message length with codec-validated persisted values. Toggleable calls also
expose active-call speaker/private controls to their endpoints.

Player settings now drive theme, wallpaper, launcher layout, sound, message alerts,
message-preview privacy, incoming-call Do Not Disturb, and reset-to-default behavior. The
unimplemented Mono icon pack option was removed. Notification and call preferences are
published as immutable concurrent snapshots, avoiding target-player ECS access from a
caller or delivery thread.

Two consecutive JDK 25 packages produced the same updated JAR hash,
`cf164f68b0d751aa2013bcec702ce09de9efbf0d2cc88ec286f8bb79d718c6ed`; release
content assertions passed, and the JAR contained `DustSettings.ui`, `DustActiveCall.ui`,
the configuration enums, and the public speaker action constants. The disposable server
and dependency-complete hytest stack both reached `ENABLED`, `Universe ready!`, and
`Hytale Server Booted!`. A real admin client downloaded the expanded Player-tab pack and
joined the hytest world without a custom-UI parser error. The live player document then
persisted `AppLayout: List` together with the new notification, preview, and incoming-call
fields. Visual confirmation of each setting and two-client messaging/calling behavior
remain the current in-game acceptance step.

The prior hytest JAR can be restored from
`/opt/hyserver/hytest/Server/mod-backups/phonemod-config-ui-20260815T000803Z/Phonemod.jar`.
The immediately preceding split-tab build is retained at
`/opt/hyserver/hytest/Server/mod-backups/phonemod-player-settings-20260815T003330Z/Phonemod.jar`.

## Asset-validation gate

The packaged and extracted-asset validator attempts did not produce a global PASS:

1. An immutable `Assets.zip` run failed every vanilla instance with
   `Universe.makeWorld: Invalid path`, because the instance validator requires a writable
   RocksDB shadow for `Server/Instances`.
2. With the documented writable shadow in place, validation advanced through the base
   assets but deadlocked in the vanilla `Gyms/Gym_Movement` instance with
   `IllegalStateException: Thread 'WorldMap - instance-validate-Gyms-Gym_Movement' is
   already started`.

Phonemod's strict device-asset startup validation passed during the disposable boot and
the server reported one loaded `DeviceAsset` and three `DeviceThemeAsset` records. The
roadmap's global Hytale asset-validation checkbox remains open until the upstream
instance-validator failure is fixed or a known-good validation server build is supplied.

## External acceptance still required

The following gates cannot be honestly completed by an isolated build environment:

- copied-v1 migration, conflict resolution, and rollback rehearsals;
- dependency-complete runtime combinations for the remaining consumers;
- the multiplayer and audio-policy acceptance checklist; and
- reviewer sign-off on migration/rollback evidence.
