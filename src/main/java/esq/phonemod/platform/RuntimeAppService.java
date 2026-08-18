package esq.phonemod.platform;

import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.task.TaskRegistration;
import esq.phonemod.api.AppDescriptor;
import esq.phonemod.api.AppRegistration;
import esq.phonemod.api.AppService;
import esq.phonemod.api.DeviceApp;
import esq.phonemod.api.DeviceActionResult;
import esq.phonemod.phone.api.PhoneApp;
import esq.phonemod.phone.core.PhoneService;
import esq.phonemod.phone.messaging.PhoneRegistry;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import esq.phonemod.operations.OperationalMetrics;

/** Owner-bound, alias-aware v2 app registry with immutable snapshots. */
public final class RuntimeAppService implements AppService {
    private final Object lock = new Object();
    private final Map<String, Entry> byIdOrAlias = new LinkedHashMap<>();
    private volatile Map<String, Entry> snapshot = Map.of();

    @Override
    public AppRegistration registerApp(@Nonnull PluginBase owner, @Nonnull DeviceApp app) {
        AppDescriptor descriptor = app.descriptor();
        return register(owner, app, new DeviceAppAdapter(app), descriptor);
    }

    @Override
    @Deprecated(since = "2.0", forRemoval = false)
    public AppRegistration registerLegacyApp(@Nonnull PluginBase owner,
            @Nonnull String canonicalId, @Nonnull Set<String> legacyAliases,
            @Nonnull PhoneApp<?> app) {
        Set<String> aliases = new java.util.LinkedHashSet<>(legacyAliases);
        aliases.add(app.getId());
        AppDescriptor descriptor = new AppDescriptor(canonicalId, aliases,
                app.getDisplayName(), app.getIconPath(), app.getUIPath(), app.getAppButtonUI(),
                Set.of(), app.getRequiredPermission() == null ? Set.of() : Set.of(app.getRequiredPermission()),
                Set.of(app.getSupportedDeviceTypes()), app.isSystemApp(), app.getSortOrder());
        DeviceApp facade = new LegacyFacade(descriptor);
        Set<String> runtimeAliases = new java.util.LinkedHashSet<>(aliases);
        runtimeAliases.add(canonicalId);
        runtimeAliases.remove(app.getId());
        return register(owner, facade, app, descriptor, runtimeAliases);
    }

    private AppRegistration register(PluginBase owner, DeviceApp app, PhoneApp<?> runtimeApp,
                                     AppDescriptor descriptor) {
        return register(owner, app, runtimeApp, descriptor, descriptor.legacyAliases());
    }

    private AppRegistration register(PluginBase owner, DeviceApp app, PhoneApp<?> runtimeApp,
                                     AppDescriptor descriptor, Set<String> runtimeAliases) {
        RuntimeRegistration registration;
        synchronized (lock) {
            if (byIdOrAlias.containsKey(descriptor.id()))
                throw new IllegalArgumentException("Duplicate app id: " + descriptor.id());
            for (String alias : descriptor.legacyAliases()) {
                if (byIdOrAlias.containsKey(alias))
                    throw new IllegalArgumentException("Duplicate app alias: " + alias);
            }
            registration = new RuntimeRegistration(descriptor.id(), this);
            Entry entry = new Entry(app, registration);
            byIdOrAlias.put(descriptor.id(), entry);
            descriptor.legacyAliases().forEach(alias -> byIdOrAlias.put(alias, entry));
            snapshot = Map.copyOf(byIdOrAlias);
            PhoneService.get().registerApp(runtimeApp, runtimeAliases);
            OperationalMetrics.get().registration();
        }
        TaskRegistration base = new TaskRegistration(CompletableFuture.completedFuture(null));
        owner.getTaskRegistry().register(new TaskRegistration(
                base, registration::active, registration::close));
        return registration;
    }

    @Override public Optional<DeviceApp> find(String id) {
        Entry entry = snapshot.get(id);
        return entry == null ? Optional.empty() : Optional.of(entry.app());
    }

    @Override public Collection<AppDescriptor> snapshot() {
        return snapshot.values().stream().map(Entry::app).distinct()
                .map(DeviceApp::descriptor).toList();
    }

    @Override public void refreshApp(@Nonnull String appId, @Nonnull String phoneNumber) {
        if (appId == null || phoneNumber == null) return;
        // Same push path the sync layer uses (SyncService.broadcast): resolve the online
        // phone, hop to its world thread, and re-render the app if it is the one open.
        PhoneRegistry.OnlineEntry entry = PhoneRegistry.getOnlineEntry(phoneNumber);
        if (entry != null) {
            entry.world().execute(() -> entry.page().refreshApp(appId));
        }
    }

    private void unregister(String canonicalId) {
        synchronized (lock) {
            Entry entry = byIdOrAlias.get(canonicalId);
            if (entry == null) return;
            byIdOrAlias.values().removeIf(candidate -> candidate == entry);
            snapshot = Map.copyOf(byIdOrAlias);
            PhoneService.get().unregisterApp(canonicalId);
        }
    }

    private record Entry(DeviceApp app, RuntimeRegistration registration) {}

    private record LegacyFacade(AppDescriptor descriptor) implements DeviceApp {
        public void build(esq.phonemod.api.DeviceAppContext context,
                          com.hypixel.hytale.server.core.ui.builder.UICommandBuilder commands,
                          com.hypixel.hytale.server.core.ui.builder.UIEventBuilder events) {}
        public DeviceActionResult handle(esq.phonemod.api.DeviceAppContext context,
                                         esq.phonemod.api.DeviceAction action) {
            return DeviceActionResult.REJECTED;
        }
    }

    private static final class RuntimeRegistration implements AppRegistration {
        private final String id;
        private final RuntimeAppService owner;
        private final AtomicBoolean active = new AtomicBoolean(true);
        RuntimeRegistration(String id, RuntimeAppService owner) { this.id = id; this.owner = owner; }
        public String appId() { return id; }
        public boolean active() { return active.get(); }
        public void close() { if (active.compareAndSet(true, false)) owner.unregister(id); }
    }
}
