# Architecture

## Class roles

| Class | Package | Role |
|-------|---------|------|
| `PhonePage` | `phone.ui` | Full-screen routing hub. Translates raw codec events into `PhoneEvent` and dispatches to the active app. |
| `PhoneService` | `phone.core` | Singleton. Manages registered apps and creates `PhonePage` instances. |
| `PhoneAppRegistry` | `phone.core` | Ordered catalog of registered apps. Throws on duplicate IDs. |
| `PhoneApp<S>` | `phone.api` | Interface every app must implement. |
| `StatefulPhoneApp<S>` | `phone.api` | Abstract base class that adds enum-backed per-player state helpers on top of `PhoneApp`. |
| `PhoneAppContext` | `phone.api` | Per-player session context passed to every app method. Provides ECS access, player identity, and scoped state storage. |
| `PhoneEvent` | `phone.api` | Normalized, immutable event payload created by `PhonePage` from the raw codec event. |
| `PhoneEventActions` | `phone.api` | String constants for every known action name. |
| `PhoneAssetPaths` | `phone.api` | String constants for every UI and icon asset path. |
| `AppMenu` | `phone.ui` | Static builder. Renders the home-screen app grid by iterating `PhoneService.get().getApps()`. |
| `PhoneStatesEnum` | `phone.ui` | Enum tracking the phone's top-level navigation state (`HOME`, `APP`, `CALLS`, `INCOMING_CALL`, `ACTIVE_CALL`). |
| `PhoneAppSessionState` | `phone.components` | ECS component on the player entity. Stores `Map<phoneNumber|appId, Map<key, value>>` so each physical phone's apps have an isolated key space. |

## Class relationship diagram

```
PhoneService (singleton)
  └─ PhoneAppRegistry
       └─ List<PhoneApp<?>>
            ├─ WhatgramApp  (StatefulPhoneApp<State>)
            ├─ ContactsApp  (StatefulPhoneApp<State>)
            ├─ CallsApp     (StatefulPhoneApp<State>)
            ├─ SettingsApp  (StatefulPhoneApp<State>)
            └─ [third-party apps...]

PhonePage  (one instance per open phone, created by PhoneService)
  ├─ currentApp: PhoneApp<?>
  ├─ currentState: PhoneStatesEnum
  ├─ phoneNumber: String
  └─ build() / handleDataEvent()
       └─ PhoneAppContext  (created on-demand per call)
            └─ PhoneAppSessionState (ECS component on player entity)
```

## Data flow — user button press

```
Player clicks UI button
  → CustomUIEvent fires (raw codec event)
  → PhonePage.handleDataEvent(ref, store, PhoneEventData)
      ├─ Phone-level action? (home / open_app / answer_call / ...)
      │    └─ Handled directly; sendUpdate() rebuilds UI
      └─ App-level action?
           ├─ PhoneAppContext created for currentApp
           ├─ PhoneEvent built from PhoneEventData
           ├─ currentApp.handleEvent(ctx, event, cmd, evb)
           └─ sendUpdate(cmd, evb, false) pushed to client
```

## Data flow — server-side push (incoming call / message)

```
CallRegistry / PhoneRegistry detects event
  → PhonePage.onIncomingCall() / onIncomingMessage() / ...
      ├─ Optionally delegates to currentApp hook
      └─ sendUpdate() pushed to client without any player interaction
```

## Design decisions

**App instances are singletons.** `PhoneService` holds exactly one instance of each registered app. This means all players share the same app object. Per-player data must never be stored as instance fields — use `PhoneAppContext` state instead.

**`PhonePage` owns phone-level routing.** Actions like `home`, `open_app`, `answer_call`, `decline_call`, and `hang_up` are intercepted and handled by `PhonePage.handleDataEvent()` before any app ever sees them. Apps only receive actions that fall through.

**`PhoneStatesEnum` is phone-level only.** It tracks whether the phone is showing the home screen, an app, a call screen, etc. App-internal navigation (e.g. "which chat is open") is not represented here — it lives in `ctx.getState()`.

**`open_chat` has special routing.** If `currentAppId` is not `"whatgram"`, `PhonePage` first opens the Whatgram app, then immediately forwards the `open_chat` event to it so the correct conversation is rendered in a single round-trip.

**Call state is restored on phone open.** `PhonePage.build()` checks `CallRegistry` for an active call, pending outgoing call, or pending incoming call before deciding whether to render the home screen. This ensures the correct UI is shown if the player reopens the phone mid-call.
