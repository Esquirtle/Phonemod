# Assets

## PhoneAssetPaths

`esq.phonemod.phone.api.PhoneAssetPaths`

String constants for every UI and icon asset path used by the built-in phone framework. All paths are relative to the game's asset root.

### Phone frame

| Constant | Path | Description |
|----------|------|-------------|
| `PHONE_UI` | `Pages/Phone/Phone.ui` | Outer phone shell — background, top bar, bottom bar |
| `APP_MENU_UI` | `Pages/Phone/AppMenu.ui` | Home screen app-launcher grid |

### Built-in app UI roots

Loaded into `#AppContent` via `appendMainUI()`.

| Constant | Path |
|----------|------|
| `WHATGRAM_UI` | `Pages/Phone/Whatgram.ui` |
| `CONTACTS_UI` | `Pages/Phone/Contacts.ui` |
| `CALLS_UI` | `Pages/Phone/Calls.ui` |
| `SETTINGS_UI` | `Pages/Phone/Settings.ui` |

### Component fragments

Appended into list containers within app UIs.

| Constant | Path |
|----------|------|
| `WHATGRAM_CHAT_ENTRY_UI` | `Pages/Phone/Components/WhatgramChatEntry.ui` |
| `WHATGRAM_CHAT_UI` | `Pages/Phone/Components/WhatgramChat.ui` |
| `WHATGRAM_BUBBLE_UI` | `Pages/Phone/Components/WhatgramMessageBubble.ui` |
| `CONTACTS_ENTRY_UI` | `Pages/Phone/Components/ContactsEntry.ui` |
| `CONTACTS_ADD_UI` | `Pages/Phone/Components/ContactsAdd.ui` |
| `CALL_HISTORY_ENTRY_UI` | `Pages/Phone/Components/CallHistoryEntry.ui` |
| `INCOMING_CALL_UI` | `Pages/Phone/Components/IncomingCall.ui` |
| `ACTIVE_CALL_UI` | `Pages/Phone/Components/ActiveCall.ui` |
| `APP_BUTTON_ENTRY_UI` | `Pages/Phone/Components/AppButtonEntry.ui` |

## Conventions for third-party apps

Follow the same naming and placement conventions as the built-in apps.

### UI files

| Type | Location pattern |
|------|-----------------|
| App root UI | `Pages/Phone/<AppName>.ui` |
| Component fragment | `Pages/Phone/Components/<AppName><ComponentName>.ui` |

### Button `.ui` file

| Type | Location pattern |
|------|-----------------|
| Built-in app button | `Pages/Phone/Components/<AppName>Button.ui` |
| Third-party app button | `Pages/<PluginNamespace>/AppIcon.ui` |

Return this path from `getAppButtonUI()`. The file must declare `#APPBUTTONENTRY` → `#APPBUTTON` (TextButton with icon in `Default` style) + `#APPNAME` (Label). Reference the icon PNG using a **relative path** from the button file's location.

### Examples

```
Pages/Phone/Notes.ui
Pages/Phone/Components/NotesList.ui
Pages/Phone/Components/NotesEdit.ui
Pages/Phone/Components/NotesButton.ui    ← getAppButtonUI()
Pages/Phone/Notes.png                    ← icon PNG (referenced as "../Notes.png" inside NotesButton.ui)
```

## AppMenu icon rendering

`AppMenu.build()` iterates `PhoneService.get().getApps()` and for each app:

1. Calls `cmd.append(APP_HOLDER, app.getAppButtonUI())` — loads the app's dedicated button `.ui` file with its icon already hardcoded in the `Default` style.
2. Calls `cmd.set(entrySelector + " #APPNAME.Text", app.getDisplayName())` — label text is a `Text` property and can be set dynamically.
3. Binds `open_app` event to `#APPHolder[i] #APPBUTTON` with payload `App = getId()`.

If `getAppButtonUI()` returns `null` or blank the app is skipped with a warning log. TextButton style backgrounds cannot be changed after parse time via `cmd.set()` or `cmd.setObject()` — embedding the path in the `.ui` file is the only reliable way.

> **Image path resolution:** inside `.ui` files, image paths are resolved **relative to the `.ui` file's own location** in the asset namespace. A button file at `Pages/Phone/Components/WhatgramButton.ui` references its icon as `"../Whatgram.png"` (resolves to `Pages/Phone/Whatgram.png`).
