---
kind: plugin
id: Phonemod
title: "Phonemod — in-game phone/PC platform host"
owner: esq
group_name: "esq.phonemod:Phonemod"
manifest_path: plugins-srcs/Phonemod/src/main/resources/manifest.json
source_root: plugins-srcs/Phonemod
last_verified: 2026-08-11
applies_skills:
  - hytale-plugin-scaffold
  - hytale-assets
  - hytale-ecs
  - hytale-codec
  - hytale-commands
  - hytale-ui
  - hytale-ui-widgets
  - hytale-protocol
  - hytale-frameworks
  - hytale-verify
applies_topics:
  - app-platform-pattern
  - service-singleton-pattern
  - reactive-hud-lifecycle
asset_pack_id: Phonemod
depends_on: {}
status: active
tags: [host-plugin, app-platform, phone-os, app-registry, StatefulPhoneApp, asset-pack]
---

# Phonemod

In-game phone/PC platform: a host plugin that renders a phone shell with an app grid, and
exposes a registration SPI so other plugins ship apps as small classes + `.ui` documents.
Apps are registered at runtime via `PhoneService.get().registerApp(new XxxApp())`, live as
singletons, and keep per-player state in a `PhoneAppContext` the host passes in. This is
the canonical `app-platform-pattern` worked example in the corpus (8 plugins register into
it).

## Architecture

- **Entry point:** `esq.phonemod.PhoneMod extends JavaPlugin`. Singleton via `PhoneService`
  (separate static holder).
- **SPI:** `PhoneService` (`esq/phonemod/phone/core/PhoneService.java`):
  - `static initialize()` (`:~25`) — bootstraps `DeviceService` first, then `PhoneService`.
  - `@Nonnull static PhoneService get()` (`:~32`) — **init-guarded** (throws if not
    initialized). This is the separate-holder-with-`initialize()` shape the
    `hytale-frameworks` skill flags as a *user convention* (the official builtins use the
    plugin-class-as-singleton, non-throwing `get()`); kept deliberately here because host
    apps must bootstrap in a specific order (`DeviceService` first) before any register call.
  - `isInitialized()` (`:~40`) — soft probe host plugins use to tear down safely.
  - `registerApp(@Nonnull PhoneApp<?> app)` (`:44`) — the single SPI method every app
    plugin calls from its `setup()`/`start()`. Adds to `PhoneAppRegistry`.
  - `unregisterApp(@Nonnull String appId)` (`:49`) — mirrors register; host plugins call
    this on shutdown/reload to avoid stale apps (see `socialcore/.../OrgAppRegistry.java:61`).
  - `getApp(appId)` / `getApps()` (`:53,57`) — registry read for the menu screen.
- **App contract:** `PhoneApp<S extends Enum<S>>` (`esq/phonemod/phone/api/PhoneApp.java`)
  interface — every app plugin implements it:
  - `getId()` / `getDisplayName()` (identity + localized name).
  - `getAppButtonUI()` — returns the path of the `.ui` doc rendering the app's icon in the
    AppMenu grid. The contract (in the interface doc): root `#APPBUTTONENTRY` containing
    `TextButton #APPBUTTON` (icon hardcoded) + `Label #APPNAME`; AppMenu sets the label
    text dynamically after appending the file.
  - `getUIPath()` — the page doc opened when the player taps the button.
  - State enum `S` typesafe the active tab/view; per-player state lives in the
    `PhoneAppContext` the host passes — **never on the app instance fields** (singleton!).
- **Convenience base:** `StatefulPhoneApp<S>` — apps extend this (banking does:
  `banking/.../BankingApp.java:38`). The base class wires the state-enum + context plumbing
  so an app author only overrides `build`/`handleEvent` + declares the `Tab` enum.
- **Device subsystem:** `esq.phonemod.device` package — `DeviceService.initialize()` runs
  first; the device framework handles per-device state (a "phone/PC" is an item + UI + a
  device-component on the player). Apps call device APIs to read the active owner.
- **Asset pack:** `IncludesAssetPack: true`. `Phonemod-Assets/` is a separate, Main-less
  DIRECTORY pack (`mods/phonemod.ui/`) — see `hytale-assets` skill "Two plugin flavors."
  Loads as a directory (jar would NPE the boot per `PluginManifest.java:499`). Apps' `.ui`
  documents live there (shared ابتداaccess from any consumer); the plugin jar itself also
  carries `Server/`+`Common/` per its manifest.
- **Built-in apps:** `SetupManager.java:94-99` registers 7 first-party apps
  (`WhatgramApp`, `ContactsApp`, `CallsApp`, `SettingsApp`, `AppStoreApp`, `GroupChatApp`).
  These both demonstrate the SPI and seed the phone with content before any third-party
  app loads.

## Patterns & anti-patterns

- **App-platform pattern (canonical worked example).** Host plugin owns a shell + a
  registry SPI; third-party plugins register feature apps via `registerApp`. Per-entity
  state lives on the passed-in `PhoneAppContext`; the app class is a shared singleton.
  Matches the `hytale-frameworks` skill's "app platform pattern" verbatim.
- **Init-guarded `get()` (intentional divergence).** `PhoneService.get()` throws if
  `initialize()` hasn't run, vs the official non-throwing pattern. Justified because
  device initialization is order-dependent; documented as a user convention in
  `hytale-frameworks`.
- **Per-player state via context, not fields.** The contract enforced through the
  `StatefulPhoneApp<S>` base class — `BankingApp.Tab` is the state enum, but the actual
  per-player balances come from a `BankAccount` resolved via `PhoneAppContext` (see
  `banking/.../BankingApp.java:260 account(ctx)`).
- **Separate asset-pack directory for dev iteration.** `Phonemod-Assets/` is Main-less and
  deployed as a directory (see skill); lets artists edit `.ui` files and reload without
  rebuilding the code jar.
- **Anti-pattern / TODO:** The init-guarded `get()` means a third-party app's `setup()`
  that runs before `Phonemod`'s `start()` (despite the manifest dep) would NPE. The dep
  guarantees Phonemod's `setup()` ran, but apps should register from `start()` post-init,
  not `setup()` — verify in your app's wiring (most do).

## Skills applied (curated)

- `hytale-plugin-scaffold` — standard `JavaPlugin` entry + manifest + Maven build.
- `hytale-assets` — `IncludesAssetPack: true` + a separate `Phonemod-Assets/` directory pack.
- `hytale-ecs` — per-player device component on the player entity (minor; mostly UI + services).
- `hytale-codec` — `PhoneApp` settings + per-app persistence via `BsonUtil`-style files.
- `hytale-commands` — phone/PC summon + admin commands (verify path).
- `hytale-ui` — the entire phone shell is `InteractiveCustomUIPage` + the AppMenu grid; the
  canonical worked example of custom UI + free-form client↔server data channel.
- `hytale-ui-widgets` — `.ui` documents for every screen (both in plugin jar + `Phonemod-Assets/`).
- `hytale-protocol` — `CustomPage` packet [218] carries all UI; `CustomPageEvent` for
  per-app input. No custom packets — the UI channel is the only data path.
- `hytale-frameworks` — the canonical `app-platform-pattern` + `service-singleton-pattern`
  worked example cited by the skill.
- `hytale-verify` — every change ends with a Docker build + boot to ENABLED.

## Topics this plugin exemplifies

- `app-platform-pattern` — canonical (host + registry + `registerApp` SPI).
- `service-singleton-pattern` — `PhoneService` (init-guarded variant).
- `reactive-hud-lifecycle` — phone HUD overlays (verify path).

## Consumers / dependencies

- **Deps:** none (`depends_on: {}`).
- **Consumed by** (`PhoneService.get().registerApp(...)` invocations across plugins-srcs):
  - `banking` (`banking/.../BankingApp.java`, registered at `setup/SetupManager.java:51`)
  - `amazonmod` (`setup/SetupManager.java:17-18`, two apps)
  - `boilerplate-app` (`MyAppPlugin.java:42`)
  - `computers` (verify path)
  - `pocket-tasks` (`setup/PocketTasksSetup.java:36`)
  - `relay-ops` (`setup/RelayOpsSetup.java:41`)
  - `signal-sweep` (`setup/SignalSweepSetup.java:38`)
  - `socialcore` (per-org apps at `organization/phone/OrgAppRegistry.java:30`)
  - `trading-app` (`setup/SetupManager.java:48`)
- **Asset-pack consumers:** plugins referencing `Common/UI/Custom/Pages/Phone/...` ship
  `.ui` docs inside their own jar OR ride `Phonemod-Assets/`; both resolve by the same path.

## Open gaps

- `computers` registers a device (block) rather than just an app — verify whether it also
  calls `registerApp` or interacts with `DeviceService` directly.
- The "reactive-hud-lifecycle" topic is speculative — verify there's a `CustomUIHud` in
  Phonemod before keeping it in `applies_topics`.