# PhoneMod (Hytale) — Developer README

>A concise guide for developers who want to understand and extend the PhoneMod smartphone plugin.

## Project overview
- Purpose: adds an in-game smartphone item with apps, UI pages, interactions and messaging for Hytale servers.
- Main gameplay/features: a handheld phone item, app system (multiple apps/screens), in-game notifications, send/receive messaging and UI-driven interactions.

## Architecture summary
- Entry point: [server/src/main/java/esq/phonemod/PhoneMod.java](server/src/main/java/esq/phonemod/PhoneMod.java) — registers managers and bootstraps setup.
- Core managers: `SetupManager` coordinates startup; `AssetRegistryManager`, `CommandRegistryManager`, `ComponentRegistryManager`, and `InteractionSetupManager` register assets, commands, components and interactions respectively (see [server/src/main/java/esq/phonemod/setup](server/src/main/java/esq/phonemod/setup)).
- Packages of interest:
  - `phone/components` — component definitions and persistent data hooks.
  - `phone/interactions` — item interactions and behavior registration.
  - `phone/messaging` — networking/packet-like messaging and message handling.
  - `phone/ui` — UI controller classes and client UI bindings.

## Project structure (important folders)

```
phonemod/
├─ server/src/main/java/esq/phonemod/
│  ├─ PhoneMod.java
│  ├─ setup/                 # registration managers
│  └─ phone/
│     ├─ components/         # persistent components
│     ├─ interactions/       # item/app interactions
│     ├─ messaging/          # in-plugin messages / packets
│     └─ ui/                 # UI controllers
└─ server/src/main/resources/
   ├─ Common/UI/Custom/Pages  # .ui pages for phone screens
   └─ Server/Item/Items       # Phone item JSON
```

## How it works — quick technical summary

- Smartphone lifecycle
  - The phone is defined as an Item asset (resources JSON). When equipped/used, interaction handlers (in `phone/interactions`) open the UI and create or bind `phone` components for the player.
- App system
  - Apps are driven by `PhonePage` state. The home screen menu is defined in `src/main/resources/Common/UI/Custom/Pages/Phone/AppMenu.ui`; app buttons send `open_app` actions to `PhonePage`, which then loads the selected app state and UI fragment.
- Client/server communication
  - The plugin uses a messaging layer (`phone/messaging`) to serialize and route UI requests, notifications and app actions between client and server. Treat these classes as packet definitions and handlers.
- UI handling
  - UI pages (.ui files) live in resources and are opened via server-side helpers. UI controllers receive events and call into managers or components for state changes.
- Save data
  - Player-related state is stored in components under `phone/components` and persisted using the server's component/codec patterns; use the `ComponentRegistryManager` for registration and serialization.
- Initialization flow
  - `PhoneMod` → `SetupManager` → individual registry managers (assets, commands, components, interactions). Registrations link JSON assets, UI pages and handlers so the phone becomes available at runtime.

## Main managers / extension points
- `SetupManager` ([server/src/main/java/esq/phonemod/setup/SetupManager.java](server/src/main/java/esq/phonemod/setup/SetupManager.java)) — central bootstrapping.
- `AssetRegistryManager` — register item assets and UI pages.
- `ComponentRegistryManager` — register persistent components and codecs.
- `InteractionSetupManager` — register item interactions (open UI, button handlers).
- `CommandRegistryManager` — in-game commands used for testing or debug.

These are the primary places to hook extensions: add registrations here so systems are discovered at startup.

## Networking & synchronization
- Use `phone/messaging` to add new message types and handlers. Keep messages small and idempotent; validate all client-sent data server-side.
- Follow existing message handler patterns to route incoming messages to the relevant `phone/ui` controller or component update.

## How to expand the plugin (practical recipes)

- Add a new smartphone app
  1. Add an app icon/button to `src/main/resources/Common/UI/Custom/Pages/Phone/AppMenu.ui`.
  2. Add an event binding in `src/main/java/esq/phonemod/phone/ui/AppMenu.java` for the new button. Use `Action=open_app` and a unique `App` identifier.
  3. Update `src/main/java/esq/phonemod/phone/ui/PhonePage.java` in `openApp(...)` to handle the new app identifier and render your app state.
  4. Create any required UI fragments under `src/main/resources/Common/UI/Custom/Pages/Phone/` and load them from `PhonePage` using `UICommandBuilder`/`UIEventBuilder`.
  
  Note: this plugin routes app launches through the `PhonePage` state machine, not through a separate interaction. The phone home screen (`AppMenu`) dispatches app IDs to `PhonePage.openApp`.

- Add UI screens
  - Write .ui markup, localize strings in `src/main/resources/Server/Languages/en-US/playground.lang`, and then inject or append the screen content from `PhonePage` using the existing UI builder APIs.

- Add packets / network logic
  - Implement new message classes and handlers under `phone/messaging`. If the UI sends new actions, extend `PhonePage.PhoneEventData` and update `PhonePage.handleDataEvent(...)` to interpret them. Register any server-side handlers as needed.

- Add persistent player data
  - Define a component in `phone/components` with a BuilderCodec for serialization. Register it with `ComponentRegistryManager`. Read and write it inside `PhonePage` event handlers or interaction code so player state persists automatically.

- Extend gameplay safely
  - Use `CommandBuffer` or the server stores/APIs for entity changes. Keep new state within `PhonePage` and components; avoid global mutable state. If you need periodic updates or cross-entity behavior, register a dedicated system rather than stuffing logic into `PhonePage`.

## Event flow (typical request path)
1. Player activates phone item → interaction handler (in `phone/interactions`).
2. Interaction opens UI page (resources UI). Server binds UI to controller.
3. Client UI events send messages via `phone/messaging` → server handler.
4. Server handler updates components or triggers actions (commands, notifications).
5. Server optionally sends UI updates/notifications to client.

## Recommended reference files
- [server/src/main/java/esq/phonemod/PhoneMod.java](server/src/main/java/esq/phonemod/PhoneMod.java)
- [server/src/main/java/esq/phonemod/setup/SetupManager.java](server/src/main/java/esq/phonemod/setup/SetupManager.java)
- [server/src/main/java/esq/phonemod/setup/ComponentRegistryManager.java](server/src/main/java/esq/phonemod/setup/ComponentRegistryManager.java)
- [server/src/main/java/esq/phonemod/phone/messaging](server/src/main/java/esq/phonemod/phone/messaging)
- `server/src/main/resources/Common/UI/Custom/Pages` (UI files)
- `server/src/main/resources/Server/Item/Items/Phone.json` (item definition)

## Next steps
- I added this README to the project root. If you want, I can:
  - open or patch example controller/component scaffolding for a new app,
  - or run a quick build (`mvn package -q`) to verify there are no resource/compilation mismatches.

---
Short, practical, and focused on extension points — tell me which follow-up example you'd like (scaffold an app, add a UI, or add a persistent component).
