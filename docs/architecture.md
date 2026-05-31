# Architecture

Phonemod is a generic, server-side **device framework**. The phone is the
reference device; tablets/terminals are just different device assets. Runtime
shell + routing live in the `device` packages; the app contract lives in
`phone.api` (kept stable for app authors).

## Class roles

| Class | Package | Role |
|---|---|---|
| `DeviceAsset` | `device.assets` | Immutable JSON device blueprint (`Server/Phonemod/Devices/*.json`): shell UI path, selectors, capabilities, default apps, theme. |
| `DeviceService` | `device.core` | Singleton. Resolves device assets, builds `DeviceShell`/`DeviceSession`, creates `DevicePage`. |
| `DeviceShell` | `device.core` | Immutable runtime descriptor from a `DeviceAsset`: content/home/home-button selectors, capability gates, sound profile, themeable selectors. |
| `DeviceSession` | `device.core` | Runtime-only per-open-device state: current app, `DevicePageState`, page handle, refs. Never serialized. |
| `DevicePage` | `device.ui` | The custom UI page. Translates raw codec events into `PhoneEvent`, handles shell-level actions, dispatches the rest to the active app, and drives theme + call overlays. |
| `DevicePageState` | `device.core` | Enum: `HOME`, `APP`, `CALLS`, `INCOMING_CALL`, `ACTIVE_CALL`. |
| `PhoneService` | `phone.core` | Singleton. Manages registered apps (`registerApp` / `getApp` / `getApps`). |
| `PhoneApp<S>` / `StatefulPhoneApp<S>` | `phone.api` | The app contract; `StatefulPhoneApp` adds enum-backed per-player state. |
| `PhoneAppContext` | `phone.api` | Per-player session context passed to every app method: ECS access, identity, scoped state, and the shell content selector. |
| `PhoneEvent` / `PhoneEventActions` | `phone.api` | Normalized event payload + action-name constants. |
| `PhoneUi` / `PhoneAssetPaths` | `phone.api` | UI-builder helpers + asset-path constants. |
| `PhoneAppSessionState` | `phone.components` | ECS component on the player: `Map<deviceId\|appId, Map<key,value>>` — isolated per device + app. |

## Relationship diagram

```
DeviceService (singleton)              PhoneService (singleton)
  ├─ DeviceRegistry (shell cache)        └─ PhoneAppRegistry → List<PhoneApp<?>>
  └─ creates ↓                                WhatgramApp / ContactsApp /
DeviceSession (one per open device)           CallsApp / SettingsApp / [3rd-party]
  ├─ shell: DeviceShell
  ├─ currentApp / currentState: DevicePageState
  └─ page: DevicePage
        └─ build() / handleDataEvent()
             └─ PhoneAppContext (per call) → PhoneAppSessionState (ECS)
```

## Data flow — user button press

```
Player clicks UI button
  → CustomUIEvent (raw codec) → DevicePage.handleDataEvent()
      ├─ Shell action (home / open_app / answer_call / hang_up / ...)?
      │    └─ handled directly; sendUpdate() refreshes UI
      └─ App action?
           ├─ PhoneAppContext created for the current app
           ├─ PhoneEvent built (unknown keys preserved in getParams())
           ├─ currentApp.handleEvent(ctx, event, cmd, evb)
           └─ sendUpdate(cmd, evb) pushed to client
```

## Data flow — server-side push (incoming call / message)

```
CallRegistry / messaging detects event
  → the live DevicePage (via DevicePageHandle) onIncomingCall() / onIncomingMessage()
      ├─ optionally delegates to the current app's hook
      └─ sendUpdate() pushed to client (no player interaction)
```

Registries store the `DevicePageHandle` interface, not a concrete page type.

## Design decisions

**App instances are singletons.** `PhoneService` holds one instance of each app, shared by all players. Per-player data must live in `PhoneAppContext` state, never instance fields.

**`DevicePage` owns shell-level routing.** `home`, `open_app`, `answer_call`, `decline_call`, `hang_up` are intercepted in `DevicePage.handleDataEvent()` before any app sees them. Apps only receive actions that fall through.

**Shell selectors come from the device asset.** Content / home / home-button / topbar / bottombar selectors are declared in `Phone.json` and read via `DeviceShell` — never hardcoded. The matching element IDs are provided by DustLib shell components (`@DustStatusBar`, `@DustHomeBar`, …).

**`DevicePageState` is shell-level only.** Home vs app vs call screen. App-internal navigation (e.g. which chat is open) lives in `ctx.getState()`.

**`open_chat` has special routing.** If the current app is not `whatgram`, `DevicePage` opens Whatgram first, then forwards `open_chat` so the conversation renders in one round-trip.

**Call state is restored on open.** `DevicePage.build()` checks `CallRegistry` for an active/pending call before rendering home, so reopening mid-call shows the right screen. Call overlays are gated by the device's `calls` capability.

**The status bar is persistent.** `#TopBar` sits above `#AppContent`, so it survives apps and Home navigation clearing the content area.
