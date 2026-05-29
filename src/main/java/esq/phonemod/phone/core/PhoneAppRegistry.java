package esq.phonemod.phone.core;

import esq.phonemod.phone.api.PhoneApp;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
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
    public void register(@Nonnull PhoneApp<?> app) {
        String appId = app.getId();
        if (apps.containsKey(appId)) {
            throw new IllegalArgumentException("Phone app already registered: " + appId);
        }
        apps.put(appId, app);
    }

    @Nonnull
    public PhoneApp<?> get(@Nonnull String appId) {
        return apps.get(appId);
    }

    @Nonnull
    public List<PhoneApp<?>> getApps() {
        List<PhoneApp<?>> sorted = new ArrayList<>(apps.values());
        sorted.sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));
        return Collections.unmodifiableList(sorted);
    }

    public boolean contains(@Nonnull String appId) {
        return apps.containsKey(appId);
    }
}
