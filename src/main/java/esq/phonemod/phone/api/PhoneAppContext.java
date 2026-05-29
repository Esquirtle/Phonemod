package esq.phonemod.phone.api;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import esq.phonemod.phone.components.PhoneAppSessionState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Context provided to phone apps for the current session.
 *
 * <p>Each app method receives a context scoped to the specific player and app.
 * Per-player state is stored in the {@link PhoneAppSessionState} component and
 * accessed via {@link #getState}/{@link #setState}. All state values are
 * {@code String}; use {@link Boolean#parseBoolean}, {@link Integer#parseInt},
 * or {@code Enum.valueOf} for typed reads.
 *
 * <p>Example:
 * <pre>{@code
 * boolean expanded = Boolean.parseBoolean(ctx.getState("expanded", "false"));
 * int page = Integer.parseInt(ctx.getState("page", "0"));
 * MyState s = MyState.valueOf(ctx.getState("__state__", MyState.DEFAULT.name()));
 * }</pre>
 */
public final class PhoneAppContext {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Ref<EntityStore> ref;
    private final Store<EntityStore> store;
    private final PlayerRef playerRef;
    private final String phoneNumber;
    private final String appId;

    public PhoneAppContext(@Nonnull Ref<EntityStore> ref,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull String phoneNumber,
                           @Nonnull String appId) {
        this.ref = ref;
        this.store = store;
        this.playerRef = playerRef;
        this.phoneNumber = phoneNumber;
        this.appId = appId;
    }

    // ── Identity ──────────────────────────────────────────────────────────────

    @Nonnull
    public Ref<EntityStore> getRef() {
        return ref;
    }

    @Nonnull
    public Store<EntityStore> getStore() {
        return store;
    }

    @Nonnull
    public PlayerRef getPlayerRef() {
        return playerRef;
    }

    @Nonnull
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /** Returns the ID of the app this context was created for. */
    @Nonnull
    public String getAppId() {
        return appId;
    }

    private String getSessionKey() {
        String sessionKey = phoneNumber + "|" + appId;
        LOGGER.atInfo().log("[PhoneAppContext] sessionKey=%s phone=%s app=%s", sessionKey, phoneNumber, appId);
        return sessionKey;
    }

    // ── Per-player app state ──────────────────────────────────────────────────

    /**
     * Returns the stored value for {@code key} in this app's state, or
     * {@code null} if the key has not been set.
     */
    @Nullable
    public String getState(@Nonnull String key) {
        PhoneAppSessionState s = store.ensureAndGetComponent(ref, PhoneAppSessionState.getComponentType());
        String value = s.get(getSessionKey(), key);
        LOGGER.atInfo().log("[PhoneAppContext] getState phone=%s app=%s key=%s value=%s", phoneNumber, appId, key, value);
        return value;
    }

    /**
     * Returns the stored value for {@code key} in this app's state, or
     * {@code defaultValue} if the key has not been set.
     */
    @Nonnull
    public String getState(@Nonnull String key, @Nonnull String defaultValue) {
        String value = getState(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Stores {@code value} under {@code key} in this app's state.
     * The value persists until the player's entity is unloaded.
     */
    public void setState(@Nonnull String key, @Nonnull String value) {
        PhoneAppSessionState s = store.ensureAndGetComponent(ref, PhoneAppSessionState.getComponentType());
        LOGGER.atInfo().log("[PhoneAppContext] setState phone=%s app=%s key=%s value=%s", phoneNumber, appId, key, value);
        s.set(getSessionKey(), key, value);
        store.putComponent(ref, PhoneAppSessionState.getComponentType(), s);
    }

    /** Removes {@code key} from this app's state. */
    public void clearState(@Nonnull String key) {
        PhoneAppSessionState s = store.ensureAndGetComponent(ref, PhoneAppSessionState.getComponentType());
        LOGGER.atInfo().log("[PhoneAppContext] clearState phone=%s app=%s key=%s", phoneNumber, appId, key);
        s.remove(getSessionKey(), key);
        store.putComponent(ref, PhoneAppSessionState.getComponentType(), s);
    }

    /** Removes all state keys for this app. */
    public void clearAllState() {
        PhoneAppSessionState s = store.ensureAndGetComponent(ref, PhoneAppSessionState.getComponentType());
        LOGGER.atInfo().log("[PhoneAppContext] clearAllState phone=%s app=%s", phoneNumber, appId);
        s.clearApp(getSessionKey());
        store.putComponent(ref, PhoneAppSessionState.getComponentType(), s);
    }

    // ── Convenience ───────────────────────────────────────────────────────────

    /**
     * Sends an in-game notification toast to this player.
     *
     * @param title the bold header line
     * @param body  the subtitle / body line
     */
    public void sendNotification(@Nonnull String title, @Nonnull String body) {
        NotificationUtil.sendNotification(
                playerRef.getPacketHandler(),
                Message.raw(title),
                Message.raw(body));
    }
}
