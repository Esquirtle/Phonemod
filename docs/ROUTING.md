# Phonemod Routing

Phone UI interactions are dispatched by a small **declarative router** instead of
a procedural `if/else` chain. Every action (`home`, `open_app`, `open_chat`,
`answer_call`, …) is described by an immutable `Route` composed of
**guards → resolvers → handlers**, registered once and matched in O(1).

```
Action
  ↓  routes.get(action)          (O(1) lookup; else fallback route)
Guards     RouteGuard.canActivate     all must pass, else → fallback route
  ↓
Resolvers  RouteResolver.resolve      pure look-ups, merged into RouteData
  ↓
Handlers   RouteHandler.handle        first non-null RouteResult wins
  ↓
RouteResult → RouterHost.sendUpdate    the page applies the UI flush
```

Package: `esq.phonemod.device.routing` (`guards/`, `resolvers/`, `handlers/`).
The router is built in `DefaultPhoneRouter.createDefault()` and held as a shared,
immutable `static final` in `DevicePage`. `DevicePage` implements `RouterHost`,
exposing the privileged operations (open app, build context, flush UI) that
handlers need — keeping the engine's `protected sendUpdate(...)` encapsulated.

## Core types

| Type | Role |
|------|------|
| `Route` / `Route.Builder` | Immutable `action + guards + resolvers + handlers + metadata`. |
| `RouteGuard` | `boolean canActivate(ctx)` — precondition. Failure ⇒ fall through to fallback. |
| `RouteResolver` | `Map<String,Object> resolve(ctx)` — pure data pre-load, merged into `RouteData`. |
| `RouteHandler` | `RouteResult handle(ctx, data)` — business logic; returns the UI flush decision. |
| `RouteContext` | Per-interaction inputs: `ref`, `store`, `session`, `action`, `eventData`, `host`. |
| `RouteData` | Immutable bag of resolved values. |
| `RouteResult` | `update(cmd, evb)` or `empty()` (acknowledgement-only flush). |
| `RouterHost` | Page-side ops: `openApp`, `createContext`, `toPhoneEvent`, `loadHomeState`, `sendUpdate`. |
| `PhoneRouter` / `DefaultPhoneRouter` | `route(ctx)` + `registerRoute(route)`. |

## Adding a new route

Build a route and register it. **No existing routing code needs to change.**

```java
router.registerRoute(
    Route.builder("contacts")
        .guard(new ValidationGuard())
        .guard(new CurrentAppGuard())          // an app must be open
        .resolver(new AppResolver())           // resolves "app", "appId", "appDisplayName"
        .handler(new OpenContactsHandler())
        .metadata(RouteMetadata.of("contacts", "Open the contacts list"))
        .build());
```

A handler returns a `RouteResult`:

```java
public final class OpenContactsHandler implements RouteHandler {
    @Override
    public RouteResult handle(RouteContext ctx, RouteData data) {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder  evb = new UIEventBuilder();
        // ...build UI into cmd/evb, or mutate via ctx.getHost()/services...
        return RouteResult.update(cmd, evb);   // or RouteResult.empty()
    }
}
```

To register routes from another module, expose the router (e.g. via a getter)
and call `registerRoute` during setup, before any phone is opened — routes are
immutable and the registry is not meant to be mutated at runtime.

## Built-in routes

| Action | Guards | Resolvers | Handler |
|--------|--------|-----------|---------|
| `home` | `ValidationGuard` | — | `HomeHandler` |
| `open_app` | `ValidationGuard` | `AppResolver` | `OpenAppHandler` |
| `open_chat` | `ValidationGuard`, `CurrentAppIsNotGuard("whatgram")` | — | `OpenChatHandler` |
| `answer_call` | `AppCapabilityGuard("calls")` | — | `CallActionsHandler` |
| `decline_call` | `AppCapabilityGuard("calls")` | — | `CallActionsHandler` |
| `hang_up` | `AppCapabilityGuard("calls")` | — | `CallActionsHandler` |
| *(fallback)* | — | — | `AppEventHandler` (forwards to the current app) |

**Fall-through is intentional.** A matched route whose guard fails is delegated
to the fallback route — not rejected. This preserves the legacy behavior exactly:
e.g. a call action on a device without the `calls` capability flows to the current
app, and `open_chat` while Whatgram is already open lets Whatgram handle it.

## Recommended patterns

- **Guards** — small, pure predicates over `RouteContext`; no side effects.
  Compose several on one route (all must pass). Prefer a reusable guard
  (`AppCapabilityGuard`, `CurrentAppGuard`) over an inline check in a handler.
- **Resolvers** — pure, read-only look-ups only. Publish values under stable
  string keys (see the `public static final` key constants on each resolver) so
  handlers stay decoupled from services.
- **Handlers** — perform inherent side effects via `ctx.getHost()` or services,
  then **return** a `RouteResult`; never call `sendUpdate` directly. Return
  `RouteResult.empty()` for an acknowledgement-only flush.

## Testing routes (patterns)

Guards and resolvers are plain objects over `RouteContext`, so they test in
isolation by constructing a context with a stub `RouterHost` and a `DeviceSession`
in the desired state:

- **Guard test** — build a `RouteContext`, assert `guard.canActivate(ctx)` for
  the positive and negative cases (e.g. `AppCapabilityGuard("calls")` true/false
  by shell capability).
- **Resolver test** — call `resolver.resolve(ctx)` and assert the returned map
  keys/values (e.g. `CallStateResolver` populates `activePartner` when a call is
  active).
- **Handler test** — pass a fake `RouterHost` that records calls; assert the
  returned `RouteResult` shape (`isUpdate()`/`empty()`) and that the expected
  host operations were invoked.
- **Integration test** — drive `DefaultPhoneRouter.route(ctx)` with a recording
  `RouterHost` and assert the final `sendUpdate` decision per action, including
  guard fall-through to the fallback.

> The project has no test harness yet; add JUnit (and a mocking library for the
> Hytale runtime types) under `src/test/java` when introducing these tests.

## Deliberate scope decisions

- **Non-generic `Route`** (not `Route<T>`): no concrete payload type existed;
  `RouteMetadata` covers extensibility.
- **`route(RouteContext)`** rather than `route(action, eventData, ctx)`: the
  context already carries the action and payload.
- **`ValidationGuard` checks the action only**, not payload — the legacy flow
  treats missing payloads (blank `App`/`Contact`) as acknowledgement no-ops, not
  fall-throughs; handlers preserve that.
- **`CallStateGuard` is provided but not wired** to the call routes: the legacy
  code did not pre-check call state, so attaching it would change behavior.

**Out of scope** (poor fit for Hytale's synchronous, single-world-thread UI
model): `CompletableFuture` async handlers, `:param` path routing, HTTP-style
404/403/500 error routes, interceptors, and `debugRoutes()`.
