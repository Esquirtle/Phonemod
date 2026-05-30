# Assets

## Asset-pack manifest (REQUIRED for images)

Custom UI images only reach the client if the asset pack ships them. In the
asset pack's `manifest.json`:

```json
"IncludesAssetPack": true
```

With this `false`, `.ui` markup still renders (it is server-pushed) but every
custom image is blank — a confusing "the UI works but images don't" symptom.

## Image rules

1. **`@2x` suffix is mandatory on disk.** A reference `"Foo.png"` loads
   `Foo@2x.png`. Name files `X@2x.png`; reference them **without** the suffix.
2. **Paths resolve relative to the `.ui` file that *writes* the property.**
   `"Foo.png"` = next to that file; `"../Foo.png"` = up one folder.
3. **Template gotcha.** A path passed into a DustLib named-expression (e.g.
   `@Icon`/`@Logo`) is consumed *inside* `DustLib.ui` (which lives in `Pages/`),
   so it resolves from there, **not** from the caller. An app button in
   `Pages/Phone/Components/` therefore passes `"Phone/Whatgram.png"` (relative to
   `Pages/`), not `"../Whatgram.png"`. Inline images in a page resolve against
   that page's own folder as normal.

## PhoneAssetPaths

`esq.phonemod.phone.api.PhoneAssetPaths` — paths are relative to
`Common/UI/Custom/`.

| Constant | Path |
|---|---|
| `PHONE_UI` | `Pages/Phone/Phone.ui` |
| `DUST_WHATGRAM_UI` | `Pages/Phone/DustWhatgram.ui` |
| `DUST_CONTACTS_UI` | `Pages/Phone/DustContacts.ui` |
| `DUST_CALLS_UI` | `Pages/Phone/DustCalls.ui` |
| `DUST_SETTINGS_UI` | `Pages/Phone/DustSettings.ui` |
| `WHATGRAM_BUTTON_UI` / `CONTACTS_BUTTON_UI` / `CALLS_BUTTON_UI` / `SETTINGS_BUTTON_UI` | `Pages/Phone/Components/<App>Button.ui` |

Component-fragment paths (list rows, chat bubbles, call overlays) live in each
app's helper class — `Whatgram.ENTRY_UI`, `Whatgram.BUBBLE_UI`,
`Contacts.CONTACTS_ENTRY_UI`, `Calls.HISTORY_ENTRY_UI`, etc. — so every path and
selector for an app sits in one place.

## DustLib component library

`Pages/DustLib.ui` is the single source of the "Dust" look: design tokens
(`@Dust*`) plus reusable components. App and shell `.ui` files import it
(`$D = "../DustLib.ui"`) and compose its members:

- **App chrome:** `@DustShellHeader`, `@DustShellPanel`, `@DustScrollList`,
  `@DustShellListEntry`, `@DustShellActionButton` / `…SecondaryButton` /
  `…DangerButton`, `@DustShellMessageInput`.
- **Shell chrome** (each owns a framework selector): `@DustStatusBar` (`#TopBar`),
  `@DustHomeBar` (`#BottomBar` + `#HomeButton`), `@PhoneAppButton`
  (`#APPBUTTONENTRY` / `#APPBUTTON` / `#APPNAME`).
- **Full-screen:** `@DustCenterScreen` (call overlays / prompts).

Themes are data (`Server/Phonemod/Themes/*.json`) applied at runtime by
`ThemeService` onto the themeable selectors each device and app declares. Do
**not** add alternate-theme `.ui` files — named expressions are file-scoped.

## App-button contract

Return a button `.ui` path from `getAppButtonUI()`. It must resolve to a
`#APPBUTTONENTRY` root containing `#APPBUTTON` (TextButton) and `#APPNAME`
(Label). The built-ins are 3-line instances of `@PhoneAppButton`:

```
$D = "../../DustLib.ui";
$D.@PhoneAppButton #APPBUTTONENTRY { @Icon = "Phone/Whatgram.png"; }
```

`DevicePage` sets `#APPNAME.Text` to `getDisplayName()` and binds `open_app` on
`#APPBUTTON` at runtime; the icon is baked via `@Icon` (mind the template
gotcha above).

## Third-party apps

| Type | Location pattern |
|---|---|
| App root UI | `Pages/<Namespace>/<AppName>.ui` |
| Component fragment | `Pages/<Namespace>/Components/<AppName><Part>.ui` |
| App button | `Pages/<Namespace>/<AppName>Button.ui` (from `getAppButtonUI()`) |
| Icon PNG | ship as `<icon>@2x.png`; reference it without `@2x` |
