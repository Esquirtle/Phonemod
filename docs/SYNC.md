# Phonemod Realtime Sync (sync channels + rooms)

A small layer for **server-authoritative shared state that auto-updates every
viewer's page**. It generalizes what calls and messages did by hand, so group
chats, social feeds, presence, and multiplayer game lobbies are easy to build.

The model: **shared state + subscribers + automatic re-render on change.**

> ## Read this first: two different packages share one name
>
> `esq.phonemod.phone.sync` exists **twice**, and only one half is on your classpath.
>
> | | Module | On a consumer's classpath? |
> |---|---|---|
> | **API half** — `SyncChannelType`, `SyncChannelHandle`, `Viewer`, `MemberAccessor`, `ChannelPersistence`, `LegacyRooms` | `phonemod-api` (`phonemod-api/src/main/java/esq/phonemod/phone/sync/`) | **Yes** — this is what you write against |
> | **Runtime half** — `SyncService`, `SyncChannel`, `ChannelStore`, `Hashing`, `RuntimeLegacySyncBridge`, `room/RoomService`, `room/RoomState` | Phonemod runtime (`src/main/java/esq/phonemod/phone/sync/`) | **No** — host internals, described here only to explain behavior |
>
> Earlier revisions of this page showed `SyncService.registerType`, `RoomState` and
> `RoomService` in consumer examples. Those types ship **only** inside the Phonemod
> plugin jar, so that code cannot compile in a plugin that depends on
> `phonemod-api`. The consumer-facing entry point is
> [`LegacyRooms`](#the-api-surface-legacyrooms) — see
> `relay-ops/src/main/java/esq/relayops/setup/RelayOpsSetup.java:9,39` for a real
> consumer using it.

> **Sync channels vs. `AppService.refreshApp`.** Use sync channels for genuinely *shared*
> state that must broadcast to multiple viewers (group chats, feeds, presence, lobbies). For a
> single player's **async data** — e.g. a DB-backed app whose service returns
> `CompletionStage` — do **not** wire a channel as a mere refresh signal: cache the result in
> memory and call `platform.apps().refreshApp(appId, phoneNumber)` after the completion to
> repaint just that player's page (see
> [creating-apps.md](creating-apps.md#step-6b-async-data-and-clickable-item-grids)).

> **Sync channels are a v1 (`PhoneApp`) facility.** The only way to obtain a
> `SyncChannelHandle` is `PhoneAppContext.channel(type, id)`
> (`phonemod-api/src/main/java/esq/phonemod/phone/api/PhoneAppContext.java:177`), and
> `PhoneAppContext` is `@Deprecated(since = "2.0")` (`:37`). A v2 `DeviceApp` receives a
> `DeviceAppContext`, whose `sync()` returns the **different, currently non-functional**
> `esq.phonemod.api.SyncService` (see [below](#the-v2-esqphonemodapisyncservice-stub)). An app
> that needs channels today must register through `AppService.registerLegacyApp`.

```
LegacyRooms / handle.update(...)
  ↓ mutate state (per-channel lock)
  ↓ persist to JSON (content-hash gated)
  ↓ broadcast → for each subscriber: resolve live OnlineEntry (PhoneRegistry)
                 → world.execute(() -> page.refreshApp(appId))
                 → app.build() re-renders from the new shared state
```

## The API surface

All of these live in `phonemod-api/src/main/java/esq/phonemod/phone/sync/` and are the
**only** sync types a consumer can reference.

| Type | Role |
|------|------|
| `SyncChannelType<S>` | A channel *kind*: `typeId`, `BuilderCodec<S>`, state factory, id accessor, `ChannelPersistence`, optional `MemberAccessor`. `SyncChannelType.java:19` |
| `SyncChannelHandle<S>` | App-facing, viewer-scoped handle. From `ctx.channel(type, id)`. `SyncChannelHandle.java:21` |
| `Viewer` | `record Viewer(String phoneNumber, String appId)` — one subscribed view. `Viewer.java:15` |
| `MemberAccessor<S>` | Lets the room layer read/mutate the membership list inside your own state `S`. `MemberAccessor.java:11` |
| `ChannelPersistence` | `PERSISTENT` \| `EPHEMERAL`. `ChannelPersistence.java:17` |
| `LegacyRooms` | Static facade for type registration + membership. `LegacyRooms.java:7` |
| `LegacySyncBridge` | **Host SPI.** The runtime installs the implementation; consumers never call it. `LegacySyncBridge.java:8` |

### `SyncChannelType<S>`

Declare one per channel kind, normally as a `static final`.

```java
// SyncChannelType.java:47 / :57 / :67
static <S> SyncChannelType<S> persistent(String typeId, BuilderCodec<S> codec,
                                         Function<String, S> stateFactory,
                                         Function<S, String> idAccessor)
static <S> SyncChannelType<S> ephemeral (String typeId, BuilderCodec<S> codec,
                                         Function<String, S> stateFactory,
                                         Function<S, String> idAccessor)
SyncChannelType<S> withMembers(MemberAccessor<S> accessor)   // returns a copy
```

Accessors: `typeId()` `:72`, `codec()` `:77`, `newState(String id)` `:82`,
`idOf(S state)` `:87`, `persistence()` `:92`, `memberAccessor()` `:97` (`@Nullable`).

`persistent` survives restarts; `ephemeral` is removed on dispose and cleaned at startup.

### `SyncChannelHandle<S>`

Obtained from `PhoneAppContext.channel(type, id)` — it is scoped to `(phoneNumber, appId)`.

```java
S      state()                  // SyncChannelHandle.java:37 — current state, treat as read-only
void   update(Consumer<S> mut)  // :42 — mutate, persist, re-render every subscriber
void   subscribe()              // :47 — this view re-renders on every change
void   unsubscribe()            // :52
String id()                     // :57
```

### The API surface: `LegacyRooms`

`LegacyRooms` (`LegacyRooms.java:7`) is the consumer-visible replacement for the runtime's
`SyncService.registerType` and `RoomService`. It is `@Deprecated(since = "2.0",
forRemoval = false)` — deprecated, but supported throughout 2.x, and it is currently the
**only** supported way for a plugin to register a channel type.

```java
// LegacyRooms.java:9,12,15,19
static <S> void    registerType(SyncChannelType<S> type)
static <S> String  create(SyncChannelType<S> type, String host)              // -> new room id
static <S> boolean join  (SyncChannelType<S> type, String id, String member)
static <S> void    leave (SyncChannelType<S> type, String id, String member)
```

Every method delegates to `LegacySyncBridge.require()`, which throws
`IllegalStateException("Phonemod sync runtime is not enabled")` if the Phonemod plugin is
not loaded (`LegacySyncBridge.java:18-22`). Call `registerType` from your plugin's `setup()`.

> **Not exposed to consumers:** the runtime's `RoomService.list`, `RoomService.findOpen`
> and `RoomService.leaveAll`
> (`src/main/java/esq/phonemod/phone/sync/room/RoomService.java:81,95,113`)
> have **no** `LegacyRooms` equivalent. Matchmaking ("find an open lobby") must therefore be
> implemented in your own plugin — e.g. keep your own index of open room ids in a channel of
> its own. `RoomState` (`.../room/RoomState.java:23`) is likewise runtime-only, so a consumer
> supplies its own state type plus a `MemberAccessor` rather than reusing `RoomState`.

## Adding a synced app

Worked, compiling reference: `relay-ops` — `RelayRoomState.java` (state + codec + accessor)
and `RelayOpsSetup.java:39` (registration).

1. **Define state + codec.** A plain class with public fields and a `BuilderCodec`.
   Codec keys are PascalCase.
   ```java
   // relay-ops/src/main/java/esq/relayops/data/RelayRoomState.java:28-37
   public static final BuilderCodec<RelayRoomState> CODEC = BuilderCodec
           .builder(RelayRoomState.class, RelayRoomState::new)
           .append(new KeyedCodec<>("Id", Codec.STRING), (o, v) -> o.id = value(v), o -> o.id).add()
           .append(new KeyedCodec<>("Members", STRING_ARRAY_CODEC),
                   (o, v) -> o.members = v != null ? v : new String[0], o -> o.members).add()
           .build();
   ```
2. **Supply a `MemberAccessor`** if the channel is a room (required by
   `LegacyRooms.create/join/leave`). See `RelayRoomState.java:39-64`.
3. **Declare the channel type and register it in plugin `setup()`:**
   ```java
   // RelayRoomState.java:66-68
   public static final SyncChannelType<RelayRoomState> ROOMS = SyncChannelType
           .persistent("relayops-room", CODEC, RelayRoomState::new, state -> state.id)
           .withMembers(MEMBERS);

   // RelayOpsSetup.java:39 — in setup()
   LegacyRooms.registerType(RelayRoomState.ROOMS);
   ```
4. **Subscribe + render in `build()`** from shared state:
   ```java
   var ch = ctx.channel(RelayRoomState.ROOMS, roomId);
   ch.subscribe();
   render(ch.state(), cmd, evb);
   ```
5. **Publish in your handler** and let the broadcast re-render everyone:
   ```java
   ctx.channel(RelayRoomState.ROOMS, roomId).update(s -> s.apply(change));
   return true; // no manual build(): the broadcast repaints all subscribers
   ```

### Rooms & membership

```java
String roomId = LegacyRooms.create(RelayRoomState.ROOMS, ctx.getPhoneNumber());
LegacyRooms.join(RelayRoomState.ROOMS, roomId, ctx.getPhoneNumber());   // false if full
ctx.channel(RelayRoomState.ROOMS, roomId).subscribe();                  // see live changes
LegacyRooms.leave(RelayRoomState.ROOMS, roomId, ctx.getPhoneNumber());
```

An empty **ephemeral** room disposes itself and deletes its JSON on the last `leave`.

## Runtime behavior (host internals)

You cannot call into these, but they define the semantics you observe. All paths below are
under `src/main/java/esq/phonemod/phone/sync/`.

| Type | Role |
|------|------|
| `SyncService` | Registry + coordinator: `getOrCreate`, `publish`, `subscribe`, `loadAll`/`flushAll`, `broadcast`, `dispose`. `SyncService.java:30` |
| `SyncChannel<S>` | A live channel: shared state + `Viewer` subscribers; mutations serialized per channel. |
| `ChannelStore` | JSON persistence under `<dataDir>/channels/<typeId>/<sha256(id)>.json`. |
| `RuntimeLegacySyncBridge` | The `LegacySyncBridge` implementation installed at startup. |
| `room/RoomState` | Ready-made room state used by bundled apps. `RoomState.java:23` |
| `room/RoomService` | Membership + matchmaking: `createRoom` `:34`, `join` `:44`, `leave` `:62`, `list` `:81`, `findOpen` `:95`, `leaveAll` `:113`. |

### Rendering model

**Full re-render.** On any change, each viewer's currently-open app is rebuilt
from the latest shared state (`DevicePageHandle.refreshApp` → `app.build`). Apps therefore
just render `channel.state()` in `build()`; they never diff.

### Persistence

Channels are JSON files the framework owns: written on change (gated by a SHA-256
content hash so unchanged state isn't rewritten), reloaded on startup for
`PERSISTENT` types, and **deleted on dispose** for `EPHEMERAL` types (stale
ephemeral files are also cleared at startup). Serialization reuses the engine's own
config primitives (`BsonUtil.writeDocument` / `RawJsonReader.readSyncWithBak`), so a
channel's `BuilderCodec<S>` is the single source of its on-disk schema.

### Lifecycle

- `SetupManager.registerSyncTypes()` registers the built-in types; `initializeRuntime()`
  binds the store (`SyncService.init(plugin.getDataDirectory())`) and `loadAll()`;
  `shutdown()` calls `flushAll()`. Third-party types register via `LegacyRooms.registerType`.
- **Subscriptions are explicit** (`handle.subscribe()` in `build()`), with automatic
  cleanup: `DeviceAppCloseEvent` (app switch) and `DeviceCloseEvent` (page dismiss)
  drop the viewer; `PlayerDisconnectEvent` drops all of a player's subscriptions and
  leaves ephemeral rooms. Broadcasts to a closed phone are safely skipped.

### Threading

Per-channel `synchronized` mutation; concurrent channel/subscriber collections;
async JSON writes; each viewer's re-render is marshalled onto its own world thread
via `OnlineEntry.world().execute(...)` (players may be in different worlds). See
[THREADING.md](THREADING.md) and
[creating-apps.md](creating-apps.md#step-6b-async-data-and-clickable-item-grids) for the
normative thread rules.

## The v2 `esq.phonemod.api.SyncService` stub

`DeviceAppContext.sync()`
(`phonemod-api/src/main/java/esq/phonemod/api/DeviceAppContext.java:16`) and
`DevicePlatform.sync()` (`DevicePlatform.java:17`) return
`esq.phonemod.api.SyncService` — a **different type** from the runtime
`esq.phonemod.phone.sync.SyncService`. Its entire definition is:

```java
// phonemod-api/src/main/java/esq/phonemod/api/SyncService.java:5-8
public interface SyncService {
    void publish(@Nonnull String channel, @Nonnull String payload);
    void subscribe(@Nonnull String channel);
}
```

**Do not build on this yet.** There is no callback, listener, or state accessor, so a
subscriber has no way to receive anything — `subscribe` cannot deliver a payload anywhere.
Treat it as reserved API surface that is not yet functional. Shared state today goes through
`SyncChannelType` + `LegacyRooms` on the v1 context.

## Deliberate scope decisions

- **Full re-render** (no delta hook), **explicit subscribe** (no reactive
  re-declaration) — both can be layered on later without breaking apps.
- **No separate `RoomPolicy` type:** lobby-vs-group is expressed by the channel
  type's `ChannelPersistence` (`ephemeral` vs `persistent`); empty ephemeral
  rooms are disposed on the last leave.
- **Out of scope:** `CompletableFuture` async handlers, broadcast batching,
  delta rendering, and an invite/lobby *UI*.
