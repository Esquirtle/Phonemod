# State Management

## Why app instances are singletons

`PhoneService` holds exactly one instance of each registered app. That single object is shared across every player who opens the phone simultaneously. This means you must never store player-specific data as instance fields on your app class. Any instance field will bleed between players.

```java
// WRONG — instance field shared across all players
public final class MyApp extends StatefulPhoneApp<MyApp.State> {
    private String currentContact; // bleeds between players!
}

// CORRECT — per-player state stored in PhoneAppContext
public final class MyApp extends StatefulPhoneApp<MyApp.State> {
    private static final String KEY_CONTACT = "currentContact";

    private void setContact(PhoneAppContext ctx, String number) {
        ctx.setState(KEY_CONTACT, number);
    }

    private String getContact(PhoneAppContext ctx) {
        return ctx.getState(KEY_CONTACT);
    }
}
```

## PhoneAppSessionState — the storage layer

Per-player state is backed by `PhoneAppSessionState`, an ECS component on the player's entity. Its internal structure is:

```
Map<sessionKey, Map<key, value>>
```

where `sessionKey` is `phoneNumber + "|" + appId`. This means state is isolated per physical phone per app — if the same player has two phones open simultaneously, each phone's apps have completely independent state. `PhoneAppContext` derives and applies the session key automatically from its `phoneNumber` and `appId` fields; you never construct it yourself.

The component is registered with the ECS. Its data is session-only — it resets naturally when the player's entity is unloaded (logout or server restart). There is no persistence across sessions.

## Accessing state via PhoneAppContext

```java
// Read (returns null if not set)
@Nullable String value = ctx.getState("key");

// Read with default
@Nonnull String value = ctx.getState("key", "default");

// Write
ctx.setState("key", "value");

// Remove one key
ctx.clearState("key");

// Remove all keys for this app
ctx.clearAllState();
```

## Typed access patterns

All values are stored as `String`. Use standard Java conversions for other types.

### Boolean

```java
boolean flag = Boolean.parseBoolean(ctx.getState("flag", "false"));
ctx.setState("flag", String.valueOf(flag));
```

### Integer

```java
int page = Integer.parseInt(ctx.getState("page", "0"));
ctx.setState("page", String.valueOf(page + 1));
```

### Enum (without StatefulPhoneApp)

```java
MyState s = MyState.valueOf(ctx.getState("state", MyState.DEFAULT.name()));
ctx.setState("state", MyState.DETAIL.name());
```

## StatefulPhoneApp — enum state machine

`StatefulPhoneApp` wraps the raw string state into a type-safe enum accessor. It uses the reserved key `__state__` internally. Do not write to `__state__` manually.

```java
public final class MyApp extends StatefulPhoneApp<MyApp.State> {

    public enum State { LIST, DETAIL }

    public MyApp() {
        super(State.LIST);  // initial state returned when no state is set yet
    }

    @Override
    public void build(PhoneAppContext ctx, UICommandBuilder cmd, UIEventBuilder evb) {
        appendMainUI(cmd);
        switch (getState(ctx)) {
            case LIST   -> renderList(ctx, cmd, evb);
            case DETAIL -> renderDetail(ctx, cmd, evb);
        }
    }

    @Override
    public boolean handleEvent(PhoneAppContext ctx, PhoneEvent event,
                               UICommandBuilder cmd, UIEventBuilder evb) {
        return switch (event.getAction()) {
            case "open_detail" -> {
                setState(ctx, State.DETAIL);
                build(ctx, cmd, evb);
                yield true;
            }
            case "back" -> {
                setState(ctx, State.LIST);
                build(ctx, cmd, evb);
                yield true;
            }
            default -> false;
        };
    }
}
```

`getState(ctx)` falls back to `initialState` if the stored name is not a valid enum constant (defensive against enum renames).

## State lifetime

State is reset when the player's entity is unloaded. This happens on:
- Player logout
- Server restart
- Any other event that causes the entity to be removed from the store

There is no built-in mechanism to persist phone app state across sessions. If your app needs cross-session persistence, use a separate persistent data component (see the `hytale-persistent-data` skill).

## Key naming recommendations

- Use lowercase with underscores: `"current_contact"`, `"page_index"`
- Avoid `__state__` — reserved by `StatefulPhoneApp`
- Keep keys short and stable — they are stored as-is with no transformation
