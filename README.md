# Phone App Framework — Documentation

This folder contains the developer reference for the Phone App Framework built into the `phonemod` Hytale plugin.

## Contents

| File | Description |
|------|-------------|
| [PROFESSIONALIZATION_PLAN.md](PROFESSIONALIZATION_PLAN.md) | Gated roadmap from 1.0 to the public, generic 2.0 device-platform SDK |
| [architecture.md](docs/architecture.md) | Class roles, data flow, and design decisions |
| [api-reference.md](docs/api-reference.md) | Legacy v1 signatures: `PhoneApp`, `StatefulPhoneApp`, `PhoneAppContext`, `PhoneEvent`, **`PhoneUi`**, `PhoneService` |
| [creating-apps.md](docs/creating-apps.md) | Step-by-step v1 app guide; **normative async + threading rules** (Step 6b) |
| [scaffolds.md](docs/scaffolds.md) | Copy-paste v1 app + device scaffolds, selector checklist, payload naming |
| [state-management.md](docs/state-management.md) | Per-player state deep dive and typed access patterns |
| [events.md](docs/events.md) | PhoneEvent, PhoneEventActions constants, EventData binding patterns |
| [assets.md](docs/assets.md) | Asset paths, image rules (`@2x` + `IncludesAssetPack`), DustLib components |
| [theming.md](docs/theming.md) | Theme palettes and runtime application |
| [phone-navigation.md](docs/phone-navigation.md) | DevicePageState, call flow, routing hub internals |
| [ROUTING.md](docs/ROUTING.md) | Declarative routing pipeline and extension points |
| [SYNC.md](docs/SYNC.md) | Realtime sync channels: the `LegacyRooms` consumer API vs. runtime-only internals, persistence, rooms |
| [V2_API.md](docs/V2_API.md) | Published v2 API reference — full signatures for every service, descriptor, action, and the `esq.phonemod.api.event` package |
| [V2_APP_TUTORIAL.md](docs/V2_APP_TUTORIAL.md) | Owner-bound typed app tutorial; why v2 apps are synchronous today |
| [examples/hello-device-app/](examples/hello-device-app) | **Runnable** standalone v2 `DeviceApp` project (verified to compile) |
| [V2_DEVICE_TUTORIAL.md](docs/V2_DEVICE_TUTORIAL.md) | Personal and shared device integration |
| [ASSET_CONVENTIONS.md](docs/ASSET_CONVENTIONS.md) | Runtime asset layout and startup validation contract |
| [THREADING.md](docs/THREADING.md) | World, network, and persistence thread rules |
| [MIGRATION_V1_TO_V2.md](docs/MIGRATION_V1_TO_V2.md) | Data and source migration/rollback guide |
| [COMPATIBILITY.md](docs/COMPATIBILITY.md) | Compatibility, SemVer, deprecation, and support policy |
| [CONFIGURATION.md](docs/CONFIGURATION.md) | In-phone development controls, persisted keys, defaults, and runtime behavior |
| [VERIFICATION_EVIDENCE.md](docs/VERIFICATION_EVIDENCE.md) | Build/test, consumer-compile matrix, and boot evidence with dates |

## Quick start

### Writing a new app (v2)

1. Copy [`examples/hello-device-app/`](examples/hello-device-app) — a standalone, compiling
   v2 `DeviceApp` project.
2. Follow [V2_APP_TUTORIAL.md](docs/V2_APP_TUTORIAL.md) for the walkthrough.
3. Keep [V2_API.md](docs/V2_API.md) open as the signature reference.

Note that v2 apps render **synchronously**; async data and realtime shared state still
require the v1 bridge — see
[V2_APP_TUTORIAL.md](docs/V2_APP_TUTORIAL.md#v2-apps-are-synchronous-today).

### Working on the framework itself

1. Read [PROFESSIONALIZATION_PLAN.md](PROFESSIONALIZATION_PLAN.md) before making changes
   intended for the 1.1 or 2.0 release lines.
2. Read [architecture.md](docs/architecture.md) to understand the current class
   relationships.
3. Read [creating-apps.md](docs/creating-apps.md) for the v1 walkthrough and the normative
   async/threading rules.
4. Refer to [api-reference.md](docs/api-reference.md) and [events.md](docs/events.md) while
   implementing.
