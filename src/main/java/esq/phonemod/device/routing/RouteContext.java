package esq.phonemod.device.routing;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.device.core.DeviceSession;
import esq.phonemod.device.core.DeviceShell;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

/**
 * Per-invocation, session-scoped execution context handed to guards, resolvers
 * and handlers for a single UI interaction.
 *
 * <p>This is the only mutable-in-spirit object in the routing pipeline, and it
 * is created fresh on every event — so a shared, immutable {@link Route}
 * registry stays thread-safe even with many phones open at once.
 */
public final class RouteContext {

    private final Ref<EntityStore> ref;
    private final Store<EntityStore> store;
    private final DeviceSession session;
    private final String action;
    private final Map<String, String> eventData;
    private final RouterHost host;

    public RouteContext(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull DeviceSession session,
            @Nonnull String action,
            @Nonnull Map<String, String> eventData,
            @Nonnull RouterHost host) {
        this.ref = ref;
        this.store = store;
        this.session = session;
        this.action = action;
        this.eventData = Collections.unmodifiableMap(eventData);
        this.host = host;
    }

    @Nonnull
    public Ref<EntityStore> getRef() {
        return ref;
    }

    @Nonnull
    public Store<EntityStore> getStore() {
        return store;
    }

    @Nonnull
    public DeviceSession getSession() {
        return session;
    }

    @Nonnull
    public String getAction() {
        return action;
    }

    /** The raw, normalized UI event payload (keys already stripped of the {@code @} prefix). */
    @Nonnull
    public Map<String, String> getEventData() {
        return eventData;
    }

    @Nonnull
    public RouterHost getHost() {
        return host;
    }

    // ── Convenience ─────────────────────────────────────────────────────────

    /**
     * Reads a payload value, tolerating the {@code @}-prefixed capture form for
     * parity with the legacy {@code DevicePage.param(...)} helper.
     */
    @Nullable
    public String param(@Nonnull String key) {
        String value = eventData.get(key);
        return value != null ? value : eventData.get("@" + key);
    }

    @Nullable
    public String currentAppId() {
        return session.getCurrentAppId();
    }

    @Nonnull
    public DeviceShell shell() {
        return session.getShell();
    }
}
