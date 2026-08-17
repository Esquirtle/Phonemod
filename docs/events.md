# Events (legacy v1)

New integrations use the typed shared events described in [V2_API.md](V2_API.md).

Phonemod has two separate event models:

- `PhoneEvent`: normalized UI action data delivered to `PhoneApp.handleEvent()`.
- `DeviceEvent`: typed framework lifecycle and communication events distributed
  synchronously through `PhoneEvents`.

## PhoneEvent

`PhoneEvent` is an immutable payload created by `DevicePage` from the raw `PhoneEventData` codec object. It is passed to `PhoneApp.handleEvent()`.

```java
event.getAction()               // the action string (e.g. "open_chat")
event.getAppId()                // app field from event data (may be blank for generic actions)
event.getParams()               // unmodifiable Map<String, String>
event.getParam("key")           // @Nullable — always check for null
event.getParam("key", "default") // @Nonnull — safe overload
```

## Phone-level vs app-level actions

`DevicePage.handleDataEvent()` intercepts phone-level actions before they reach any app. App-level actions are forwarded to `currentApp.handleEvent()`.

### Phone-level actions (intercepted by DevicePage)

These actions normally use framework routes before app dispatch. If a route's
guard rejects the action, the router intentionally falls through to the current
app's `handleEvent()`. Apps generally should not handle them, except where that
fallback is part of the app's behavior, such as `open_chat` in Whatgram.

| Constant | Value | Description | Payload keys |
|----------|-------|-------------|--------------|
| `HOME` | `"home"` | Return to home screen | — |
| `OPEN_APP` | `"open_app"` | Open a registered app | `App` → app ID |
| `OPEN_CHAT` | `"open_chat"` | Open a Whatgram conversation (opens Whatgram if needed) | `Contact` → phone number |
| `ANSWER_CALL` | `"answer_call"` | Answer incoming call | — |
| `DECLINE_CALL` | `"decline_call"` | Decline incoming call | — |
| `HANG_UP` | `"hang_up"` | Hang up active call | — |

Note: `open_chat` has special routing — if Whatgram is not already active, `DevicePage` opens it first and then immediately forwards the event. You do not need to handle this in your app unless you are building a Whatgram replacement.

### App-level actions (forwarded to handleEvent)

| Constant | Value | Description | Payload keys |
|----------|-------|-------------|--------------|
| `BACK` | `"back"` | Navigate back within the app | — |
| `SEND_MESSAGE` | `"send_message"` | Send a message | `MessageValue` (captured from `#MessageInput.Value`) |
| `START_CALL` | `"start_call"` | Initiate a call | `Contact` or `DialNumber` |
| `OPEN_ADD_CONTACT` | `"open_add_contact"` | Navigate to add-contact form | — |
| `CONTACTS` | `"contacts"` | Navigate back to contacts list | — |
| `SAVE_CONTACT` | `"save_contact"` | Save a new contact | `ContactFormNumber`, `ContactFormName` |
| `REMOVE_CONTACT` | `"remove_contact"` | Remove a contact | `Contact` → phone number |

Custom apps can define additional action strings. Use a unique prefix to avoid collisions with other plugins (e.g. `"myplugin_open_detail"`).

## EventData bindings

`EventData` is used to configure what data is sent when a UI event fires.

### Static payload value

The value is baked in at binding time.

```java
evb.addEventBinding(
    CustomUIEventBindingType.Activating,
    "#MyButton",
    EventData.of("Action", "my_action"),
    false);

// With an additional static value
evb.addEventBinding(
    CustomUIEventBindingType.Activating,
    "#ChatButton",
    EventData.of("Action", PhoneEventActions.OPEN_CHAT)
             .append("Contact", somePhoneNumber),
    false);
```

### Dynamic payload value — `@` prefix

Prefix a key with `@` to read a UI element's value at the moment the event fires.

```java
// Reads #MessageInput.Value at event-fire time and sends it as MessageValue
evb.addEventBinding(
    CustomUIEventBindingType.Activating,
    "#SendButton",
    EventData.of("Action", PhoneEventActions.SEND_MESSAGE)
             .append("@MessageValue", "#MessageInput.Value"),
    false);

// Reads #DialInput.Value
evb.addEventBinding(
    CustomUIEventBindingType.Activating,
    "#CallButton",
    EventData.of("Action", PhoneEventActions.START_CALL)
             .append("@DialNumber", "#DialInput.Value"),
    false);

// Reads two form fields at once
evb.addEventBinding(
    CustomUIEventBindingType.Activating,
    "#SaveButton",
    EventData.of("Action", PhoneEventActions.SAVE_CONTACT)
             .append("@ContactFormNumber", "#NumberInput.Value")
             .append("@ContactFormName",   "#NameInput.Value"),
    false);
```

The `@` prefix is stripped from the key name before the event reaches `handleEvent`. `event.getParam("MessageValue")` retrieves the captured value; the key in `getParam` does not include `@`.

### Slot clicks — clickable `ItemGrid`

Bind a click on an `ItemGrid`'s slots with `PhoneUi.bindSlot`. The client appends the clicked
slot index automatically as `SlotIndex`; read it with `PhoneEvent.slotIndex()`. Slot events
reach `handleEvent` exactly like button actions (the phone event pipeline passes `SlotIndex`
and any custom params through), so a data app can map the index to its cached list. `bindSlot`
does not lock the interface (slot selection is navigation, not a mutation).

```java
// in build(): make an item grid clickable
PhoneUi.bindSlot(evb, "#MyGrid", "select_item");
// ...or with an extra literal param
PhoneUi.bindSlot(evb, "#MyGrid", "select_item", PhoneUi.params("Category", categoryId));

// in handleEvent():
if ("select_item".equals(event.getAction())) {
    int slot = event.slotIndex();               // -1 if the event carried no SlotIndex
    if (slot >= 0 && slot < items.size()) open(items.get(slot));
    return true;
}
```

## Event payload decoding

`DevicePage` decodes the raw UI event into a generic `Map<String, String>` and
builds a `PhoneEvent` from it. **`Action` and `App` are reserved**; every other
string field is preserved and reachable via `event.getParam(key)` /
`event.getParams()`. The `@` capture prefix is stripped during decode.

This means app-owned payload keys work without registering them anywhere:

```java
// binding
EventData.of("Action", "amazon_view").append("ProductId", product.id())
// handler
event.getParam("ProductId")   // → the value
```

Reserved / compatibility keys in use by the built-ins:

| Key | Access | Notes |
|---|---|---|
| `Action` | `getAction()` | reserved |
| `App` | `getAppId()` | reserved (for `open_app`) |
| `Contact`, `MessageValue`, `ContactFormNumber`, `ContactFormName`, `DialNumber` | `getParam(...)` | built-in app payloads |
| `State` | `getParam("State")` | integer state as string |

## Handling events

```java
@Override
public boolean handleEvent(PhoneAppContext ctx, PhoneEvent event,
                           UICommandBuilder cmd, UIEventBuilder evb) {
    String action = event.getAction();

    if ("open_detail".equals(action)) {
        String id = event.getParam("Contact");  // may be null — always check
        if (id == null || id.isBlank()) {
            build(ctx, cmd, evb);  // graceful fallback
            return true;
        }
        ctx.setState("selected_id", id);
        setState(ctx, State.DETAIL);
        build(ctx, cmd, evb);
        return true;
    }

    if (PhoneEventActions.BACK.equals(action)) {
        ctx.clearState("selected_id");
        setState(ctx, State.LIST);
        build(ctx, cmd, evb);
        return true;
    }

    return false;  // unhandled — framework sends a no-op update
}
```

## DeviceEvent Bus

Keep the listener instance used during setup and remove that specific listener
during shutdown:

```java
private final Consumer<PhoneMessageReceivedEvent> messageListener =
        this::onMessageReceived;

PhoneEvents.subscribe(PhoneMessageReceivedEvent.class, messageListener);
PhoneEvents.unsubscribe(PhoneMessageReceivedEvent.class, messageListener);
```

`PhoneEvents.post()` invokes listeners synchronously on the posting thread.
Listeners that mutate world or ECS state must dispatch to the appropriate world
thread. `unsubscribeAll(type)` removes every subscriber for that type and is
intended only for framework-wide shutdown.

Current emission contract:

| Event | Emitted when |
|-------|--------------|
| `DeviceOpenEvent` / `DeviceCloseEvent` | Device page opens or is dismissed |
| `DeviceAppOpenEvent` / `DeviceAppCloseEvent` | App opens or closes |
| `DeviceSettingsChangedEvent` | Settings app persists a change |
| `PhoneCallStartedEvent` | Call registry accepts an outgoing attempt |
| `PhoneCallAnsweredEvent` | Pending call connects; emitted once per participant |
| `PhoneCallEndedEvent` | Active call ends; emitted once per participant |
| `PhoneCallMissedEvent` | Pending call ends before answer |
| `PhoneMessageSentEvent` / `PhoneMessageReceivedEvent` | Registry accepts a message, before delivery or queueing |
