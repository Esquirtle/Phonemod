# Device Navigation

## DevicePageState

`esq.phonemod.device.core.DevicePageState`

Tracks the device's top-level navigation state. This is `DevicePage`-internal — it is not exposed to apps. App-internal navigation states (e.g. "which chat is open") live in `PhoneAppContext` per-player state via `ctx.getState()`.

| Value | When set | UI shown |
|-------|----------|----------|
| `HOME` | `loadHomeState()` / `home` action | App grid (built into `#APPHolder`) |
| `APP` | `openApp()` called | The active app's UI (loaded into `#AppContent`) |
| `CALLS` | `onCallEnded()` | Calls screen (dialer + history) |
| `INCOMING_CALL` | `onIncomingCall()` or call-state restore on open | Incoming-call overlay (`DustIncomingCall.ui`) |
| `ACTIVE_CALL` | `onCallAnswered()`; or call-state restore on open | Active-call overlay (`DustActiveCall.ui`) |

## DevicePage — routing hub

`DevicePage` is the single routing hub for the device UI. One instance is created per open device, via `DeviceService.createDevicePage(session)`.

### build()

Called once when the phone page is first opened. Logic:

1. Appends `Phone.ui` (the outer shell).
2. Wires the permanent `#HomeButton → home` binding.
3. Checks `CallRegistry` for call state:
   - Active call partner → set `ACTIVE_CALL`, render active call screen
   - Pending outgoing callee → set `ACTIVE_CALL`, render "Calling..." screen
   - Pending incoming caller → set `INCOMING_CALL`, render incoming call prompt
   - No call → call `loadHomeState()`

This ensures the player sees the correct UI when reopening the phone mid-call.

### handleDataEvent()

Called for every UI event. Phone-level actions are handled first:

| Action | Handling |
|--------|----------|
| `home` | Set state `HOME`; rebuild home screen |
| `open_app` | Call `openApp()` for the given app ID |
| `open_chat` | If not already in Whatgram: open Whatgram, then forward event to it |
| `answer_call` | Delegate to `CallRegistry.answerCall()` |
| `decline_call` / `hang_up` | Delegate to `CallRegistry.hangUp()` |

Any action not matched above is forwarded to `currentApp.handleEvent()`. If `currentApp` is null (no app open), the unhandled action is logged and a no-op update is sent.

### openApp()

Called when `open_app` is received:

1. Look up the app by ID in `PhoneService`.
2. If an app was open, call its `onClose()` with a context scoped to that closing
   app and post `DeviceAppCloseEvent`.
3. Update `currentApp`, `currentAppId`, and `currentState` (`APP`).
4. Create a `PhoneAppContext` scoped to the opened app.
5. Clear `#AppContent`.
6. Call `app.onOpen(ctx, cmd, evb)`.
7. Call `app.build(ctx, cmd, evb)`.

Navigating home or dismissing the device page also closes the active app and
posts `DeviceAppCloseEvent`.

## Live-push methods

These are called externally by `CallRegistry` or `PhoneRegistry` and push UI updates to the player without any player interaction.

### onIncomingCall(callerNumber, callerName)

Called when an incoming call arrives.

1. Play the ringtone sound.
2. If an app is active, call `currentApp.onIncomingCall(ctx, ...)` so it can
   update state or perform side effects.
3. Set state `INCOMING_CALL` and render the framework incoming-call overlay.
4. Push the update via `sendUpdate()`.

### onCallAnswered(partnerNumber, partnerName)

Called when the call connects.

1. Set state `ACTIVE_CALL`.
2. Render the active-call overlay.
3. Push update.

### onCallEnded()

Called when either party hangs up.

1. Render the Calls screen.
2. Set state `CALLS`.
3. Push update.

The session retains the current app while call overlays are shown. Navigating
home, opening another app, or dismissing the page performs app closure.

### onIncomingMessage(fromNumber)

Called when a text message arrives while an app is the visible top-level state.

1. If an app is visible and entity refs are cached:
   a. Call `currentApp.onIncomingMessage(ctx, fromNumber, cmd, evb)` (4-arg).
   b. If it returns `true` — send those builders directly. **No rebuild.** This is the dynamic-append fast path.
   c. If it returns `false` — call `currentApp.onIncomingMessage(ctx, fromNumber)` (2-arg), then call `build()` for a full rebuild.
2. Push the update.

If no app is visible the method does nothing. In particular, background message
updates do not replace call overlays or framework screens.

## Call state restoration sequence

```
Player opens device
  → DevicePage.build()
      ├─ CallRegistry.getActivePartner(phoneNumber) != null?
      │    └─ YES: render ACTIVE_CALL screen
      ├─ CallRegistry.getPendingCallee(phoneNumber) != null?
      │    └─ YES: render ACTIVE_CALL screen ("Calling...")
      ├─ CallRegistry.getPendingCaller(phoneNumber) != null?
      │    └─ YES: render INCOMING_CALL screen
      └─ ELSE: loadHomeState()
```

## Navigation flow summary

```
HOME
  └─[open_app]→ APP
       └─[home]→ HOME
       └─[during call]→ INCOMING_CALL or ACTIVE_CALL (live push)

INCOMING_CALL
  └─[answer_call]→ ACTIVE_CALL
  └─[decline_call]→ previous state or HOME

ACTIVE_CALL
  └─[hang_up]→ CALLS (if refs cached) or HOME
```
