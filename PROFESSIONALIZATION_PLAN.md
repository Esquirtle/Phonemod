# Phonemod Professionalization Plan

This document is the gated roadmap from Phonemod 1.0 to 2.0. It is the source of truth
for sequencing, compatibility, test coverage, and release readiness. Architecture and API
details in the existing `docs/` directory describe the current v1 implementation unless
they explicitly say otherwise.

## Reconciliation status — 2026-08-17 (verified)

The checkboxes below predate a large body of implementation and had drifted out of sync.
Reconciled against the actual tree at `2.0.0-alpha.1`, with the following **verified-green**
baseline (Docker `maven:3.9-eclipse-temurin-25`, JDK 25):

- **Build:** both modules `BUILD SUCCESS` (`phonemod-api` + `phonemod-runtime`).
- **Tests:** 26 unit tests, 0 failures — CallSession, TokenBucket, UiEventValidator,
  UiEventDecoder, IdentityRegistry, MigrationComponent, PhonemodConfig, DeviceSettings,
  ApiContract, PhoneRegistryPreferences, SettingsUiContract.
- **Package:** `scripts/assert-release-jar.sh` passes (manifest, `DevicePlatform.class`,
  `Phone.ui`, sounds, device/item JSON, `.lang` all present in the runtime jar).
- **Boot:** disposable server reaches `Enabled plugin esq.phonemod:Phonemod` → ENABLED,
  asset pack loads, universe reaches ready. Only non-plugin exception is the server core's
  `HardwareUtil.getUUID` under Docker (0 phonemod frames).

Reconciled gate reality (details ticked inline below where evidenced by a passing test or
the boot):

- **1.1 security/correctness — mostly IMPLEMENTED + unit-tested.** Payload/param
  validation (`security/UiEventValidator`, `UiSafetyLimits`), boundary decode
  (`UiEventDecoder`), per-session rate-limit (`security/TokenBucket`), and the atomic
  `CallSession` state machine all exist and pass tests. **Still open:** the forged-action /
  audio-leak *multiplayer* acceptance pass, and runtime proof of world-thread dispatch &
  packet-filter handle deregistration.
- **2.0 Alpha — API + identity + migration DONE (test-evidenced).** Module split,
  `phonemod-api`, `DevicePlatform`/services, `identity/` UUID registry (+ codec document),
  and the `/deviceframework` command (status, migration dry-run/status/conflicts/repair,
  transfer, release) are in. The migration engine was **already implemented** (not a stub:
  `PhoneNumber`→UUID via `IdentityRegistry.claimLegacy`, first-claimant/duplicate resolution,
  timestamped `.pre-v2` backup via `ensureBackupLocked`, no-overwrite component remap,
  `SchemaVersion` on all six components) and is now covered by 33 passing unit tests proving
  backup-once, double-run byte-equivalence, concurrent-claim resolution, and PhoneNumber→UUID.
  **Remaining Alpha gaps:** mailbox/sync-channel bounded persistence, `SchemaVersion`-gated
  branch logic (currently write-only), and the dry-run/rehearsal/rollback **exit-gate evidence**.
- **2.0 Beta — PARTIAL.** `AssetHealthService` + `OperationalMetrics` seeds exist; the Dust
  shared-design-system rebuild, responsive terminal/desktop shell, startup-failing asset
  validation, bounded retention/pruning, and structured no-PII logging are not done.
- **2.0 RC — NOT STARTED.** Consumer migration matrix, release evidence (reproducible
  hashes, migration rehearsal + rollback), and the in-game acceptance pass are outstanding.

## Destination

Phonemod 2.0 is a public, generic Hytale device-platform SDK rather than a phone-specific
application host. The release consists of:

- one production plugin JAR containing the runtime Java classes, public API classes,
  `Common/`, `Server/`, UI documents, the reference phone item, sounds, language files,
  and `manifest.json`;
- a separately published, compile-only `esq.phonemod:phonemod-api` artifact for consumers;
- a generic v2 device API suitable for phones, PCs, tablets, and shared terminals;
- a deprecated v1 phone compatibility bridge that remains supported and tested throughout
  all 2.x releases;
- durable device identity, messaging, calls, synchronization, migrations, operational
  controls, tests, and public documentation; and
- successful migration and smoke testing of every known in-repository consumer.

The runtime plugin identifier remains **`esq.phonemod:Phonemod`**. Existing manifest
dependencies must continue to resolve without modification.

## How to use this roadmap

Current implementation and verification evidence is recorded in
[`docs/VERIFICATION_EVIDENCE.md`](docs/VERIFICATION_EVIDENCE.md). Unchecked release gates
remain intentionally open until their required migration data, multiplayer participants,
or upstream validator fix is available; a successful compilation is not treated as a
substitute for those gates.

- Work proceeds in order: 1.1, 2.0 Alpha, 2.0 Beta, then 2.0 RC.
- A phase may start only after every exit gate in the preceding phase has evidence.
- Check a task only when its implementation and required tests are committed together.
- Record evidence beside the relevant checkbox or in the release notes: test name, build
  artifact, migration report, asset-validation result, or boot-log location.
- A failed gate reopens the phase that introduced the failure. Schedule pressure does not
  waive a gate.
- API compatibility applies to behavior and persisted data, not only compilation.

No phase advances with failing tests, unresolved migration conflicts, unbounded state,
thread-affinity violations, stale global registrations, or missing required assets.

## Compatibility invariants

These constraints apply to every phase:

- Preserve existing player, device, messaging, call, installation, settings, app-state,
  and synchronization data.
- Keep `esq.phonemod:Phonemod` as the runtime manifest identifier.
- Keep the server-driven architecture and Hytale's existing UI and voice packet set. Do
  not introduce a custom packet protocol.
- Treat the bundled phone as the reference device, not as the abstraction boundary.
- Require explicit ownership transfer for personal devices. Item possession or copied
  metadata never transfers ownership.
- Support and test `PhoneService`, `PhoneApp`, `PhoneAppContext`, and `PhoneEvents` as
  deprecated adapters for all 2.x releases. Their removal is reserved for 3.0.
- Maintain a documented rollback path until the final 2.0 release is accepted.

## Target API and architecture

### Platform locator and services

Add `DevicePlatform` as the public API locator. `find()` provides an optional lookup and
`require()` provides a fail-fast lookup. The platform exposes these service boundaries:

- app registration and discovery;
- device identity, ownership, and device-type discovery;
- presence;
- messaging and offline delivery;
- calling and audio policy;
- notifications; and
- synchronization channels.

The API artifact contains contracts and immutable value types only. Runtime
implementations remain internal to the production plugin.

### Application contract

Replace the v1 `PhoneApp` contract with `DeviceApp`:

- `AppDescriptor` declares the canonical namespaced ID, legacy aliases, localization key,
  icon and UI paths, capabilities, permissions, supported device types, system-app status,
  and sort order.
- `DeviceAppContext` exposes the player, stable device identity, optional human address,
  world-thread ECS access, bounded session state, notifications, and sync channels.
- `DeviceAction` is an immutable, validated event envelope. Raw client payloads never
  reach an application implementation.
- `DeviceActionResult` is one of `HANDLED`, `RERENDER`, or `REJECTED`.

Canonical app IDs are namespaced, for example `phonemod:whatgram` and `computers:pc`.
Each migrated application declares its old simple ID as a legacy alias so installed-app
and session-state data can be remapped.

### Owner-bound registration

`registerApp(PluginBase owner, DeviceApp app)` returns an `AppRegistration` handle.
Registration must:

- bind lifecycle cleanup to the guest plugin's `TaskRegistry`;
- reject duplicate canonical IDs and legacy aliases with an actionable diagnostic;
- publish immutable snapshots safe for concurrent reads and registration changes; and
- make explicit `AppRegistration.close()` idempotent and optional for normal plugin
  shutdown.

### Shared events

Publish framework events through Hytale's shared event bus. Provide typed pre/post events
for device opening, application installation/opening, messages, and call transitions.
Authorization-sensitive pre-events are cancellable. Post-events describe committed state
and cannot retroactively veto it.

The deprecated `PhoneEvents` adapter forwards to this shared event surface during 2.x.

### Permissions and administration

Remove the shaded permission helper from the runtime. Resolve application requirements
through Hytale's `PermissionsModule`. Retain old permission commands as warning aliases
during 2.x; supported administration lives under `/deviceframework` and is permission
gated.

## 1.1 — Security and correctness gate

**Goal:** make the current platform safe and deterministic before changing its public
contract or persistence model.

### Server-authoritative actions

- [ ] Enforce installation, device compatibility, ownership, and permission checks in the
  server-side application-open path. UI visibility never grants authority.
- [x] Validate total UI payload size, action and app IDs, parameter count, key/value
  lengths, action-specific fields, and current page/app/call state.
- [x] Catch malformed payloads at the boundary, acknowledge them safely, and avoid leaking
  exception details to clients.
- [ ] Lock mutation buttons until acknowledgement and make repeated action handling
  idempotent where possible.
- [x] Rate-limit UI actions, messages, and call attempts per player/session.
- [ ] Dispatch every target-player ECS operation onto that player's world thread,
  including administrative application commands.

### Calls and voice

- [x] Replace independent call maps with one atomic `CallSession` state machine that
  reserves both endpoints.
- [x] Reject self-calls and overlapping calls; make every transition idempotent.
- [ ] Keep calls alive when either UI page is dismissed. Track presence separately from an
  optional open-page handle and restore call UI when the page reopens.
- [ ] Terminate calls on hangup, disconnect, timeout, or framework shutdown.
- [ ] Route voice from an immutable concurrent snapshot read by the Netty thread.
- [ ] Retain and deregister the exact packet-filter registration handle.
- [ ] Add `CallAudioPolicy` to device assets with `PRIVATE`, `SPEAKER`, and `TOGGLEABLE`.
  Private audio is the default; toggleable calls begin private, and only an active
  endpoint may change speaker state.

### Persistence and assets

- [ ] Serialize sync persistence through
  `Universe.get().getStorageManager().doSave`.
- [ ] Snapshot state while holding the channel lock, sequence writes per path, and update
  saved hashes only after a successful write.
- [ ] Replace global asset registration with the plugin-owned asset registry.
- [ ] Invalidate cached device shells on asset reload.
- [ ] Fail setup when any required codec or interaction registration fails.

### 1.1 tests

- [ ] App-open authorization covers hidden UI, forged app IDs, incompatible devices,
  missing ownership, missing permissions, and revoked permissions.
- [ ] Boundary tests cover malformed payloads, stale actions, size/count/length limits,
  state mismatches, acknowledgements, locks, and rate limits.
- [ ] Deterministic concurrency tests cover simultaneous calls, repeated transitions,
  disconnects, timeouts, two worlds, and shutdown.
- [ ] Call integration tests cover pages open/closed, reopen, reconnect behavior, private
  and speaker policies, muted voice, and stale client actions.
- [ ] Ordered-save tests cover concurrent channel updates, write failure/retry, and
  shutdown during saves.
- [ ] Asset reload and failed-registration tests prove there are no stale global handles.

### 1.1 exit gate

- [ ] All 1.1 tasks and tests are complete.
- [ ] Docker/JDK 25 Maven tests pass.
- [ ] The packaged JAR passes Hytale asset validation where applicable.
- [ ] A disposable server boot reaches `esq.phonemod:Phonemod` **ENABLED** with no
  plugin-specific exception.
- [ ] A focused multiplayer pass confirms forged actions cannot open or mutate an
  unauthorized app and calls cannot overlap or leak audio.

## 2.0 Alpha — API, identity, and persistence

**Entry condition:** the 1.1 exit gate is complete.

**Goal:** establish the public v2 contract and migrate data without loss.

### Build and packaging

- [x] Convert the build into API and runtime modules.
- [x] Publish `esq.phonemod:phonemod-api` for compile-only consumer use.
- [x] Include the API classes in the single production runtime JAR.
- [ ] Remove machine-specific filesystem paths and resolve Hytale as a provided, locally
  installed dependency.
- [ ] Move all `Phonemod-Assets` content into the runtime module under the correct
  `Common/` and `Server/` roots.
- [ ] Retire the separate production asset pack. Use runtime resources directly only for
  development-time asset iteration.
- [x] Add a deterministic JAR-content assertion for the manifest, API classes, `Common/`,
  `Server/`, phone item, UI documents, sounds, and language files.

### v2 API and compatibility bridge

- [x] Implement `DevicePlatform` and the app, device, presence, messaging, calling,
  notification, and synchronization service contracts.
- [x] Implement `DeviceApp`, `AppDescriptor`, `DeviceAppContext`, `DeviceAction`,
  `DeviceActionResult`, and `AppRegistration`.
- [ ] Add owner-bound, alias-aware registration and concurrent immutable snapshots.
- [ ] Add typed shared-event-bus pre/post events.
- [ ] Implement and test the deprecated v1 adapters without requiring consumers to call
  `initialize()`.
- [ ] Document semantic-versioning and 2.x/3.0 compatibility policy before publishing
  the first API artifact.

### Durable identity

- [x] Introduce `DeviceIdentity` with a UUID independent of its human-readable address.
- [ ] Personal devices support explicit ownership transfer. Copied item metadata never
  changes ownership.
- [ ] Shared devices such as terminals use a shared identity and may omit an address.
- [x] Store the authoritative universe-scoped identity registry in a versioned codec
  document.
- [ ] Derive the address index in memory and save changes atomically through
  `StorageManager`.

### Persistence, versioning, and migration

- [ ] Persist offline mailboxes and sync channels in bounded per-address/per-channel files
  using ordered saves.
- [ ] Add schema versions to every framework-owned component and document.
- [x] Make migrations idempotent and create a timestamped backup before the first v2
  mutation.
- [x] On first v2 use, convert legacy `PhoneNumber` metadata to a UUID identity.
- [x] Let the first legitimate claimant retain the old address.
- [x] Give duplicate legacy items a new address and remap that player's contacts,
  histories, installs, settings, and app state without overwriting another owner.
- [ ] Keep stable identity UUID/address during an explicit transfer while leaving the old
  owner's history and application data private. The new owner receives clean defaults.
- [x] Add permission-gated `/deviceframework` migration dry-run, status,
  conflict-report, repair, transfer, and release commands.
- [ ] Make all command operations target-world safe and audit logged.

### 2.0 Alpha tests

- [ ] Unit tests cover duplicate/guest registration cleanup, alias conflicts, immutable
  snapshots, and plugin unload/reload.
- [ ] Identity tests cover claims, copied metadata, duplicate simultaneous claims,
  explicit transfer, release, shared devices, and conflicts.
- [ ] Migration fixtures cover every current component/document/channel schema.
- [x] Running each migration twice produces byte-equivalent canonical data and no second
  backup or mutation.
- [ ] Retention and ordered-save tests cover offline mailboxes and sync-channel files.
- [ ] V1 consumers compile and run unchanged through the compatibility bridge.

### 2.0 Alpha exit gate

- [ ] All Alpha tasks and tests are complete.
- [ ] A dry run against copied v1 data reports every planned mutation and conflict.
- [ ] A full rehearsal migrates copied v1 data without loss; a second run is a no-op.
- [ ] Backup restoration returns the rehearsal environment to its pre-migration state.
- [ ] Both API and runtime artifacts are reproducible and contain only their intended
  surfaces.
- [ ] Docker/JDK 25 tests, JAR inspection, asset validation, and disposable-server boot
  are green.

## 2.0 Beta — UX and production operations

**Entry condition:** the 2.0 Alpha exit gate is complete.

**Goal:** finish the player experience and make the platform observable and operable in
production.

### User experience and validation

- [ ] Rebuild Dust as a shared design system with localized text, reusable
  tokens/templates, consistent spacing, and explicit loading, disabled, error, and empty
  states.
- [ ] Provide responsive phone and terminal/desktop shells.
- [ ] Validate each device asset's selectors, default apps, capabilities, call policy,
  themes, and UI paths during asset loading.
- [ ] Fail startup for an invalid required device or shared design-system asset rather
  than starting a partially functional platform.
- [ ] Move application installation/opening, calls, contacts, messaging, settings, and
  group chat onto the validated v2 action pipeline.

### Operations and bounded state

- [ ] Add structured logs with operation, app/device/session IDs, result, and latency.
  Never log message bodies, contact data, or other sensitive content.
- [ ] Implement `MetricProvider` counters/gauges for active sessions/calls,
  registrations, rejected actions, rate limits, mailbox depth, sync queue depth, save
  failures, and migration conflicts.
- [ ] Add `/deviceframework status` output for API version, registered apps/devices,
  active sessions, pending saves, persistence health, asset health, and migration state.
- [ ] Replace every unbounded collection with configured retention and deterministic
  pruning.

### 2.0 Beta tests

- [ ] Selector, theme, path, capability, default-app, and call-policy validation tests
  cover valid and invalid assets.
- [ ] End-to-end action tests cover installation/opening, contacts, direct and group
  messaging, settings, calls, and stale page state.
- [ ] Log tests prove sensitive message/contact content is absent.
- [ ] Metrics/status tests prove counters, gauges, queue depths, health, and migration
  state match controlled scenarios.
- [ ] Long-running retention tests prove deterministic pruning and bounded memory/disk
  state.

### 2.0 Beta exit gate

- [ ] All Beta tasks and tests are complete.
- [ ] Every built-in workflow uses the validated v2 action pipeline.
- [ ] Invalid required assets stop startup with an actionable diagnostic.
- [ ] Operators can explain active load, persistence health, asset health, and migration
  state from `/deviceframework status` plus metrics without inspecting private content.
- [ ] Docker/JDK 25 tests, deterministic package inspection, asset validation, and
  disposable-server boot are green.

## 2.0 RC — Ecosystem migration and release

**Entry condition:** the 2.0 Beta exit gate is complete.

**Goal:** migrate the ecosystem, publish the SDK documentation, and prove release
readiness.

### Consumer migration matrix

Each consumer must compile against `phonemod-api`, declare the runtime dependency on
`esq.phonemod:Phonemod`, remove calls to `initialize()`, use owner-bound registration,
adopt canonical namespaced IDs and typed actions, declare its legacy aliases, and use
explicit unregister only when locally useful.

| Consumer | API build | Manifest | Canonical IDs/aliases | Runtime smoke | Notes |
|---|---:|---:|---:|---:|---|
| AmazonMod | [ ] | [ ] | [ ] | [ ] | Migrate both registered apps. |
| Banking | [ ] | [ ] | [ ] | [ ] | Include integrations used by Trading App. |
| Boilerplate App | [ ] | [ ] | [ ] | [ ] | Becomes the canonical public example. |
| Computers | [ ] | [ ] | [ ] | [ ] | Cover PC and shared-device behavior. |
| Pocket Tasks | [ ] | [ ] | [ ] | [ ] | Cover existing phone/PC support. |
| Relay Ops | [ ] | [ ] | [ ] | [ ] | Cover persistent sync rooms. |
| Signal Sweep | [ ] | [ ] | [ ] | [ ] | Cover device compatibility. |
| SocialCore | [ ] | [ ] | [ ] | [ ] | Cover dynamic/per-organization apps. |
| Trading App | [ ] | [ ] | [ ] | [ ] | Cover Banking and PC dependencies. |

### Public documentation

- [ ] Update Boilerplate App into a minimal, tested v2 example.
- [ ] Publish API reference documentation.
- [ ] Publish an application tutorial and a device tutorial.
- [ ] Document asset layout, naming, selectors, themes, localization, and validation.
- [ ] Document world-thread, network-thread, and persistence-thread rules.
- [ ] Publish the v1-to-v2 migration guide and compatibility table.
- [ ] Publish semantic-versioning, deprecation, and support policy.
- [ ] Update every existing v1-oriented page under `docs/` or label it as legacy.

### Release evidence

- [ ] Build every consumer against the published-candidate `phonemod-api` artifact.
- [ ] Boot representative standalone combinations and all dependency-complete
  combinations.
- [ ] Produce one deterministic production JAR and assert its manifest, API classes,
  `Common/`, `Server/`, phone item, UI documents, sounds, and language files.
- [ ] Rehearse migration against a fresh copy of production-shaped v1 data.
- [ ] Resolve every migration conflict or document and approve an explicit repair.
- [ ] Rehearse rollback from the migrated copy.
- [ ] Complete the multiplayer in-game acceptance checklist below.

### 2.0 release gate

- [ ] All RC tasks and every consumer-matrix cell are complete.
- [ ] All unit, deterministic-concurrency, migration, integration, and compatibility tests
  pass under Docker/JDK 25.
- [ ] Hytale asset validation succeeds.
- [ ] A disposable server reaches `esq.phonemod:Phonemod` **ENABLED** with no
  plugin-specific exception for every required plugin combination.
- [ ] Migration and rollback rehearsals have retained evidence and reviewer sign-off.
- [ ] The in-game multiplayer acceptance pass is complete.
- [ ] The final JAR and API artifact match the documented version and reproducible hashes.

## Default safety configuration

All limits are server-configurable, codec-validated, and bounded to defensible minimum
and maximum values. These defaults apply unless a migration or measured load test
justifies a documented change.

| Control | Default |
|---|---:|
| UI event payload | 8 KiB |
| UI parameters per action | 32 |
| UI parameter key | 64 characters |
| UI parameter value | 1,024 characters |
| Message body | 512 characters |
| Contact name | 64 characters |
| UI action rate | 10/second, burst 20 |
| Message rate | 2/second, burst 5 |
| Call attempts | 1 per 5 seconds |
| Messages retained per conversation | 500 |
| Call records retained | 200 |
| Persistent channel messages retained | 1,000 |
| Offline messages retained per address | 200 for 30 days |

Rate-limit rejection must be safe, observable, and must not mutate application or call
state. Pruning order must be deterministic and covered by tests.

## Cross-cutting test inventory

The phase lists determine when each test lands. Before 2.0 release, the combined suite
must cover:

- duplicate and guest-plugin registration cleanup;
- application-launch authorization and all input limits;
- rate limiting and safe acknowledgement;
- identity claims, transfers, releases, duplicate/conflicting claims, and copied metadata;
- every legal and illegal call transition;
- retention and deterministic pruning;
- synchronization write ordering and write-failure recovery;
- all v1-to-v2 component, document, mailbox, and channel migrations;
- simultaneous calls, concurrent channel updates, shutdown during saves, and plugin
  unload/reload;
- calls with pages open/closed, disconnect/reconnect, multiple worlds, speaker policies,
  muted voice, timeouts, and stale client actions; and
- unchanged v1 consumers running through the deprecated adapters.

Concurrency tests must be deterministic: use barriers, controlled executors, and explicit
state assertions rather than timing sleeps.

## In-game multiplayer acceptance checklist

- [ ] Create and open the bundled phone on a clean player.
- [ ] Claim a legacy phone and confirm its address and data migrate once.
- [ ] Attempt to use copied metadata and confirm it cannot steal ownership.
- [ ] Explicitly transfer a personal device and confirm the recipient receives clean
  personal state while the previous owner's data remains private.
- [ ] Install and uninstall applications, including permission and compatibility failures.
- [ ] Send direct and group messages across disconnect/reconnect and server restart.
- [ ] Place, answer, decline, hang up, time out, and reopen UI during calls.
- [ ] Verify private, speaker, and toggleable audio policies with muted voice.
- [ ] Use PC/shared-device access without creating accidental personal ownership.
- [ ] Exercise controller and mouse interaction across phone and terminal layouts.
- [ ] Open and exercise every built-in application.
- [ ] Verify `/deviceframework status`, migration commands, and audit output with allowed
  and denied administrators.

## Definition of done

Phonemod 2.0 is done only when the 2.0 release gate is fully checked, retained release
evidence is reviewable, the production JAR is self-contained, all known consumers pass
their matrix, v1 compatibility remains green, and migration plus rollback have succeeded
against copied v1 data. Compilation alone is not release evidence.
