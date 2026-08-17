# Build or open a device

Device assets live under `Server/Phonemod/Devices`. A personal device has a stable UUID,
an optional human address, and an explicit owner. Item copying never transfers ownership;
use `/deviceframework transfer` for an audited transfer.

Shared terminals have an address-optional shared identity. From a block interaction call:

```java
DevicePlatform.require().devices().openShared(
    playerRef, playerRefEntity, store, "PC", stableWorldBlockKey);
```

The stable key deterministically resolves the same UUID after restart. The runtime dispatches
page opening to the player's world and validates all subsequent page actions against the
owning player and live page.

Declare supported device types and capabilities on every app/device. A device default app
must resolve by canonical ID or legacy alias and support the device type. Required device
assets are validated during startup; a broken shell, selector, theme, UI path, or default app
prevents partial startup.
