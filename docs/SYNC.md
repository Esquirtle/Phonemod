# Phonemod Realtime Sync (`SyncChannels` + Rooms)

A small layer for **server-authoritative shared state that auto-updates every
viewer's page**. It generalizes what calls and messages did by hand, so group
chats, social feeds, presence, and multiplayer game lobbies are easy to build.

The model: **shared state + subscribers + automatic re-render on change.**

> **Sync channels vs. `AppService.refreshApp`.** Use sync channels for genuinely *shared*
> state that must broadcast to multiple viewers (group chats, feeds, presence, lobbies). For a
> single player's **async data** — e.g. a DB-backed app whose service returns
> `CompletionStage` — do **not** wire a channel as a mere refresh signal: cache the result in
> memory and call `platform.apps().refreshApp(appId, phoneNumber)` after the completion to
> repaint just that player's page (see `V2_API.md`).

```
publish(type, id, mutator)
  ↓ mutate state (per-channel lock)
  ↓ persist to JSON (content-hash gated)
  ↓ broadcast → for each subscriber: resolve live OnlineEntry (PhoneRegistry)
                 → world.execute(() -> page.refreshApp(appId))
                 → app.build() re-renders from the new shared state
```

Package: `esq.phonemod.phone.sync` (+ `sync/room`).

## Core types

| Type | Role |
|------|------|
| `SyncChannelType<S>` | A channel *kind*: `typeId`, `BuilderCodec<S>`, state factory, id accessor, `ChannelPersistence`, optional `MemberAccessor`. Register once with `SyncService.registerType`. |
| `SyncChannel<S>` | A live channel: shared `state` + `Viewer` subscribers. Mutations serialized per channel. |
| `SyncChannelHandle<S>` | App-facing, viewer-scoped: `state()`, `update(Consumer<S>)`, `subscribe()`, `unsubscribe()`. From `ctx.channel(type, id)`. |
| `SyncService` | Registry + coordinator: create/get/dispose, `publish`, `subscribe`, `loadAll`/`flushAll`, `broadcast`. |
| `ChannelStore` | JSON persistence under `<dataDir>/channels/<typeId>/<sha256(id)>.json`. |
| `Viewer` | `(phoneNumber, appId)` — one subscribed view. |
| `RoomState` / `RoomService` | Ready-made membership + matchmaking on top of the above. |

## Rendering model

**Full re-render.** On any change, each viewer's currently-open app is rebuilt
from the latest shared state (`DevicePageHandle.refreshApp` → `app.build`). This is
the same path the phone already used for incoming messages. Apps therefore just
render `channel.state()` in `build()`; they never diff. (A delta hook and per-tick
broadcast batching are natural future optimizations for very large/busy views.)

## Persistence

Channels are JSON files the framework owns: written on change (gated by a SHA-256
content hash so unchanged state isn't rewritten), reloaded on startup for
`PERSISTENT` types, and **deleted on dispose** for `EPHEMERAL` types (stale
ephemeral files are also cleared at startup). Serialization reuses the engine's own
config primitives (`BsonUtil.writeDocument` / `RawJsonReader.readSyncWithBak`), so a
channel's `BuilderCodec<S>` is the single source of its on-disk schema.

## Lifecycle

- `SetupManager.registerSyncTypes()` registers types (setup); `initializeRuntime()`
  binds the store (`SyncService.init(plugin.getDataDirectory())`) and `loadAll()`;
  `shutdown()` calls `flushAll()`.
- **Subscriptions are explicit** (`handle.subscribe()` in `build()`), with automatic
  cleanup: `DeviceAppCloseEvent` (app switch) and `DeviceCloseEvent` (page dismiss)
  drop the viewer; `PlayerDisconnectEvent` drops all of a player's subscriptions and
  leaves ephemeral rooms. Broadcasts to a closed phone are safely skipped.

## Threading

Per-channel `synchronized` mutation; concurrent channel/subscriber collections;
async JSON writes; each viewer's re-render is marshalled onto its own world thread
via `OnlineEntry.world().execute(...)` (players may be in different worlds).

## Adding a synced app

1. **Define state + codec** (`BuilderCodec<S>`; mirror the existing components).
2. **Declare a channel type** and register it in `SetupManager.registerSyncTypes()`:
   ```java
   public static final SyncChannelType<MyState> ROOM =
       SyncChannelType.persistent("myapp", MyState.CODEC, MyState::new, s -> s.id);
   // SetupManager: SyncService.registerType(MyApp.ROOM);
   ```
3. **Subscribe + render in `build()`** from shared state:
   ```java
   var ch = ctx.channel(ROOM, roomId);
   ch.subscribe();
   render(ch.state(), cmd, evb);
   ```
4. **Publish in your handler** and let the broadcast re-render everyone:
   ```java
   ctx.channel(ROOM, roomId).update(s -> s.apply(change));
   return true; // no manual build(): refreshApp repaints all subscribers
   ```

See `phone/apps/GroupChatApp.java` for a complete persistent example (one global
group chat). It composes its view from the Dust UI library
(`Phonemod-Assets/.../Pages/Phone/DustGroupChat.ui` + `Components/GroupChatButton.ui`,
reusing the Whatgram message bubble), exactly like `WhatgramApp`. Copy the
**subscribe/publish wiring**.

## Rooms & matchmaking (lobbies, groups)

A *room* is a channel whose type exposes a `MemberAccessor` (via
`SyncChannelType.withMembers`). `RoomState` provides a ready-made one:

```java
// A temporal game lobby (auto-disposed + JSON removed when the last member leaves):
static final SyncChannelType<RoomState> LOBBY = RoomState.ephemeralType("ttt-lobby");
// SetupManager: SyncService.registerType(LOBBY);

// Matchmaking from a handler: join an open lobby (cap 2) or create one.
String roomId = RoomService.findOpen(LOBBY, ctx.getPhoneNumber(), 2);
ctx.channel(LOBBY, roomId).subscribe();         // see live joins/moves

// Make a move / change room state:
ctx.channel(LOBBY, roomId).update(s -> s.status = "active");

// Leave (an empty ephemeral room disposes itself and deletes its JSON):
RoomService.leave(LOBBY, roomId, ctx.getPhoneNumber());
```

`RoomService`: `createRoom`, `join`, `leave`, `list`, `findOpen`, and `leaveAll`
(disconnect cleanup). For a game, store the board in a custom state type alongside
the member list and supply your own `MemberAccessor`.

## Deliberate scope decisions

- **Full re-render** (no delta hook), **explicit subscribe** (no reactive
  re-declaration) — both can be layered on later without breaking apps.
- **No separate `RoomPolicy` type:** lobby-vs-group is expressed by the channel
  type's `ChannelPersistence` (`ephemeralType` vs `persistentType`); empty ephemeral
  rooms are disposed in `RoomService.leave`.
- **Out of scope (v1):** `CompletableFuture` async handlers, broadcast batching,
  delta rendering, and an invite/lobby *UI* (the matchmaking API is present;
  invites can reuse `PhoneNotifications`).
- The bundled `GroupChatApp` renders inline and reuses an existing button asset to
  stay self-contained; it proves the API, not UI polish.
