# Phonemod v2 API

Depend on `esq.phonemod:phonemod-api:2.0.0-alpha.1` with `provided` scope and keep
`"esq.phonemod:Phonemod"` in the plugin manifest. The runtime plugin includes the API
classes in its single production JAR.

Every signature below is from `phonemod-api/src/main/java/`; line references are to the
file named in each heading. A compiling end-to-end use of this API is
[`examples/hello-device-app/`](../examples/hello-device-app).

> ## Three simple names mean two different types
>
> The API module and the runtime each define a class with these names. As a consumer you
> can only ever see the API one; the runtime one appears in this repo's own sources and in
> stack traces. Always package-qualify in discussion.
>
> | Simple name | API module (yours) | Runtime (internal) |
> |---|---|---|
> | `SyncService` | `esq.phonemod.api.SyncService` — 2-method stub, [not yet functional](#syncservice-reserved-not-functional) | `esq.phonemod.phone.sync.SyncService` — the real channel coordinator |
> | `DeviceService` | `esq.phonemod.api.DeviceService` — identity lookups | `esq.phonemod.device.core.DeviceService` — shell/session/page factory |
> | `DeviceOpenEvent` | `esq.phonemod.api.event.DeviceOpenEvent` — published, cancellable `Pre` | `esq.phonemod.device.events.DeviceOpenEvent` — internal `PhoneEvents` payload |
>
> The runtime itself writes the API ones fully qualified to avoid the clash — see
> `src/main/java/esq/phonemod/device/ui/DevicePage.java:105`.

## Entry point — `DevicePlatform`

`esq.phonemod.api.DevicePlatform` — the runtime locator for the single enabled platform.

```java
// DevicePlatform.java:11-17
@Nonnull AppService          apps();
@Nonnull DeviceService       devices();
@Nonnull PresenceService     presence();
@Nonnull MessagingService    messaging();
@Nonnull CallingService      calling();
@Nonnull NotificationService notifications();
@Nonnull SyncService         sync();

// :19-21
static Optional<DevicePlatform> find();      // empty when Phonemod is absent
static DevicePlatform           require();   // throws IllegalStateException instead
```

`install(DevicePlatform)` `:25` and `uninstall(DevicePlatform)` `:31` are **host lifecycle
hooks** — third-party plugins must not call them.

## `AppService`

```java
// AppService.java:11-34
@Nonnull AppRegistration registerApp(@Nonnull PluginBase owner, @Nonnull DeviceApp app);

@Deprecated(since = "2.0", forRemoval = false)          // :13 — supported through 2.x
@Nonnull AppRegistration registerLegacyApp(@Nonnull PluginBase owner,
        @Nonnull String canonicalId, @Nonnull Set<String> legacyAliases,
        @Nonnull PhoneApp<?> app);

@Nonnull Optional<DeviceApp>       find(@Nonnull String canonicalIdOrAlias);
@Nonnull Collection<AppDescriptor> snapshot();           // immutable
void refreshApp(@Nonnull String appId, @Nonnull String phoneNumber);
```

`refreshApp` requests a repaint of `appId` on that phone's currently-open page and is
**safe to call from any thread** — the framework hops to the player's world thread itself
(`AppService.java:20-22`; implementation
`src/main/java/esq/phonemod/platform/RuntimeAppService.java:93-99`). It is a no-op if the
phone is offline or a different app is open.

> `phoneNumber` is `PhoneAppContext.getPhoneNumber()` (`AppService.java:32`) — a **v1**
> value. A v2 `DeviceAppContext` has no phone-number accessor, so `refreshApp` currently has
> no supported v2 caller. See
> [V2_APP_TUTORIAL.md § v2 apps are synchronous today](V2_APP_TUTORIAL.md#v2-apps-are-synchronous-today).

### `AppRegistration`

```java
// AppRegistration.java:5-8 — extends AutoCloseable
@Nonnull String appId();
boolean         active();
void            close();
```

Closing is safe but usually unnecessary: the registration is bound to the owning plugin's
task registry and is closed on disable. Duplicate canonical IDs and aliases are rejected
atomically.

## `DeviceApp`

`esq.phonemod.api.DeviceApp` — the public v2 app contract.

```java
// DeviceApp.java:9-17
@Nonnull AppDescriptor descriptor();
void build(@Nonnull DeviceAppContext context, @Nonnull UICommandBuilder commands,
           @Nonnull UIEventBuilder events);
default void onOpen (@Nonnull DeviceAppContext context) {}
default void onClose(@Nonnull DeviceAppContext context) {}
@Nonnull default DeviceActionResult handle(@Nonnull DeviceAppContext context,
                                           @Nonnull DeviceAction action) {
    return DeviceActionResult.REJECTED;   // :16 — the default REJECTS
}
```

## `DeviceAppContext`

```java
// DeviceAppContext.java:9-17
@Nonnull PlayerRef        player();
@Nonnull DeviceIdentity   identity();
@Nonnull default Optional<String> address();   // :11 == identity().optionalAddress()
void     executeOnWorld(@Nonnull Consumer<WorldAccess> work);
@Nonnull Optional<String> state(@Nonnull String key);
void     state(@Nonnull String key, @Nonnull String value);
void     notify(@Nonnull String title, @Nonnull String body);
@Nonnull SyncService      sync();
```

`executeOnWorld` is the **only** legal route to the ECS from an app: it hops to the owning
world thread and re-checks `ref.isValid()` before invoking the callback
(`src/main/java/esq/phonemod/platform/RuntimeDeviceAppContext.java:29-35`).

`state` is scoped per device + app and bounded — keep durable domain data in your own
component.

### `WorldAccess`

```java
// WorldAccess.java:9 — valid only inside an executeOnWorld callback
public record WorldAccess(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {}
```

## `AppDescriptor`

```java
// AppDescriptor.java:10-21
public record AppDescriptor(
        @Nonnull  String      id,                    // must match [a-z0-9_.-]+:[a-z0-9_.-]+  (:22)
        @Nonnull  Set<String> legacyAliases,         // each [a-z0-9_.:-]{1,64}, may not contain id
        @Nonnull  String      localizationKey,       // required, non-blank
        @Nullable String      iconPath,
        @Nonnull  String      uiPath,                // required, non-blank
        @Nullable String      appButtonUiPath,
        @Nonnull  Set<String> capabilities,
        @Nonnull  Set<String> permissions,
        @Nonnull  Set<String> supportedDeviceTypes,  // empty == all device types
        boolean               system,
        int                   sortOrder) {

    public boolean supports(@Nonnull String deviceType);   // :40
}
```

The compact constructor (`:25-38`) throws `IllegalArgumentException` on a non-namespaced
id, a blank `localizationKey`/`uiPath`, or an invalid alias, and defensively copies every
set (a `null` set becomes `Set.of()`).

## `DeviceIdentity`

```java
// DeviceIdentity.java:9-18
public record DeviceIdentity(@Nonnull UUID id, @Nullable String address, boolean shared) {
    @Nonnull public Optional<String> optionalAddress();
}
```

`address` is optional and mutable over a device's life; a blank or >64-character address
throws. The `id` is the stable key.

## `DeviceAction` / `DeviceActionResult`

```java
// DeviceAction.java:9-10
public record DeviceAction(@Nonnull String action, @Nonnull String appId,
                           @Nonnull Map<String, String> parameters, long sequence) {}
```

Validated at construction (`:13-28`): `action` and `appId` must match
`[a-z0-9_.:-]{1,64}`; at most **32** parameters; keys 1–64 chars; values ≤ **1024** chars;
`sequence` ≥ 0. `parameters` is copied immutably. Client input is therefore *bounded*, not
*trusted* — still apply domain validation.

```java
// DeviceActionResult.java:3
public enum DeviceActionResult { HANDLED, RERENDER, REJECTED }
```

## `DeviceService`

`esq.phonemod.api.DeviceService` (not the runtime class of the same name).

```java
// DeviceService.java:14-21
@Nonnull Optional<DeviceIdentity>   find(@Nonnull UUID id);
@Nonnull Optional<DeviceIdentity>   findByAddress(@Nonnull String address);
@Nonnull Collection<DeviceIdentity> snapshot();
void openShared(@Nonnull PlayerRef player, @Nonnull Ref<EntityStore> ref,
                @Nonnull Store<EntityStore> store, @Nonnull String deviceAssetId,
                @Nonnull String stableKey);
@Nonnull Map<String, String> contacts(@Nonnull Ref<EntityStore> ref,
                @Nonnull Store<EntityStore> store, @Nonnull String address);
```

`openShared` and `contacts` take a `Ref` + `Store`, so they must be called **on the owning
world thread** (from inside `executeOnWorld`). `contacts` returns address → display name.

## Communication services

```java
// CallingService.java:6-7
boolean call(@Nonnull DeviceIdentity from, @Nonnull String toAddress);
void    hangUp(@Nonnull DeviceIdentity endpoint);

// MessagingService.java:6
boolean send(@Nonnull DeviceIdentity from, @Nonnull String toAddress, @Nonnull String body);

// NotificationService.java:6
void send(@Nonnull DeviceIdentity target, @Nonnull String title, @Nonnull String body);

// PresenceService.java:5
boolean online(@Nonnull DeviceIdentity identity);
```

`call` and `send` return `false` when the platform refused the operation (offline,
unreachable, policy) rather than throwing.

## `SyncService` (reserved, not functional)

```java
// SyncService.java:5-8
public interface SyncService {
    void publish(@Nonnull String channel, @Nonnull String payload);
    void subscribe(@Nonnull String channel);
}
```

**Do not build on this.** There is no callback or state accessor, so `subscribe` has
nowhere to deliver a payload. Shared multi-viewer state today uses `SyncChannelType` +
`LegacyRooms` on the v1 context — see [SYNC.md](SYNC.md).

## Published events — `esq.phonemod.api.event`

Six types, all `IEvent<Void>` on the **shared Hytale event bus**. Payloads identify
operations without exposing message bodies.

### Subscribing

Register through your plugin's own registry so listeners are removed on disable:

```java
// in your plugin's setup()
getEventRegistry().registerGlobal(esq.phonemod.api.event.DeviceOpenEvent.Post.class,
        event -> LOGGER.atInfo().log("opened by %s", event.playerId()));
```

The runtime fires them with
`HytaleServer.get().getEventBus().dispatchFor(X.class).dispatch(new X(...))`
(e.g. `src/main/java/esq/phonemod/device/ui/DevicePage.java:105-113`).

> **`Pre` is not a subtype of the outer event.** In every one of these types `Post extends
> <Outer>` but `Pre extends CancellableDeviceEvent implements IEvent<Void>` (e.g.
> `DeviceOpenEvent.java:12,17`). Subscribe to `X.Pre` or `X.Post` explicitly — subscribing
> to `X` will not receive `Pre`.

### Cancellation contract

```java
// CancellableDeviceEvent.java:5-8
abstract class CancellableDeviceEvent implements ICancellable {
    public final boolean isCancelled();
    public final void    setCancelled(boolean cancelled);
}
```

The class itself is **package-private**, so you cannot name it or write a listener typed to
it — but both methods are `public final` and are callable through each public `Pre`
subclass. (Verified: `examples/hello-device-app` calls `event.setCancelled(true)` on an
`AppOpenEvent.Pre` from another package and compiles.)

Cancelling a `Pre` aborts the operation — the runtime checks `pre.isCancelled()` and bails
(`DevicePage.java:390`). Only `Pre` events are cancellable.

### The events

| Event | Accessors | Fired |
|---|---|---|
| `DeviceOpenEvent.Pre` / `.Post` | `UUID playerId()`, `DeviceIdentity device()` — `DeviceOpenEvent.java:11,15` | a device page opens (`DevicePage.java:105,112`) |
| `AppOpenEvent.Pre` / `.Post` | `UUID playerId()`, `String address()`, `String appId()` — `AppOpenEvent.java:10,14` | an app opens (`DevicePage.java:388,417`) |
| `AppInstallationEvent.Pre` / `.Post` | `UUID playerId()`, `String address()`, `String appId()`, `boolean installing()` — `AppInstallationEvent.java:10,14` | install (`installing=true`) / uninstall (`false`) (`AppInstallService.java:80,88,112,123`) |
| `MessageEvent.Pre` / `.Post` | `String from()`, `String to()`, `int bodyLength()` — `MessageEvent.java:9,13` | a message is sent (`PhoneRegistry.java:198,212`) |
| `CallTransitionEvent.Pre` / `.Post` | `String caller()`, `String callee()`, `Transition transition()` — `CallTransitionEvent.java:10,14` | call state changes (`CallRegistry.java:178,193,219`) |
| `CancellableDeviceEvent` | `isCancelled()` / `setCancelled(boolean)` | package-private base of every `Pre` |

`CallTransitionEvent.Transition` is `RINGING`, `ANSWERED`, `ENDED`, `MISSED`
(`CallTransitionEvent.java:7`).

`MessageEvent` deliberately carries only `bodyLength()`, never the body.

> The `address()` accessor on `AppOpenEvent` / `AppInstallationEvent` receives whatever the
> runtime passes at the fire site, which is the device id (`session.getDeviceId()`,
> `DevicePage.java:389`). Treat it as a device key, not a guaranteed dialable address.

## Deprecated v1 surface

`PhoneApp`, `PhoneAppContext`, `PhoneUi`, `PhoneEvent`, `PhoneService` and the typed room
bridge (`LegacyRooms`) are all `@Deprecated(since = "2.0", forRemoval = false)` and remain
source/binary compatible **throughout 2.x**. New integrations should prefer `DeviceApp` —
except where a v2 gap forces v1 (async refresh, sync channels), as noted above.

Reference: [api-reference.md](api-reference.md) · [events.md](events.md) · [SYNC.md](SYNC.md)
