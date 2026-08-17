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
 * Per-player persisted state for phone apps.
 *
 * Stores a map of {@code sessionKey → (key → value)} where each session key
 * is derived from the phone number and app ID. This ensures each physical
 * phone instance has isolated state even when the same player owns multiple
 * phones. All values are serialized as {@code String}; apps that need booleans,
 * ints, or enums should use {@link String#valueOf} / {@link Boolean#parseBoolean}
 * / {@link Integer#parseInt} / {@code Enum.valueOf}.
 *
 * <p>This codec-backed ECS component is persisted when callers write it through
 * {@code Store.putComponent}. State survives entity unloads and server restarts
 * until an app explicitly clears it.
 */
public class PhoneAppSessionState implements Component<EntityStore> {

    // ── Fields ───────────────────────────────────────────────────────────────

    /** appId → (stateKey → value) */
    private Map<String, Map<String, String>> stateByApp;
    private int schemaVersion = 2;

    // ── Codec ────────────────────────────────────────────────────────────────

    public static final BuilderCodec<PhoneAppSessionState> CODEC =
            BuilderCodec.builder(PhoneAppSessionState.class, PhoneAppSessionState::new)
                    .append(new KeyedCodec<>("SchemaVersion", Codec.INTEGER, true),
                            (data, value) -> data.schemaVersion = value, data -> data.schemaVersion).add()
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
        if (appId.length() > 129 || key.isBlank() || key.length() > 64 || value.length() > 1024) {
            throw new IllegalArgumentException("Invalid app session state");
        }
        Map<String, String> appState = stateByApp.computeIfAbsent(appId, k -> new HashMap<>());
        if (!appState.containsKey(key) && appState.size() >= 32) {
            throw new IllegalStateException("App session state limit reached");
        }
        appState.put(key, value);
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

    public void remapDevice(@Nonnull String oldAddress, @Nonnull String newAddress) {
        String prefix = oldAddress + "|";
        Map<String, Map<String, String>> additions = new HashMap<>();
        java.util.Iterator<Map.Entry<String, Map<String, String>>> iterator = stateByApp.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Map<String, String>> entry = iterator.next();
            if (entry.getKey().startsWith(prefix)) {
                additions.putIfAbsent(newAddress + entry.getKey().substring(oldAddress.length()), entry.getValue());
                iterator.remove();
            }
        }
        stateByApp.putAll(additions);
    }
}
