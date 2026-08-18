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
   for messaging/calls.
7. Register in plugin `setup()`:

```java
DevicePlatform.require().apps().registerApp(this, new TasksApp());
```

Do not call framework `initialize()`. Cleanup follows the owning plugin automatically.

> **Runnable example:** [`examples/hello-device-app/`](../examples/hello-device-app) is a
> complete standalone Maven project containing everything below — `pom.xml`, plugin class,
> `DeviceApp`, `.ui` documents, `.lang` file and manifest. It is verified to compile against
> `phonemod-api:2.0.0-alpha.1`. The sibling `boilerplate-app` project is a second working
> example.

## A complete v2 app

A `DeviceApp` object is a **singleton shared by every player** — it must hold no per-player
fields. Per-player values go in `DeviceAppContext.state`, which the framework scopes to the
device + app.

```java
public final class HelloDeviceApp implements DeviceApp {

    private static final String ACTION_GREET    = "hello_greet";
    private static final String ACTION_SET_NAME = "hello_set_name";
    private static final String PARAM_NAME      = "NameText";
    private static final String STATE_NAME      = "name";
    private static final String STATE_VIEW      = "view";

    // (id, legacyAliases, localizationKey, iconPath, uiPath, appButtonUiPath,
    //  capabilities, permissions, supportedDeviceTypes, system, sortOrder)
    private static final AppDescriptor DESCRIPTOR = new AppDescriptor(
            "example:hello", Set.of("hello"), "hellodevice.name", null,
            "Pages/Phone/HelloDeviceApp/Main.ui", "Pages/Phone/HelloDeviceApp/AppButton.ui",
            Set.of("apps"), Set.of(), Set.of("phone"), false, 50);

    @Override public @Nonnull AppDescriptor descriptor() { return DESCRIPTOR; }

    @Override
    public void build(@Nonnull DeviceAppContext context, @Nonnull UICommandBuilder commands,
                      @Nonnull UIEventBuilder events) {
        boolean detail = "detail".equals(context.state(STATE_VIEW).orElse("main"));
        commands.set("#MainView.Visible", !detail);
        commands.set("#DetailView.Visible", detail);
        commands.set("#TitleLabel.Text", Message.translation("hellodevice.title"));
        // address() is Optional — a shared device may have no assigned address.
        commands.set("#AddressLabel.Text", context.address().orElse("(no address)"));
        commands.set("#GreetingLabel.Text", context.state(STATE_NAME).orElse("(nobody yet)"));

        bind(events, "#GreetButton", EventData.of("Action", ACTION_GREET));
        // "@" marks a param whose value the client reads from a UI element.
        bind(events, "#SaveNameButton", EventData.of("Action", ACTION_SET_NAME)
                .append("@" + PARAM_NAME, "#NameField.Value"));
    }

    @Override
    public @Nonnull DeviceActionResult handle(@Nonnull DeviceAppContext context,
                                              @Nonnull DeviceAction action) {
        return switch (action.action()) {
            case ACTION_GREET -> {
                context.notify("Hello", "Hello, " + context.state(STATE_NAME).orElse("stranger") + "!");
                yield DeviceActionResult.HANDLED;
            }
            case ACTION_SET_NAME -> {
                // DeviceAction already bounds sizes; still validate the domain.
                String name = action.parameters().getOrDefault(PARAM_NAME, "").trim();
                if (name.isEmpty() || name.length() > 32) yield DeviceActionResult.REJECTED;
                context.state(STATE_NAME, name);
                context.state(STATE_VIEW, "detail");
                yield DeviceActionResult.RERENDER;   // framework re-runs build()
            }
            default -> DeviceActionResult.REJECTED;
        };
    }

    private static void bind(UIEventBuilder events, String selector, EventData data) {
        events.addEventBinding(CustomUIEventBindingType.Activating, selector, data, true);
    }
}
```

Return values from `handle` (`DeviceActionResult.java:3`):

| Result | Meaning |
|---|---|
| `HANDLED` | Consumed; no re-render |
| `RERENDER` | Consumed; the framework calls `build()` again |
| `REJECTED` | Not consumed (also the `DeviceApp.handle` default, `DeviceApp.java:14-17`) |

Every selector you address must exist in the `.ui` document named by
`AppDescriptor.uiPath` — the framework mounts that document before `build()` runs. The app
never clears or appends the shell content area itself.

## v2 apps are synchronous today

**`build()` must render from data already in hand.** A v2 `DeviceApp` has no supported way
to repaint itself after an asynchronous completion, so do not design one around a
`CompletionStage`.

The reason is a concrete gap, not a style preference:

- The repaint call is
  `AppService.refreshApp(String appId, String phoneNumber)`
  (`phonemod-api/src/main/java/esq/phonemod/api/AppService.java:34`). Its contract names the
  second argument as `PhoneAppContext.getPhoneNumber()` (`:32`), and the implementation
  resolves the live page with `PhoneRegistry.getOnlineEntry(phoneNumber)`
  (`src/main/java/esq/phonemod/platform/RuntimeAppService.java:93-99`).
- `DeviceAppContext` exposes **no phone number**. Its full surface is `player()`,
  `identity()`, `address()`, `executeOnWorld`, `state` (×2), `notify` and `sync()`
  (`phonemod-api/src/main/java/esq/phonemod/api/DeviceAppContext.java:8-17`).
- `address()` is *not* a substitute. It is `identity().optionalAddress()` (`:11`), so it is
  an `Optional` that is empty whenever the device identity carries a null address
  (`DeviceIdentity.java:9,18`), and the identity is resolved by lookup rather than being the
  raw registry key (`RuntimeDeviceAppContext.java:19-28`). Nothing guarantees it equals the
  key `refreshApp` needs.
- `DeviceAppContext.sync()` returns `esq.phonemod.api.SyncService`, which is a two-method
  stub with no callback and cannot deliver anything to a subscriber
  (`phonemod-api/src/main/java/esq/phonemod/api/SyncService.java:5-8`). It is **not** an
  async-refresh mechanism — see [SYNC.md](SYNC.md#the-v2-esqphonemodapisyncservice-stub).

**If you need async data or live shared state today, register a v1 app** via
`AppService.registerLegacyApp(owner, canonicalId, legacyAliases, PhoneApp<?>)`
(`AppService.java:14-16`), which is supported throughout 2.x. The v1 `PhoneAppContext` does
expose `getPhoneNumber()` (`PhoneAppContext.java:81`), which is what both the async-refresh
pattern and sync channels require.

- Async cache + `refreshApp` pattern (normative):
  [creating-apps.md § Step 6b](creating-apps.md#step-6b-async-data-and-clickable-item-grids)
- Multi-viewer shared state: [SYNC.md](SYNC.md)

## Threading

`build()` and `handle()` are called on the player's world thread. The normative rules live
in [creating-apps.md § Step 6b](creating-apps.md#step-6b-async-data-and-clickable-item-grids);
the v2-specific points are:

- **ECS access** — never touch the `Store` directly from a context callback. Use
  `context.executeOnWorld(world -> ...)`, which hops to the owning world thread and
  re-checks `ref.isValid()` before handing you the `Ref` + `Store`
  (`RuntimeDeviceAppContext.java:29-35`).
- **Never block** `build()`/`handle()` on I/O or `CompletionStage.join()` — the player's
  tick stalls.

## See also

- [V2_API.md](V2_API.md) — full v2 signature reference and the published event package
- [examples/hello-device-app/](../examples/hello-device-app) — the compiling version of this page
- [MIGRATION_V1_TO_V2.md](MIGRATION_V1_TO_V2.md) — moving an existing `PhoneApp` across
- [V2_DEVICE_TUTORIAL.md](V2_DEVICE_TUTORIAL.md) — personal and shared device integration
