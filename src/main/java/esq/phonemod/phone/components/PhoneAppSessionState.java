package esq.phonemod.phone.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Per-player session state for phone apps.
 *
 * Stores a map of {@code sessionKey → (key → value)} where each session key
 * is derived from the phone number and app ID. This ensures each physical
 * phone instance has isolated state even when the same player owns multiple
 * phones. All values are serialized as {@code String}; apps that need booleans,
 * ints, or enums should use {@link String#valueOf} / {@link Boolean#parseBoolean}
 * / {@link Integer#parseInt} / {@code Enum.valueOf}.
 *
 * <p>This component is registered with the ECS but its data is only meaningful
 * for the duration of the current play session — it resets naturally when the
 * player's entity is unloaded (logout / server restart).
 */
public class PhoneAppSessionState implements Component<EntityStore> {

    // ── Fields ───────────────────────────────────────────────────────────────

    /** appId → (stateKey → value) */
    private Map<String, Map<String, String>> stateByApp;

    // ── Codec ────────────────────────────────────────────────────────────────

    public static final BuilderCodec<PhoneAppSessionState> CODEC =
            BuilderCodec.builder(PhoneAppSessionState.class, PhoneAppSessionState::new)
                    .append(
                            new KeyedCodec<>("StateByApp",
                                    new MapCodec<>(
                                            new MapCodec<>(Codec.STRING, HashMap::new, false),
                                            HashMap::new, false)),
                            (data, value) -> data.stateByApp = value,
                            data -> data.stateByApp)
                    .add()
                    .build();

    // ── Constructors ─────────────────────────────────────────────────────────

    public PhoneAppSessionState() {
        this.stateByApp = new HashMap<>();
    }

    public PhoneAppSessionState(@Nonnull PhoneAppSessionState source) {
        this.stateByApp = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> outer : source.stateByApp.entrySet()) {
            this.stateByApp.put(outer.getKey(), new HashMap<>(outer.getValue()));
        }
    }

    // ── Component impl ────────────────────────────────────────────────────────

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new PhoneAppSessionState(this);
    }

    // ── ComponentType ─────────────────────────────────────────────────────────

    private static ComponentType<EntityStore, PhoneAppSessionState> COMPONENT_TYPE;

    public static ComponentType<EntityStore, PhoneAppSessionState> getComponentType() {
        return COMPONENT_TYPE;
    }

    public static void setComponentType(ComponentType<EntityStore, PhoneAppSessionState> type) {
        COMPONENT_TYPE = type;
    }

    // ── API ───────────────────────────────────────────────────────────────────

    @Nullable
    public String get(@Nonnull String appId, @Nonnull String key) {
        Map<String, String> appState = stateByApp.get(appId);
        return appState != null ? appState.get(key) : null;
    }

    @Nonnull
    public String get(@Nonnull String appId, @Nonnull String key, @Nonnull String defaultValue) {
        String value = get(appId, key);
        return value != null ? value : defaultValue;
    }

    public void set(@Nonnull String appId, @Nonnull String key, @Nonnull String value) {
        stateByApp.computeIfAbsent(appId, k -> new HashMap<>()).put(key, value);
    }

    public void remove(@Nonnull String appId, @Nonnull String key) {
        Map<String, String> appState = stateByApp.get(appId);
        if (appState != null) {
            appState.remove(key);
        }
    }

    public void clearApp(@Nonnull String appId) {
        stateByApp.remove(appId);
    }
}
