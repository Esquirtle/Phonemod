# Phone App Framework — Documentation

This folder contains the developer reference for the Phone App Framework built into the `phonemod` Hytale plugin.

## Contents

| File | Description |
|------|-------------|
| [architecture.md](docs/architecture.md) | Class roles, data flow, and design decisions |
| [api-reference.md](docs/api-reference.md) | Core app-facing API signatures and descriptions |
| [creating-apps.md](docs/creating-apps.md) | Step-by-step guide to building a new phone app |
| [scaffolds.md](docs/scaffolds.md) | Copy-paste app + device scaffolds, selector checklist, payload naming |
| [state-management.md](docs/state-management.md) | Per-player state deep dive and typed access patterns |
| [events.md](docs/events.md) | PhoneEvent, PhoneEventActions constants, EventData binding patterns |
| [assets.md](docs/assets.md) | Asset paths, image rules (`@2x` + `IncludesAssetPack`), DustLib components |
| [theming.md](docs/theming.md) | Theme palettes and runtime application |
| [phone-navigation.md](docs/phone-navigation.md) | DevicePageState, call flow, routing hub internals |
| [ROUTING.md](docs/ROUTING.md) | Declarative routing pipeline and extension points |
| [SYNC.md](docs/SYNC.md) | Realtime sync channels, persistence, and rooms |

## Quick start

1. Read [architecture.md](docs/architecture.md) to understand the class relationships.
2. Read [creating-apps.md](docs/creating-apps.md) for a guided walkthrough.
3. Refer to [api-reference.md](docs/api-reference.md) and [events.md](docs/events.md) while implementing.
