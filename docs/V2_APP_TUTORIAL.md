# Build a v2 app

1. Add the API artifact with `provided` scope and declare the runtime manifest dependency.
2. Implement `DeviceApp` with one immutable `AppDescriptor`. Use an ID such as
   `myplugin:tasks` and include the old simple ID in `legacyAliases` when migrating.
3. Put UI under `Common/UI/Custom/<your namespace>/...`; declare that path in the
   descriptor. Phonemod mounts the document before calling `build`.
4. Bind only named, lowercase actions. Client values arrive in a validated `DeviceAction`;
   still apply domain validation and authorization before mutation.
5. Keep per-device transient values in `DeviceAppContext.state`. It is bounded to 32 keys,
   64-character keys, and 1,024-character values. Keep durable domain data in your own
   versioned component/document.
6. Use `executeOnWorld` for ECS work, `notify` for user feedback, and the platform services
   for messaging/calls/sync.
7. Register in plugin `setup()`:

```java
DevicePlatform.require().apps().registerApp(this, new TasksApp());
```

Do not call framework `initialize()`. Cleanup follows the owning plugin automatically.
The repository's sibling `boilerplate-app` is the compilable canonical example.

## Async Data and Clickable Results

Many apps fetch data from an async service (one returning `java.util.concurrent.CompletionStage`).
The core rule: `build()` must **never** block on the service call. Render synchronously from an
in-memory per-player cache; repaint after the completion fires by calling
`AppService.refreshApp(appId, phoneNumber)`. This hops the re-render to the player's world thread
and is a no-op if the phone is offline or a different app is open.

### Pattern: Cache + Async fetch + refreshApp

```java
public final class SearchPhoneApp extends StatefulPhoneApp<SearchPhoneApp.State> {
    public enum State { LIST, DETAIL }

    private final Map<String, CachedResults> cache = new ConcurrentHashMap<>();
    private final SearchService searchService;  // returns CompletionStage<List<Result>>

    @Override
    public void build(PhoneAppContext ctx, UICommandBuilder cmd, UIEventBuilder evb) {
        appendMainUI(ctx, cmd);
        String phone = ctx.getPhoneNumber();
        CachedResults cached = cache.computeIfAbsent(phone, k -> new CachedResults());
        cached.appId = ctx.getAppId();  // capture for async refreshApp calls

        // If cache is empty and we haven't started loading, kick the fetch
        if (cached.results == null && !cached.loading) {
            kickSearch(ctx.getRef(), phone, cached, "default query");
        }

        if (getState(ctx) == State.LIST) {
            renderList(cached, cmd, evb);
        } else {
            renderDetail(cached, cmd, evb);
        }
    }

    private void renderList(CachedResults cached, UICommandBuilder cmd, UIEventBuilder evb) {
        PhoneUi.safeClear(cmd, "#SearchList");
        if (cached.results == null) {
            PhoneUi.setText(cmd, "#SearchStatus.Text",
                    cached.loading ? "Loading…" : "");
            return;
        }
        for (int i = 0; i < cached.results.size(); i++) {
            Result r = cached.results.get(i);
            String row = PhoneUi.appendListItem(cmd, "#SearchList", "Pages/Phone/Components/ResultRow.ui", i);
            PhoneUi.setText(cmd, PhoneUi.child(row, "#Name.Text"), r.getName());
            PhoneUi.setText(cmd, PhoneUi.child(row, "#Desc.Text"), r.getDescription());
            // Bind both button and grid item
            PhoneUi.bindAction(evb, PhoneUi.child(row, "#ViewButton"), "select_result",
                    PhoneUi.params("Index", String.valueOf(i)));
            PhoneUi.bindSlot(evb, PhoneUi.child(row, "#Icon"), "select_result",
                    PhoneUi.params("Index", String.valueOf(i)));
        }
    }

    private void renderDetail(CachedResults cached, UICommandBuilder cmd, UIEventBuilder evb) {
        Result r = cached.selectedResult;
        if (r == null) {
            PhoneUi.setText(cmd, "#DetailTitle.Text", "No selection");
            PhoneUi.bindAction(evb, "#DetailBack", PhoneEventActions.BACK);
            return;
        }
        PhoneUi.setText(cmd, "#DetailTitle.Text", r.getName());
        PhoneUi.setText(cmd, "#DetailBody.Text", r.getFullDescription());
        PhoneUi.bindAction(evb, "#DetailBack", PhoneEventActions.BACK);
    }

    @Override
    public boolean handleEvent(PhoneAppContext ctx, PhoneEvent event,
            UICommandBuilder cmd, UIEventBuilder evb) {
        String action = event.getAction();
        String phone = ctx.getPhoneNumber();
        CachedResults cached = cache.computeIfAbsent(phone, k -> new CachedResults());

        if ("select_result".equals(action)) {
            String indexStr = event.getParam("Index");
            if (indexStr != null && cached.results != null) {
                try {
                    int idx = Integer.parseInt(indexStr);
                    if (idx >= 0 && idx < cached.results.size()) {
                        cached.selectedResult = cached.results.get(idx);
                        setState(ctx, State.DETAIL);
                        build(ctx, cmd, evb);
                        return true;
                    }
                } catch (NumberFormatException ignored) { }
            }
            return false;
        }

        if (PhoneEventActions.BACK.equals(action)) {
            setState(ctx, State.LIST);
            cached.selectedResult = null;
            build(ctx, cmd, evb);
            return true;
        }

        return false;
    }

    private void kickSearch(Ref<EntityStore> ref, String phone, CachedResults cached, String query) {
        cached.loading = true;
        // Call the async service — this runs off the world thread
        searchService.search(query).whenComplete((results, err) ->
            onWorld(ref, () -> {
                // Back on the player's world thread: update cache and refresh
                cached.loading = false;
                cached.results = (err != null || results == null) ? List.of() : results;
                refresh(cached.appId, phone);
            })
        );
    }

    /** Marshal an off-thread completion back to the world thread. */
    private static void onWorld(Ref<EntityStore> ref, Runnable work) {
        if (ref == null) return;
        ref.getStore().getExternalData().getWorld().execute(() -> {
            if (ref.isValid()) work.run();
        });
    }

    /** Repaints this app if the player has it open. */
    private static void refresh(String appId, String phone) {
        DevicePlatform.require().apps().refreshApp(appId, phone);
    }

    private static final class CachedResults {
        List<Result> results;
        boolean loading;
        Result selectedResult;
        String appId;
    }
}
```

### Key points

**Capture on the world thread.** Store `appId = ctx.getAppId()` and `phone = ctx.getPhoneNumber()`
in your per-player cache before the async call, so they're available to the completion callback
off-thread.

**World-thread hop.** Completions fire on the service's thread pool. Use `onWorld(ref, work)` to
hop back to the player's world thread before mutating the cache and calling `refreshApp`. The
player's `World` is the serial execution context for ECS and UI — never `.join()` it.

**Clickable items.** Bind an `ItemGrid` with `PhoneUi.bindSlot(evb, gridSelector, action, params)`.
The client appends the clicked slot index as `SlotIndex`; read it in `handleEvent` via
`event.slotIndex()`. Use it to look up the item in your cached list. Slot clicks are mutations
only by your code's choice — they're fundamentally navigation (no interface lock).

### Gotchas

- **Never block on `CompletionStage.join()`** on the world thread, or the player's tick will stall.
- **`refreshApp` is best-effort:** if the phone went offline or the player switched to another app,
  the repaint is a no-op. Your cache remains intact for the next `build()`.
- **Unbounded service calls:** if the user clicks "search" repeatedly while loading, you'll queue
  multiple calls. Add a loading guard (e.g., `if (!cached.loading) kickSearch(...)`) to prevent
  duplicate calls, or debounce via a request queue.
- **Keep cached data in-memory only.** Component state slots (per `PhoneAppContext.getState()`)
  are bounded to 32 keys × 64 characters; domain objects belong in a `ConcurrentHashMap`.

### See also

- [V2_API.md](V2_API.md) — `AppService.refreshApp` contract
- [events.md](events.md) — `PhoneUi.bindSlot` and `PhoneEvent.slotIndex()`
- [creating-apps.md](creating-apps.md#step-6b-async-data-and-clickable-item-grids) — sync channel alternative
- [SYNC.md](SYNC.md) — when to prefer sync channels over `refreshApp` (multi-viewer shared state)
