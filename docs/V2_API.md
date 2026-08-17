# Phonemod v2 API

Depend on `esq.phonemod:phonemod-api:2.0.0-alpha.1` with `provided` scope and keep
`"esq.phonemod:Phonemod"` in the plugin manifest. The runtime plugin includes the API
classes in its single production JAR.

`DevicePlatform.find()` is the optional locator; `DevicePlatform.require()` fails fast when
the runtime dependency is absent. It exposes app registration, identities/devices,
presence, messaging, calls, notifications, and sync.

Apps implement `DeviceApp`. `AppDescriptor` owns the canonical namespaced ID, v1 aliases,
localization/UI/icon paths, capabilities, permissions, device types, system flag, and sort
order. `DeviceAction` is an immutable validated client-event envelope. Handlers return
`HANDLED`, `RERENDER`, or `REJECTED`.

Register with `platform.apps().registerApp(plugin, app)`. The returned `AppRegistration`
is safe to close explicitly, but it is also attached to the owner plugin's task registry.
Duplicate canonical IDs and aliases are rejected atomically; snapshots are immutable.

The shared Hytale event bus publishes typed pre/post events for device opens, app installs
and opens, messages, and call transitions. Authorization-sensitive pre-events are
cancellable. Event payloads identify operations without exposing message bodies.

The deprecated `PhoneApp`, `PhoneAppContext`, `PhoneService`, `PhoneEvents`, and typed room
bridge remain source/binary compatibility surfaces throughout 2.x. New integrations should
not use them.
