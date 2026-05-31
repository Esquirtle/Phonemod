# Device Theming

Phonemod themes are **data-driven** and applied at **runtime by Java**. This is a
deliberate consequence of how Hytale `.ui` works: named expressions (`@Token`)
are **file-scoped** and **cannot be overridden across documents**, and imports are
**not re-exported transitively**. So you cannot "swap a palette file" the way CSS
lets you swap a stylesheet — a wrapper `.ui` that just imports a theme file
exports nothing.

Instead:

| Layer | What it holds | Where |
|-------|---------------|-------|
| Component library | Structure + the **default** palette baked into tokens | `Common/UI/Custom/Pages/DustLib.ui` |
| Theme palettes | `role → hex color` maps, as data | `Server/Phonemod/Themes/<Theme>.json` (`DeviceThemeAsset`) |
| Themeable selectors | `role → on-screen selector`, per device / per app | device asset `Selectors` map + `PhoneApp.getThemeableSelectors()` |
| Applier | Sets `selector.Background = color` at runtime | `ThemeService` |

## How a theme is applied

1. `DevicePage.build()` appends the shell, then calls
   `ThemeService.applyShell(cmd, shell, themeId)`.
2. `DevicePage.openApp()` calls `ThemeService.apply(...)` with the opened app's
   `getThemeableSelectors()`.
3. The theme id is resolved from the player's saved `DeviceSettings` (falling
   back to the device asset's `DefaultThemeId`).
4. When the Settings app changes the theme it posts `DeviceSettingsChangedEvent`;
   a listener registered in `SetupManager` calls `DevicePageHandle.reapplyTheme`,
   which re-themes the live page without a reload.

`ThemeService` only sets **`Background`** colors. Text colors live inside `Style`
objects and cannot be addressed through a selector path, so they stay baked into
the library defaults. Setting a color on an absent selector is a safe no-op, so an
app's themeable selectors can be listed even when that app is off-screen.

## Roles

Role keys are arbitrary strings; the **intersection** of a device/app's
`role → selector` map and a theme's `role → color` map is what gets themed.
Current roles:

| Role | Phone shell selector | Notes |
|------|----------------------|-------|
| `topbar` | `#TopBar` | declared in `Devices/Phone.json` `Selectors` |
| `bottombar` | `#BottomBar` | declared in `Devices/Phone.json` `Selectors` |
| `appContent` | `#WhatgramContent` | declared via `WhatgramApp.getThemeableSelectors()` |
| `appHeader`, `appPanel` | — | reserved; present in palettes for Dust apps to opt into |

## Adding a theme

Create `Server/Phonemod/Themes/<Name>.json` with a `Colors` map. No code change
needed — the file id becomes the theme id (e.g. used by `DeviceSettings.themeId`).

## Adding a themeable element

1. Give the element a stable `#Id` in its `.ui` file.
2. Add `role → "#Id"` to the device asset `Selectors` map (shell-level) **or**
   override `PhoneApp.getThemeableSelectors()` (app-level).
3. Add `role → color` to each theme palette JSON.

## Known limitation

Color injection cannot change **layout/sizing** (e.g. a genuinely "compact"
theme with smaller paddings). The `Compact` palette is therefore a color variant
only. Size-based themes would require either per-theme library documents or
sending size values to additional non-color properties — out of scope for the
runtime color-injection model.
