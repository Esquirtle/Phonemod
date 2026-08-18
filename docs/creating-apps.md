# Creating a Phone App (legacy v1)

New apps should follow [V2_APP_TUTORIAL.md](V2_APP_TUTORIAL.md). This guide remains for the
2.x compatibility bridge.

## Step 1: Declare a dependency on the phone framework

Your plugin must depend on the phonemod plugin at runtime so `PhoneService` is available when your plugin's `setup()` runs.

## Step 2: Register your app during plugin setup

```java
public class MyPluginSetup {
    public void setup() {
        PhoneService.initialize();
        PhoneService.get().registerApp(new MyCustomPhoneApp());
    }
}
```

`PhoneAppRegistry.register()` throws `IllegalArgumentException` if the same app ID is registered twice. This prevents silent overwrites from misconfigured plugin load order.

## Step 3: Implement the app contract

Extend `StatefulPhoneApp` and use `PhoneEventActions` constants to avoid magic strings:

```java
public final class MyCustomPhoneApp extends StatefulPhoneApp<MyCustomPhoneApp.State> {

    public enum State { MAIN, DETAILS }

    public MyCustomPhoneApp() {
        super(State.MAIN);
    }

    @Override public String getId()           { return "myapp"; }
    @Override public String getDisplayName()  { return "My App"; }
    @Override public String getAppButtonUI()  { return "Pages/Phone/Components/MyAppButton.ui"; }
    @Override public String getUIPath()       { return "Pages/Phone/MyApp.ui"; }

    @Override
    public void build(PhoneAppContext ctx, UICommandBuilder cmd, UIEventBuilder evb) {
        appendMainUI(ctx, cmd);
        boolean details = getState(ctx) == State.DETAILS;
        PhoneUi.setVisible(cmd, "#MainView", !details);
        PhoneUi.setVisible(cmd, "#DetailsView", details);
        PhoneUi.bindAction(evb, "#OpenDetailsButton", "open_details");
        PhoneUi.bindBack(evb, "#BackButton");
    }

    @Override
    public boolean handleEvent(PhoneAppContext ctx, PhoneEvent event,
                               UICommandBuilder cmd, UIEventBuilder evb) {
        switch (event.getAction()) {
            case "open_details" -> {
                setState(ctx, State.DETAILS);
                build(ctx, cmd, evb);
                return true;
            }
            case PhoneEventActions.BACK -> {
                setState(ctx, State.MAIN);
                build(ctx, cmd, evb);
                return true;
            }
        }
        return false;
    }
}
```

### Key points

- Always call `appendMainUI(ctx, cmd)` at the top of `build()`. It clears the shell content area (`ctx.getContentSelector()`) and loads `getUIPath()`.
- Keep app views inside that root UI and toggle app-owned selectors. Do not
  append additional views directly into `ctx.getContentSelector()` after
  `appendMainUI()`.
- Return `true` from `handleEvent` if you handled the event (the framework sends your builders as the update). Return `false` if you did not (the framework sends a no-op).
- After mutating state in `handleEvent`, call `build()` with the same builders to produce the updated UI in the same response.

## Step 4: Add UI assets in your asset pack

Follow the same path conventions as the built-in apps:

- App root UI: `Pages/Phone/MyApp.ui`
- Component fragments: `Pages/Phone/Components/MyComponent.ui`
- App button: `Pages/Phone/Components/MyAppButton.ui` — declares `#APPBUTTONENTRY` with the icon hardcoded in `#APPBUTTON`'s `Default` style
- App icon PNG: `Pages/Phone/MyApp.png` — referenced from the button file using a relative path (e.g. `"../MyApp.png"`)

The button `.ui` file structure:

```
Group #APPBUTTONENTRY {
  LayoutMode: Top;
  FlexWeight: 1;
  Padding: (Horizontal: 3, Vertical: 3);
  Anchor: (Width: 84, Height: 100);
  TextButton #APPBUTTON {
    Anchor: (Width: 64, Height: 64);
    Style: (
      Default: (Background: "../MyApp.png"),
      Hovered:  (Background: #808080),
      Pressed:  (Background: #7a7a7a)
    );
  }
  Label #APPNAME {
    Anchor: (Top: 8, Width: 84, Height: 16);
    Text: "App";
    Style: (FontSize: 12, Alignment: Center, RenderBold: true);
  }
}
```

See [assets.md](assets.md) for full conventions and `PhoneAssetPaths` constants.

## Step 5: Bind UI events with `EventData`

```java
// Simple button press
evb.addEventBinding(
    CustomUIEventBindingType.Activating,
    "#MyButton",
    EventData.of("Action", "open_details"),
    false);

// Capture a text input value (@ prefix reads the element's .Value property)
evb.addEventBinding(
    CustomUIEventBindingType.Activating,
    "#SendButton",
    EventData.of("Action", PhoneEventActions.SEND_MESSAGE)
        .append("@MessageValue", "#MessageInput.Value"),
    false);

// Pass a static data value alongside the action
evb.addEventBinding(
    CustomUIEventBindingType.Activating,
    "#ChatButton",
    EventData.of("Action", PhoneEventActions.OPEN_CHAT)
        .append("Contact", somePhoneNumber),
    false);
```

The `@` prefix on a key tells the codec to read a dynamic UI element value at event fire time. Without `@`, the value is a static string baked in at binding time.

## Step 6: Use `PhoneAppContext` for per-player state and runtime access

```java
// ECS access
ctx.getRef()         // Ref<EntityStore>
ctx.getStore()       // Store<EntityStore>
ctx.getPlayerRef()   // PlayerRef — for packet/notification sending
ctx.getPhoneNumber() // String — the player's phone number
ctx.getAppId()       // String — this app's registered ID

// Per-player key-value state (String only, isolated per app)
ctx.getState("key")                  // @Nullable String
ctx.getState("key", "defaultValue")  // @Nonnull String
ctx.setState("key", "value")
ctx.clearState("key")
ctx.clearAllState()

// Convenience
ctx.sendNotification("Title", "Body text");
```

### Typed state access patterns

All state values are `String`. Convert for typed access:

```java
// Boolean
boolean expanded = Boolean.parseBoolean(ctx.getState("expanded", "false"));
ctx.setState("expanded", String.valueOf(true));

// Integer
int page = Integer.parseInt(ctx.getState("page", "0"));
ctx.setState("page", String.valueOf(page + 1));

// Enum (raw, without StatefulPhoneApp)
MyState s = MyState.valueOf(ctx.getState("view", MyState.LIST.name()));
ctx.setState("view", MyState.DETAIL.name());
```

When you use `StatefulPhoneApp`, the `__state__` key is managed automatically by `getState(ctx)` and `setState(ctx, s)`. Do not write to `__state__` manually.

## Step 6b: Async data and clickable item grids

> **This section is the normative async + threading guidance for apps.** Other pages link
> here rather than restating it. Note that the pattern needs `ctx.getPhoneNumber()`, so it
> is available only to v1 `PhoneApp`s — see
> [V2_APP_TUTORIAL.md](V2_APP_TUTORIAL.md#v2-apps-are-synchronous-today) for why a v2
> `DeviceApp` cannot do this yet.

### The three thread rules

| What you touch | Which thread | Why |
|---|---|---|
| The ECS `Store` / `Ref` (`ctx.getStore()`, `ctx.getState`, `ctx.setState`, components) | **World thread only** | The Store hard-throws off-thread. Hop with `world.execute(...)`, or in v2 use `DeviceAppContext.executeOnWorld`. |
| `AppService.refreshApp(appId, phoneNumber)` | **Any thread** | It hops internally: `RuntimeAppService` resolves the online phone and calls `entry.world().execute(...)` (`src/main/java/esq/phonemod/platform/RuntimeAppService.java:93-99`, contract at `AppService.java:20-22`). |
| A plain in-memory cache (`ConcurrentHashMap`) | **Any thread** | It is your own object, not engine state. |

So an async completion callback may write to your cache and call `refreshApp` directly —
**no manual world hop is required**, and adding one buys nothing. What it must *not* do is
touch the `Store` (including `ctx.getState` / `ctx.setState`) off-thread.

### Pattern: capture → fetch → cache → refresh

`build()` runs on the world thread and must render synchronously — **never block it** on a
service call. Keep results in an in-memory per-player cache, render from the cache, and
repaint when new data arrives:

```java
// capture stable ids while on the world thread — the callback has no ctx
String appId = ctx.getAppId();
String phone = ctx.getPhoneNumber();
myService.fetch(query).whenComplete((result, err) -> {
    // off-thread. A plain map write is safe here; ctx/Store access would NOT be.
    cache.put(phone, err != null ? List.of() : result);
    DevicePlatform.require().apps().refreshApp(appId, phone);   // any thread; framework repaints
});
// build() then renders cache.get(phone) synchronously
```

Guard against duplicate in-flight calls (a `loading` flag per player), and remember
`refreshApp` is best-effort: if the phone went offline or the player switched apps, the
repaint is a no-op and your cache simply serves the next `build()`.

To make an `ItemGrid` clickable, bind the slots and read the clicked index:

```java
PhoneUi.bindSlot(evb, "#MyGrid", "select_item");     // in build()
// in handleEvent():
if ("select_item".equals(event.getAction())) {
    int slot = event.slotIndex();                    // -1 if not a slot click
    ...
}
```

See [V2_API.md](V2_API.md) (`refreshApp`), [events.md](events.md) (slot clicks) and
[THREADING.md](THREADING.md). Prefer `refreshApp` over a sync channel used only as a
refresh signal — sync channels are for genuinely shared multi-viewer state ([SYNC.md](SYNC.md)).

## Step 7: Handle incoming message/call callbacks if needed

There are two overloads for incoming messages. The 4-arg version is tried first:

```java
@Override
public boolean onIncomingMessage(PhoneAppContext ctx, String fromNumber,
                                 UICommandBuilder cmd, UIEventBuilder evb) {
    // Preferred path — return true to send cmd/evb as the update with NO full rebuild.
    // Use this to append a single UI element (e.g. a message bubble) rather than
    // clearing and re-rendering the entire view.
    // Return false to fall through to the 2-arg version + automatic build().
    String currentContact = ctx.getState("currentContact");
    if (getState(ctx) == State.CHAT && fromNumber.equals(currentContact)) {
        // append the new bubble into #MessageList and return true
        cmd.append("#MessageList", "Pages/Phone/Components/MyBubble.ui");
        // ... populate the bubble ...
        return true;
    }
    return false; // not currently viewing this conversation — fall back to rebuild
}

@Override
public void onIncomingMessage(PhoneAppContext ctx, String fromNumber) {
    // Fallback — only called when the 4-arg version returns false.
    // build() is called automatically after this returns.
    // Update state here if needed before the rebuild.
}
```

```java
@Override
public void onIncomingCall(PhoneAppContext ctx, String callerNumber, String callerName) {
    // Called when an incoming call arrives while this app is open.
    // The framework shows the incoming-call overlay after this returns.
}
```

## Step 8: Optional lifecycle hooks

```java
@Override
public void onOpen(PhoneAppContext ctx, UICommandBuilder cmd, UIEventBuilder evb) {
    // Called once before build() when the app is opened.
    // cmd and evb are the same builders passed to build() — changes here are included.
}

@Override
public void onClose(PhoneAppContext ctx) {
    // Called when the player navigates away.
    // No builders — UI changes are not sent here.
}
```

## Controlling sort order

Override `getSortOrder()` to control sorted registry/catalog views and where an
installed non-system app appears in the home app grid. Lower values appear
first. System apps retain their device asset `DefaultApps` order. The default
is `0`; equal values are ordered by app ID.

```java
@Override
public int getSortOrder() {
    return 10;
}
```

## Full working example

```java
public final class NotesApp extends StatefulPhoneApp<NotesApp.State> {

    public enum State { LIST, EDIT }

    public NotesApp() { super(State.LIST); }

    @Override public String getId()           { return "notes"; }
    @Override public String getDisplayName()  { return "Notes"; }
    @Override public String getAppButtonUI()  { return "Pages/Phone/Components/NotesButton.ui"; }
    @Override public String getUIPath()       { return "Pages/Phone/Notes.ui"; }

    @Override
    public void build(PhoneAppContext ctx, UICommandBuilder cmd, UIEventBuilder evb) {
        appendMainUI(ctx, cmd);
        boolean editing = getState(ctx) == State.EDIT;
        PhoneUi.setVisible(cmd, "#NotesListView", !editing);
        PhoneUi.setVisible(cmd, "#NotesEditView", editing);
        PhoneUi.bindAction(evb, "#OpenNoteButton", "open_note");
        PhoneUi.bindBack(evb, "#BackButton");
    }

    @Override
    public boolean handleEvent(PhoneAppContext ctx, PhoneEvent event,
                               UICommandBuilder cmd, UIEventBuilder evb) {
        switch (event.getAction()) {
            case "open_note" -> { setState(ctx, State.EDIT);  build(ctx, cmd, evb); return true; }
            case PhoneEventActions.BACK -> { setState(ctx, State.LIST); build(ctx, cmd, evb); return true; }
        }
        return false;
    }
}
```
