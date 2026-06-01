# PhoneMod — Hytale Plugin

Adds an in-game smartphone to Hytale servers. Players equip a phone item and interact with it to open a full-screen device UI with a home screen, built-in apps (Whatgram, Contacts, Calls, Settings, App Store), and a public API for third-party plugins to register their own apps.

Built on a generalized **device framework**: the phone is one device descriptor (`Server/Phonemod/Devices/Phone.json`) driving a data-defined shell, themes, and app grid — so new device types can be added as data, not code.

---

## ⚠️ Companion asset pack required

The compiled plugin needs the **Phonemod-Assets** pack (item model, all `.ui` files, icons, sounds, languages):

> https://github.com/Esquirtle/Phonemod-Assets

The plugin JAR ships only its built-in data (`Server/Phonemod/*`). Without the asset pack the `open_phone` interaction is registered but there is **no item to obtain and no UI to render**. Deploy both, or build the bundled JAR (below).

---

## Quick links

- **Full API docs** → [`docs/`](docs/README.md)
- **Build (plugin only)** → `./build.sh` → `target/final/Phonemod.jar`
- **Build (bundled with assets)** → `./build.sh --bundle` → `target/final/Phonemod-bundled.jar`
- **Deploy** → copy the JAR (and, for the plain build, the Phonemod-Assets pack) into the server's `mods/` directory

---

## Building

`build.sh` produces two flavours without modifying the committed `pom.xml`:

| Command | Output | Contents |
|---------|--------|----------|
| `./build.sh` | `target/final/Phonemod.jar` | Plugin code + built-in resources only (`Server/Phonemod/*`). Pair with the separate Phonemod-Assets pack. |
| `./build.sh --bundle` | both jars | Above **plus** `Phonemod-bundled.jar` — the full Phonemod-Assets tree (`Common/` + `Server/`) merged in. One drop-in JAR for users. |
| `./build.sh --bundle-only` | `Phonemod-bundled.jar` | Only the combined user JAR. |
| `./build.sh --clean` | — | `mvn clean` first (combinable with the above). |

The script auto-detects the asset pack and the local `HytaleServer.jar`; override with `PHONEMOD_ASSETS_DIR` / `HYTALE_SERVER_JAR`. Run `./build.sh --help` for details.

> The built-in resources and the pack do not collide at the file level (plugin uses `Server/Phonemod/`, the pack uses `Common/`, `Server/Audio`, `Server/Item`, `Server/Languages`); the bundle merges them and keeps the plugin's `manifest.json`.

---

## Project structure

```
src/main/java/esq/phonemod/
├── PhoneMod.java                     — plugin entry point (setup/start/shutdown)
├── setup/
│   ├── SetupManager.java             — orchestrates startup; initializeRuntime()/shutdown()
│   ├── AssetRegistryManager.java     — registers DeviceAsset + DeviceThemeAsset stores
│   ├── ComponentRegistryManager.java — registers ECS components (+ registerSystems hook)
│   ├── EventRegistryManager.java     — registers disconnect handler + voice filter
│   ├── CommandRegistryManager.java   — registers the /phonemod debug command
│   └── InteractionSetupManager.java  — registers the open_phone item interaction
├── device/                           — the generalized DEVICE FRAMEWORK
│   ├── api/                          — DevicePageHandle (live callback surface)
│   ├── assets/                       — DeviceAsset, DeviceThemeAsset, DeviceSoundProfile
│   ├── components/                   — DeviceSettings(+Component)
│   ├── core/                         — DeviceService, DeviceSession, DeviceShell,
│   │                                   DeviceRegistry, DevicePageState
│   ├── events/                       — PhoneEvents bus + typed device/phone events,
│   │                                   PhoneNotification(s)
│   ├── services/                     — Device{Call,Contact,Messaging,Notification,
│   │                                   Presence,VoiceRouting}Service (+ Phone* impls)
│   ├── theme/                        — ThemeService (palette → selector backgrounds)
│   └── ui/                           — DevicePage (the shell page + routing hub)
└── phone/
    ├── api/                          — public app API: PhoneApp, StatefulPhoneApp,
    │                                   PhoneAppContext, PhoneEvent, PhoneEventActions,
    │                                   PhoneUi, PhoneAssetPaths
    ├── apps/                         — built-in apps: WhatgramApp, ContactsApp,
    │                                   CallsApp, SettingsApp, AppStoreApp (+ helpers/)
    ├── components/                   — PhoneOwnerComponent, PhoneAppSessionState,
    │                                   ConversationHistoryComponent, CallHistoryComponent,
    │                                   CallRecord
    ├── core/                         — PhoneService singleton, PhoneAppRegistry
    ├── interactions/                 — OpenPhone item interaction
    └── messaging/                    — PhoneRegistry (sessions/presence/inbox),
                                        CallRegistry (calls + voice routing),
                                        PhoneEventHandler (disconnect cleanup),
                                        TextMessage / ChatMessage

docs/                                 — developer reference
build.sh                              — plugin / bundled build script
```

> The companion **Phonemod-Assets** repo holds the client-side pack (`Common/UI/...`, item, icons, sounds, languages).

---

## Architecture

### Device framework

A device is described by JSON (`Server/Phonemod/Devices/Phone.json`): shell `.ui` path, content selector, home holder, default apps, capabilities, default theme, and a sound profile. `DeviceService` builds a `DeviceShell` from that asset and a per-open `DeviceSession`; `DevicePage` is the custom UI page that renders the shell and routes events.

```
DeviceService (singleton)
  ├─ createShell(assetId)   → DeviceShell      (data from DeviceAsset)
  ├─ createSession(...)     → DeviceSession     (one per opened device)
  └─ createDevicePage(...)  → DevicePage        (the shell page + router)

PhoneService (singleton)
  └─ PhoneAppRegistry → List<PhoneApp<?>>       (one app instance serves all players)
```

`DevicePage` handles shell-level actions itself (`home`, `open_app`, `open_chat`, `answer_call`, `decline_call`, `hang_up`) and delegates everything else to the current app's `handleEvent`. Per-player state lives in `PhoneAppContext` (backed by the `PhoneAppSessionState` component) — app singletons hold no per-player state.

### Session registry & presence

`PhoneRegistry` tracks open phone sessions (`phoneNumber → OnlineEntry`) plus a `presence` map of connected-but-closed phones, so messages reach players whether the phone UI is open or not. A player may have multiple phones open at once — sessions are keyed by phone number, not player UUID. `CallRegistry` manages call state, a ring-timeout watcher, and private voice routing between participants.

### Theming

Themes are pure data (`DeviceThemeAsset`, `Server/Phonemod/Themes/*.json`): a map of *role → hex color*. A device declares which selectors play each role; `ThemeService` sets those selectors' backgrounds at render and on settings change.

### State isolation

`PhoneAppSessionState` (ECS component) stores `Map<phoneNumber|appId, Map<key, value>>`. State is scoped per physical phone per app, so two phones owned by the same player never bleed state into each other.

---

## Adding a third-party app

### 1. Implement `StatefulPhoneApp`

```java
public final class MyApp extends StatefulPhoneApp<MyApp.State> {

    public enum State { LIST, DETAIL }

    public MyApp() { super(State.LIST); }

    @Override public String getId()          { return "myapp"; }
    @Override public String getDisplayName() { return "My App"; }
    @Override public String getAppButtonUI() { return "Pages/Phone/Components/MyAppButton.ui"; }
    @Override public String getUIPath()      { return "Pages/Phone/MyApp.ui"; }

    @Override
    public void build(PhoneAppContext ctx, UICommandBuilder cmd, UIEventBuilder evb) {
        appendMainUI(ctx, cmd);                 // clears + appends this app's root
        // populate the app's own selectors based on getState(ctx)
    }

    @Override
    public boolean handleEvent(PhoneAppContext ctx, PhoneEvent event,
                               UICommandBuilder cmd, UIEventBuilder evb) {
        // handle events, update state via setState(ctx, ...), call build(), return true
        return false;
    }
}
```

> **Single-root rule:** an app owns its root under the shell content area (`#AppContent`) and must never replace the content area itself. Toggle child visibility and refill lists instead (see `WhatgramApp` / `ContactsApp`). Use the `PhoneUi` helpers for all bindings and list operations.

### 2. Create the app button `.ui` file

Place it at the path returned by `getAppButtonUI()` in your asset pack. Image paths inside `.ui` files resolve relative to the file's own location. The button root must expose `#APPBUTTON` (activates) and `#APPNAME` (label) — `DevicePage` binds the `open_app` action to it.

### 3. Register in your plugin's setup

```java
// PhoneService.initialize() is idempotent — safe to call even if phonemod's own
// setup() has not run yet (load ordering is not guaranteed).
PhoneService.initialize();
PhoneService.get().registerApp(new MyApp());
```

The App Store lists every registered app (including third-party ones) automatically.

See [`docs/creating-apps.md`](docs/creating-apps.md) and [`docs/scaffolds.md`](docs/scaffolds.md) for the full walkthrough.

---

## Key event actions

Shell actions are handled by `DevicePage`; the rest are delegated to the owning app.

| Constant | Value | Handled by |
|----------|-------|------------|
| `HOME` | `"home"` | DevicePage (shell) |
| `OPEN_APP` | `"open_app"` | DevicePage (shell) |
| `OPEN_CHAT` | `"open_chat"` | DevicePage → WhatgramApp |
| `ANSWER_CALL` | `"answer_call"` | DevicePage (shell) |
| `DECLINE_CALL` | `"decline_call"` | DevicePage (shell) |
| `HANG_UP` | `"hang_up"` | DevicePage (shell) |
| `BACK` | `"back"` | app |
| `SEND_MESSAGE` | `"send_message"` | WhatgramApp |
| `START_CALL` | `"start_call"` | ContactsApp / CallsApp / WhatgramApp |
| `OPEN_ADD_CONTACT` / `SAVE_CONTACT` / `REMOVE_CONTACT` / `CONTACTS` | … | ContactsApp |

See [`PhoneEventActions`](src/main/java/esq/phonemod/phone/api/PhoneEventActions.java) for the full list.

---

## Persistence

| Component | Purpose |
|-----------|---------|
| `PhoneOwnerComponent` | Contact list, keyed by own phone number |
| `ConversationHistoryComponent` | Full message history across sessions |
| `CallHistoryComponent` | Call records (incoming / outgoing / missed) |
| `DeviceSettingsComponent` | Per-device settings (theme, wallpaper, etc.) |
| `PhoneAppSessionState` | Live per-app session state (resets on logout) |

---

## Admin / debug command

`/phonemod sendtext <number> <message>` — sends a message to a phone number as `[Server]`, for testing the messaging pipeline without a second player. Gated behind the `phonemod.admin.sendtext` permission (operators by default).

---

## Reference

| Topic | File |
|-------|------|
| Documentation index | [`docs/README.md`](docs/README.md) |
| Full API reference | [`docs/api-reference.md`](docs/api-reference.md) |
| Creating an app (step-by-step) | [`docs/creating-apps.md`](docs/creating-apps.md) |
| App + device scaffolds | [`docs/scaffolds.md`](docs/scaffolds.md) |
| State management | [`docs/state-management.md`](docs/state-management.md) |
| Events & EventData bindings | [`docs/events.md`](docs/events.md) |
| Asset paths & icon conventions | [`docs/assets.md`](docs/assets.md) |
| Theming | [`docs/theming.md`](docs/theming.md) |
| DevicePage routing & call flow | [`docs/phone-navigation.md`](docs/phone-navigation.md) |
| Class roles & data flow | [`docs/architecture.md`](docs/architecture.md) |
