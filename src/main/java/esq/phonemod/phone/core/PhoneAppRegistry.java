package esq.phonemod.phone.core;

import esq.phonemod.phone.api.PhoneApp;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for available phone apps.
 */
public final class PhoneAppRegistry {

    private final Map<String, PhoneApp<?>> apps = new LinkedHashMap<>();

    /**
     * Registers a phone app with this registry.
     *
     * @throws IllegalArgumentException if an app with the same ID is already registered
     */
    public synchronized void register(@Nonnull PhoneApp<?> app) {
        register(app, java.util.Set.of());
    }

    public synchronized void register(@Nonnull PhoneApp<?> app, @Nonnull java.util.Set<String> aliases) {
        String appId = app.getId();
        if (apps.containsKey(appId)) {
            throw new IllegalArgumentException("Phone app already registered: " + appId);
        }
        for (String alias : aliases) {
            if (apps.containsKey(alias)) throw new IllegalArgumentException("Phone app alias already registered: " + alias);
        }
        apps.put(appId, app);
        for (String alias : aliases) apps.put(alias, app);
    }

    /**
     * Reverses {@link #register}. No-op if no app with that id is registered. Host plugins call
     * this on shutdown so a disable→enable (/reload) does not leave stale app instances behind.
     */
    public synchronized void unregister(@Nonnull String appId) {
        PhoneApp<?> removed = apps.get(appId);
        if (removed != null) apps.values().removeIf(app -> app == removed);
    }

    @Nonnull
    public synchronized PhoneApp<?> get(@Nonnull String appId) {
        return apps.get(appId);
    }

    @Nonnull
    public synchronized List<PhoneApp<?>> getApps() {
        List<PhoneApp<?>> sorted = new ArrayList<>(new java.util.LinkedHashSet<>(apps.values()));
        sorted.sort(Comparator
                .comparingInt((PhoneApp<?> app) -> app.getSortOrder())
                .thenComparing(PhoneApp::getId));
        return Collections.unmodifiableList(sorted);
    }

    public synchronized boolean contains(@Nonnull String appId) {
        return apps.containsKey(appId);
    }
}
