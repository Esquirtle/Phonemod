# hello-device-app

A complete, compiling Phonemod **v2 `DeviceApp`** — the smallest thing that is still a real
plugin: descriptor, synchronous render, validated action handling, per-device state,
notification, ECS hop, and event subscription.

This is a **standalone Maven project**. It is deliberately *not* a module of the Phonemod
reactor (`../../pom.xml`), so it can never break the framework build.

## Build

The API artifact must be in your local repository first:

```bash
# from plugins-srcs/
mvn -f Phonemod/pom.xml install -DskipTests
mvn -f Phonemod/examples/hello-device-app/pom.xml package
```

Or with the project's Docker toolchain, from `plugins-srcs/`:

```bash
docker run --rm -u $(id -u):$(id -g) -e HOME=/mavenhome \
  -v ~/.m2:/mavenhome/.m2 -v "$PWD":/ws -w /ws \
  maven:3.9-eclipse-temurin-25 \
  mvn -B -f Phonemod/examples/hello-device-app/pom.xml package
```

Judge the result by the **exit code**. Maven prints a non-fatal
`'dependencies.dependency.systemPath' ... must specify an absolute path` line at `[ERROR]`
level — it comes from the installed `phonemod-api` pom and does not fail the build.

## Run

Copy `target/hello-device-app-1.0.0.jar` into the server's `Server/mods/` directory
alongside the Phonemod plugin jar. The manifest declares
`"esq.phonemod:Phonemod": ">=2.0.0-alpha.1"`, so the loader enables Phonemod first. Open a
phone in game; "Hello" appears on the home grid.

## Layout

| File | Purpose |
|---|---|
| `pom.xml` | Standalone build. `phonemod-api` is `provided`; the Hytale jar is `system`. |
| `src/main/java/.../HelloDeviceAppPlugin.java` | Registers the app and two event listeners in `setup()` |
| `src/main/java/.../HelloDeviceApp.java` | The `DeviceApp`: descriptor, `build`, `handle`, `onOpen` |
| `src/main/resources/manifest.json` | Plugin identity + hard dependency on Phonemod |
| `.../Pages/Phone/HelloDeviceApp/Main.ui` | Root document mounted by `AppDescriptor.uiPath` |
| `.../Pages/Phone/HelloDeviceApp/AppButton.ui` | Home-grid button (`#APPBUTTONENTRY` contract) |
| `.../Server/Languages/en-US/hellodevice.lang` | Translation keys |

## What it demonstrates

- An immutable `AppDescriptor` with a namespaced id and a legacy alias.
- **Synchronous rendering only.** A v2 `DeviceApp` has no working async-refresh path today
  — see [`docs/V2_APP_TUTORIAL.md`](../../docs/V2_APP_TUTORIAL.md#v2-apps-are-synchronous-today).
- Re-validating client input even though `DeviceAction` already bounds it.
- `DeviceActionResult.RERENDER` to let the framework re-run `build()`.
- `context.executeOnWorld(...)` as the only legal route to the ECS `Store`.
- Subscribing to `esq.phonemod.api.event` and cancelling a `Pre` event.

Full signature reference: [`docs/V2_API.md`](../../docs/V2_API.md).
