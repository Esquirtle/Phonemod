# Phone App Framework — Documentation

This folder contains the developer reference for the Phone App Framework built into the `phonemod` Hytale plugin.

## Contents

| File | Description |
|------|-------------|
| [architecture.md](architecture.md) | Class roles, data flow, and design decisions |
| [api-reference.md](api-reference.md) | Every public API method with signatures and descriptions |
| [creating-apps.md](creating-apps.md) | Step-by-step guide to building a new phone app |
| [scaffolds.md](scaffolds.md) | Copy-paste app + device scaffolds, selector checklist, payload naming |
| [required-selectors.md](required-selectors.md) | The shell/app selector contract and per-app selectors |
| [state-management.md](state-management.md) | Per-player state deep dive and typed access patterns |
| [events.md](events.md) | PhoneEvent, PhoneEventActions constants, EventData binding patterns |
| [assets.md](assets.md) | Asset paths, image rules (`@2x` + `IncludesAssetPack`), DustLib components |
| [theming.md](theming.md) | Theme palettes and runtime application |
| [phone-navigation.md](phone-navigation.md) | DevicePageState, call flow, routing hub internals |

## Quick start

1. Read [architecture.md](architecture.md) to understand the class relationships.
2. Read [creating-apps.md](creating-apps.md) for a guided walkthrough.
3. Refer to [api-reference.md](api-reference.md) and [events.md](events.md) while implementing.
