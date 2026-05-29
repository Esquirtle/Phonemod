# Events

## PhoneEvent

`PhoneEvent` is an immutable payload created by `PhonePage` from the raw `PhoneEventData` codec object. It is passed to `PhoneApp.handleEvent()`.

```java
event.getAction()               // the action string (e.g. "open_chat")
event.getAppId()                // app field from event data (may be blank for generic actions)
event.getParams()               // unmodifiable Map<String, String>
event.getParam("key")           // @Nullable — always check for null
event.getParam("key", "default") // @Nonnull — safe overload
```

## Phone-level vs app-level actions

`PhonePage.handleDataEvent()` intercepts phone-level actions before they reach any app. App-level actions are forwarded to `currentApp.handleEvent()`.

### Phone-level actions (intercepted by PhonePage)

These are never passed to `handleEvent`. Do not handle them in your app.

| Constant | Value | Description | Payload keys |
|----------|-------|-------------|--------------|
| `HOME` | `"home"` | Return to home screen | — |
| `OPEN_APP` | `"open_app"` | Open a registered app | `App` → app ID |
| `OPEN_CHAT` | `"open_chat"` | Open a Whatgram conversation (opens Whatgram if needed) | `Contact` → phone number |
| `ANSWER_CALL` | `"answer_call"` | Answer incoming call | — |
| `DECLINE_CALL` | `"decline_call"` | Decline incoming call | — |
| `HANG_UP` | `"hang_up"` | Hang up active call | — |

Note: `open_chat` has special routing — if Whatgram is not already active, `PhonePage` opens it first and then immediately forwards the event. You do not need to handle this in your app unless you are building a Whatgram replacement.

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

## PhoneEventData codec fields

The following fields are decoded from the raw UI event by `PhonePage.PhoneEventData.CODEC`. They map to `PhoneEvent` params as shown:

| Codec key | PhoneEvent param key | Description |
|-----------|----------------------|-------------|
| `Action` | `getAction()` | Action name |
| `App` | `getAppId()` | App ID for `open_app` |
| `Contact` | `getParam("Contact")` | Phone number |
| `@MessageValue` | `getParam("MessageValue")` | Captured text input |
| `@ContactFormNumber` | `getParam("ContactFormNumber")` | Captured form input |
| `@ContactFormName` | `getParam("ContactFormName")` | Captured form input |
| `@DialNumber` | `getParam("DialNumber")` | Captured dial input |
| `State` | `getParam("State")` | Integer state (as string) |

To pass a value not in this table from your app's UI events, you must use one of the existing codec fields. Custom payload fields outside this set are not decoded by the current codec.

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
