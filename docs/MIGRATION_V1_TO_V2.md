# Migrate v1 data and integrations

Before first v2 use, copy the universe and rehearse `/deviceframework migration dry-run`.
The identity document is schema v2 and a timestamped `.pre-v2-*.bak` is written before the
first mutation of an existing registry.

On first legitimate use, legacy `PhoneNumber` metadata claims a UUID identity. The first
owner retains the address. A duplicate copied item receives a fresh UUID/address and that
player's installs, settings, contacts, histories, calls, and app session state are remapped
without overwriting an existing destination. Re-running component remaps is idempotent.

For source migration, replace the runtime JAR compile dependency with `phonemod-api`, remove
`PhoneService.initialize()`, select a canonical namespaced ID, retain the old ID as an alias,
and use owner-bound registration. Convert apps to `DeviceApp` and typed `DeviceAction` in
priority order; the owner-bound legacy registration bridge is available throughout 2.x.

Inspect conflicts with `/deviceframework migration status|conflicts`; use permission-gated
`repair`, `transfer`, or `release` only after reviewing the copied rehearsal. To roll back,
stop the server, restore the copied v1 universe and v1 plugin JARs, and do not mix v2-mutated
files into the restored copy.
