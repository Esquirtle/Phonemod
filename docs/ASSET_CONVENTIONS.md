# Asset conventions

The production JAR is the asset pack. Use `Common/` for UI, textures, and client-visible
resources; use `Server/` for device/theme JSON, items, interactions, sounds, and languages.
Do not ship a second production Phonemod asset pack.

Device IDs and theme IDs are filenames. UI paths are relative to `Common/UI/Custom/`.
Selectors must be simple named selectors such as `#AppContent`. Device JSON must declare a
type, shell path, content/home selectors, metadata key, unique default apps, capabilities,
default theme, and `CallAudioPolicy` (`Private`, `Speaker`, or `Toggleable`). Private is the
default.

All user-visible text should be a localization key. Images use Hytale's density convention:
reference `Icon.png` while providing `Icon@2x.png`. Shared layout/tokens belong in
`Pages/DustLib.ui`; app documents own their internal selectors and empty/loading/error/
disabled states.

Startup validation is authoritative. UI visibility is never permission or compatibility
authorization.
