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
