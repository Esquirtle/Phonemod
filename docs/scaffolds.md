# Scaffolds & Checklists

Copy-paste starting points for new apps and devices, plus quick validation
checklists. For the full explanations see `creating-a-phone-app.md`.

---

## 1. New app scaffold

Five files. Replace `MyApp` / `myapp` / `my_app` throughout. Drop the `.ui` files
under `Common/UI/Custom/`, the `.lang` under `Server/Languages/en-US/`, and ship
`Pages/Phone/MyApp@2x.png` as the icon.

### `MyApp.java`

```java
package com.example.myapp;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import esq.phonemod.phone.api.PhoneAppContext;
import esq.phonemod.phone.api.PhoneEvent;
import esq.phonemod.phone.api.PhoneUi;
import esq.phonemod.phone.api.StatefulPhoneApp;

public final class MyApp extends StatefulPhoneApp<MyApp.Screen> {

    public enum Screen { MAIN }

    // App-owned action + selector constants (one source of truth).
    private static final String ACTION_PING = "myapp_ping";
    private static final String SEL_CONTENT = "#MyAppContent";
    private static final String SEL_PING    = "#PingButton";

    public MyApp() { super(Screen.MAIN); }

    @Override public String getId()          { return "example_my_app"; } // globally unique
    @Override public String getDisplayName() { return "My App"; }
    @Override public String getAppButtonUI() { return "Pages/Phone/Components/MyAppButton.ui"; }
    @Override public String getUIPath()      { return "Pages/Phone/MyApp.ui"; }

    @Override
    public void build(PhoneAppContext ctx, UICommandBuilder cmd, UIEventBuilder evb) {
        appendMainUI(ctx, cmd);                       // clears content area, appends root
        PhoneUi.bindAction(evb, SEL_PING, ACTION_PING);
        PhoneUi.safeClear(cmd, SEL_CONTENT);          // populate your own container
        // … append rows / set text here …
    }

    @Override
    public boolean handleEvent(PhoneAppContext ctx, PhoneEvent event,
                               UICommandBuilder cmd, UIEventBuilder evb) {
        if (ACTION_PING.equals(event.getAction())) {
            ctx.sendNotification("My App", "Pong!");
            build(ctx, cmd, evb);                     // always rebuild after a handled event
            return true;
        }
        return false;
    }
}
```

### `Pages/Phone/MyApp.ui`

```
$D = "../DustLib.ui";

Group {
  FlexWeight: 1;
  LayoutMode: Top;

  $D.@DustShellHeader #MyAppHeader { @Title = "My App"; }

  $D.@DustShellPanel #MyAppPanel {
    FlexWeight: 1;
    $D.@DustShellActionButton #PingButton { Anchor: (Height: 34, Bottom: $D.@DustShellSpacing); Text: "Ping"; }
    $D.@DustScrollList #MyAppContent { }
  }
}
```

### `Pages/Phone/Components/MyAppButton.ui`

```
$D = "../../DustLib.ui";
// @Icon is consumed inside DustLib (Pages/), so path is relative to Pages/.
$D.@PhoneAppButton #APPBUTTONENTRY { @Icon = "Phone/MyApp.png"; }
```

### `Pages/Phone/Components/MyAppEntry.ui` (one list row, optional)

```
$D = "../../DustLib.ui";
$D.@DustShellListEntry #MyAppEntry {
  Anchor: (Height: $D.@DustShellListEntryHeight, Bottom: 6);
  Label #RowLabel { FlexWeight: 1; Text: "Row"; Style: (FontSize: 14, TextColor: $D.@DustTextPrimary); }
  $D.@DustShellActionButton #RowButton { Anchor: (Width: 64, Height: 38); Text: "Open"; }
}
```

### `Server/Languages/en-US/myapp.lang`

```properties
myapp.display_name = My App
myapp.ping = Ping
```

### Register (in your plugin `setup()`, after `PhoneService.initialize()`)

```java
PhoneService.get().registerApp(new MyApp());
```

---

## 2. New device asset template

`Server/Phonemod/Devices/MyDevice.json` (key = filename → `MyDevice`).

```json
{
  "DisplayNameKey": "mymod.device.mydevice",
  "DeviceType": "tablet",
  "ShellUiPath": "Pages/MyDevice/MyDevice.ui",
  "ContentSelector": "#AppContent",
  "HomeHolderSelector": "#APPHolder",
  "HomeButtonSelector": "#HomeButton",
  "MetadataKey": "DeviceId",
  "DefaultApps": ["contacts", "settings"],
  "Capabilities": ["apps", "notifications"],
  "DefaultThemeId": "Default",
  "Selectors": { "topbar": "#TopBar", "bottombar": "#BottomBar" }
}
```

- Omit `"Id"` (inferred from filename); add `"Parent": "Phone"` to inherit.
- **Capabilities gate behavior** — only `calls`-capable devices show call overlays;
  only `messaging`-capable devices route message events. Add `messaging`,
  `calls`, `voice_route`, `contacts` as needed.
- The shell `.ui` should compose `@DustStatusBar #TopBar`, `@DustHomeBar #BottomBar`,
  an empty `Group #AppContent`, and provide `#HomeButton` (inside `@DustHomeBar`).

---

## 3. Selector checklist

Before first run, confirm:

**Manifest / assets**
- [ ] asset pack `manifest.json` has `"IncludesAssetPack": true`
- [ ] every UI image is named `X@2x.png` and referenced **without** `@2x`
- [ ] template-passed image paths (`@Icon` etc.) are relative to `Pages/`, not the caller

**App button** (`getAppButtonUI()`)
- [ ] root `#APPBUTTONENTRY`, child `TextButton #APPBUTTON`, child `Label #APPNAME`
- [ ] (easiest: `$D.@PhoneAppButton #APPBUTTONENTRY { @Icon = "Phone/X.png"; }`)

**App root** (`getUIPath()`)
- [ ] owns a container you clear/fill yourself (don't re-clear the shell content area)
- [ ] themeable roots (if any) match `getThemeableSelectors()` (`appHeader`/`appPanel`/`appContent`)

**Shell** (custom devices)
- [ ] element IDs match the device JSON: `ContentSelector`, `HomeHolderSelector`,
      `HomeButtonSelector`, `Selectors.topbar`, `Selectors.bottombar`

**Java**
- [ ] use `ctx.getContentSelector()` — never hardcode `"#AppContent"`
- [ ] `handleEvent` returns `true` and calls `build()` for actions it handles
- [ ] per-player data via `ctx.getState/setState`, never instance fields

---

## 4. Event-payload naming

- **Reserved (shell-owned):** `Action`, `App`. Actions `home`, `open_app`,
  `open_chat`, `answer_call`, `decline_call`, `hang_up` are intercepted by
  `DevicePage` before your app sees them.
- **App-owned keys just work** — `DevicePage` preserves any extra string field in
  `event.getParams()`. No framework/codec edits.
- **Name keys for intent, not by reuse.** Prefer `ProductId` / `ListingId` /
  `ItemId` over the phone's `Contact`; prefer `MessageText` over a generic `Value`.
- **Capture a live input** with `PhoneUi.bindCapturedText(evb, btn, action, "Key", "#Input.Value")`
  → read via `event.getParam("Key")` (the `@` prefix is added/stripped for you).
- **Prefix custom actions** to avoid collisions: `myapp_open`, not `open`.
```
