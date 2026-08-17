# Threading rules

- Player/entity component access and mutation runs on that player's world thread. For a
  target player in another world, dispatch to the target world—not the caller's world.
- UI build/update calls run on the owning world thread. A dismissed page is not presence;
  calls and mailboxes survive page dismissal.
- The Netty voice filter reads only an immutable route snapshot. It must not touch ECS,
  mutable call maps, or UI. Retain and deregister the exact packet-filter handle.
- Persistence snapshots are taken under the domain lock, then written with
  `Universe.get().getStorageManager().doSave`. Writes to one path are sequenced and saved
  hashes advance only after successful completion.
- Plugin registration and unload may race with reads. Registries publish immutable snapshots
  and registrations are tied to the guest plugin's task registry.

Violating thread affinity is a release blocker even when a local test appears to work.
