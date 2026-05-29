# PhoneMod — Hytale Plugin

Adds an in-game smartphone to Hytale servers. Players equip a phone item and interact with it to open a full-screen UI with a home screen, built-in apps (Whatgram, Calls, Contacts, Settings), and a public API for third-party plugins to register their own apps.
https://github.com/Esquirtle/Phonemod-Assets
This assets are required to run the compiled plugin.


---
## IF YOU CRASH WHEN TRY TO COMPILE THIS SOURCE CODE AND RUN THE PLUGIN WITHOUT THE PHONEMOD ASSETS ##
  https://github.com/Esquirtle/Phonemod-Assets
This assets are required to run the compiled plugin.
## Quick links

- **Full API docs** → [`docs/`](docs/README.md)
- **Build** → `mvn package -q` (output: `target/final/Phonemod.jar`)
- **Build with bundled assets** → `mvn clean package -P bundle -q` (includes `Phonemod-Assets/Common` and `Phonemod-Assets/Server` into `resources/`)
- **Deploy** → copy JAR to the server's `mods/` directory

---

## Project structure

```
src/main/java/esq/phonemod/
├── PhoneMod.java                     — plugin entry point
├── setup/
│   ├── SetupManager.java             — orchestrates startup
│   ├── ComponentRegistryManager.java — registers ECS components
│   ├── EventRegistryManager.java     — registers disconnect handler, voice filter
│   └── InteractionSetupManager.java  — registers open_phone item interaction
└── phone/
    ├── api/                          — public framework API (PhoneApp, StatefulPhoneApp,
    │                                    PhoneAppContext, PhoneEvent, PhoneEventActions,
    │                                    PhoneAssetPaths)
    ├── apps/                         — built-in apps (WhatgramApp, CallsApp,
    │                                    ContactsApp, SettingsApp)
    ├── components/                   — ECS components (PhoneOwnerComponent,
    │                                    PhoneAppSessionState, ConversationHistoryComponent,
    │                                    CallHistoryComponent)
    ├── core/                         — PhoneService singleton, PhoneAppRegistry
    ├── interactions/                 — OpenPhone item interaction
    ├── messaging/                    — PhoneRegistry (sessions), CallRegistry (calls),
    │                                    PhoneEventHandler (disconnect cleanup)
    └── ui/                           — PhonePage (routing hub), AppMenu, PhoneStatesEnum

assets/phonemod.ui/                   — client-side asset pack (UI files, icons)
docs/                                 — developer reference
```

---

## Architecture

### App framework

Apps are singletons registered with `PhoneService`. One instance serves all players — per-player state lives exclusively in `PhoneAppContext`.

```
PhoneService
  └─ PhoneAppRegistry
       └─ List<PhoneApp<?>>          one instance per registered app

PhonePage                            one instance per player per phone open
  ├─ currentApp: PhoneApp<?>
  └─ build() / handleDataEvent()
       └─ PhoneAppContext            created per-call; backed by PhoneAppSessionState
```

### Session registry

`PhoneRegistry` tracks active open phone sessions (`phoneNumber → OnlineEntry`). A single player can have multiple phones open simultaneously — each session is keyed by phone number, not player UUID. `CallRegistry` manages voice routing between call participants.

### State isolation

`PhoneAppSessionState` (ECS component) stores `Map<phoneNumber|appId, Map<key, value>>`. State is scoped per physical phone per app, so two phones owned by the same player never bleed state into each other.

---

## Adding a third-party app

### 1. Implement `StatefulPhoneApp`

```java
public final class MyApp extends StatefulPhoneApp<MyApp.State> {

    public enum State { LIST, DETAIL }

    public MyApp() { super(State.LIST); }

    @Override public String getId()           { return "myapp"; }
    @Override public String getDisplayName()  { return "My App"; }
    @Override public String getAppButtonUI()  { return "Pages/Playground/MyAppButton.ui"; }
    @Override public String getUIPath()       { return "Pages/Phone/MyApp.ui"; }

    @Override
    public void build(PhoneAppContext ctx, UICommandBuilder cmd, UIEventBuilder evb) {
        appendMainUI(cmd);
        // populate #AppContent based on getState(ctx)
    }

    @Override
    public boolean handleEvent(PhoneAppContext ctx, PhoneEvent event,
                               UICommandBuilder cmd, UIEventBuilder evb) {
        // handle events, update state, call build(), return true
        return false;
    }
}
```

### 2. Create the app button `.ui` file

Place at the path returned by `getAppButtonUI()` in your plugin's asset pack. Image paths inside `.ui` files are resolved relative to the file's own location.

```
// Pages/Playground/MyAppButton.ui
Group #APPBUTTONENTRY {
  LayoutMode: Top; FlexWeight: 1;
  Padding: (Horizontal: 3, Vertical: 3);
  Anchor: (Width: 84, Height: 100);
  TextButton #APPBUTTON {
    Anchor: (Width: 64, Height: 64);
    Style: (
      Default: (Background: "../Phone/MyApp.png"),
      Hovered:  (Background: #808080),
      Pressed:  (Background: #7a7a7a)
    );
  }
  Label #APPNAME {
    Anchor: (Top: 8, Width: 84, Height: 16);
    Text: "App";
    Style: (FontSize: 12, Alignment: Center, RenderBold: true);
  }
}
```

### 3. Register in your plugin's setup

```java
// Call PhoneService.initialize() first — it is idempotent, so safe to call
// even if phonemod's own setup() has not run yet (alphabetical load ordering).
PhoneService.initialize();
PhoneService.get().registerApp(new MyApp());
```

---

## Key event actions

| Constant | Value | Handled by |
|----------|-------|------------|
| `HOME` | `"home"` | PhonePage |
| `OPEN_APP` | `"open_app"` | PhonePage |
| `OPEN_CHAT` | `"open_chat"` | PhonePage |
| `ANSWER_CALL` | `"answer_call"` | PhonePage |
| `DECLINE_CALL` | `"decline_call"` | PhonePage |
| `HANG_UP` | `"hang_up"` | PhonePage |
| `BACK` | `"back"` | app |
| `SEND_MESSAGE` | `"send_message"` | WhatgramApp |
| `START_CALL` | `"start_call"` | CallsApp / WhatgramApp |

See [`PhoneEventActions`](src/main/java/esq/phonemod/phone/api/PhoneEventActions.java) for the full list.

---

## Persistence

| Component | Purpose |
|-----------|---------|
| `PhoneOwnerComponent` | Contact list, keyed by own phone number |
| `ConversationHistoryComponent` | Full message history across sessions |
| `CallHistoryComponent` | Call records (incoming/outgoing/missed) |
| `PhoneAppSessionState` | Live session state for apps (resets on logout) |

---

## Reference

| Topic | File |
|-------|------|
| Full API reference | [`docs/api-reference.md`](docs/api-reference.md) |
| Creating an app (step-by-step) | [`docs/creating-apps.md`](docs/creating-apps.md) |
| State management | [`docs/state-management.md`](docs/state-management.md) |
| Events & EventData bindings | [`docs/events.md`](docs/events.md) |
| Asset paths & icon conventions | [`docs/assets.md`](docs/assets.md) |
| PhonePage routing internals | [`docs/phone-navigation.md`](docs/phone-navigation.md) |
| Class roles & data flow | [`docs/architecture.md`](docs/architecture.md) |
