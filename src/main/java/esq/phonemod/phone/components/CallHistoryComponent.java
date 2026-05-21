package esq.phonemod.phone.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistent component that stores a phone's full call history across server restarts.
 *
 * <p>History is keyed by own phone number (phone-locked), then stores an ordered
 * array of {@link CallRecord} objects serialized via {@link CallRecord#CODEC}.
 */
public final class CallHistoryComponent implements Component<EntityStore> {

    // ── Codec ─────────────────────────────────────────────────────────────────

    private static final ArrayCodec<CallRecord> RECORD_ARRAY_CODEC =
            new ArrayCodec<>(CallRecord.CODEC, CallRecord[]::new);

    public static final BuilderCodec<CallHistoryComponent> CODEC =
            BuilderCodec.builder(CallHistoryComponent.class, CallHistoryComponent::new)
                    .append(
                            new KeyedCodec<>("History",
                                    new MapCodec<>(RECORD_ARRAY_CODEC, HashMap::new, false)),
                            (data, value) -> data.history = value,
                            data -> data.history)
                    .add()
                    .build();

    // ── Fields ────────────────────────────────────────────────────────────────

    /** own phone number → ordered call records (most recent last). */
    private Map<String, CallRecord[]> history;

    // ── Constructors ──────────────────────────────────────────────────────────

    public CallHistoryComponent() {
        this.history = new HashMap<>();
    }

    public CallHistoryComponent(@Nonnull CallHistoryComponent source) {
        this.history = new HashMap<>();
        for (Map.Entry<String, CallRecord[]> entry : source.history.entrySet()) {
            this.history.put(entry.getKey(), entry.getValue().clone());
        }
    }

    // ── Component impl ────────────────────────────────────────────────────────

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new CallHistoryComponent(this);
    }

    // ── ComponentType ─────────────────────────────────────────────────────────

    private static ComponentType<EntityStore, CallHistoryComponent> COMPONENT_TYPE;

    public static ComponentType<EntityStore, CallHistoryComponent> getComponentType() {
        return COMPONENT_TYPE;
    }

    public static void setComponentType(
            @Nonnull ComponentType<EntityStore, CallHistoryComponent> type) {
        COMPONENT_TYPE = type;
    }

    // ── API ───────────────────────────────────────────────────────────────────

    /** Appends a call record. Caller must call {@code store.putComponent} to persist. */
    @Nonnull
    public CallHistoryComponent addRecord(@Nonnull String ownNumber, @Nonnull CallRecord record) {
        CallRecord[] existing = history.getOrDefault(ownNumber, new CallRecord[0]);
        CallRecord[] updated  = Arrays.copyOf(existing, existing.length + 1);
        updated[existing.length] = record;
        history.put(ownNumber, updated);
        return this;
    }

    /**
     * Returns an unmodifiable list of call records for the given own phone number,
     * ordered oldest-first, or empty if none.
     */
    @Nonnull
    public List<CallRecord> getHistory(@Nonnull String ownNumber) {
        CallRecord[] records = history.get(ownNumber);
        return records != null
                ? Collections.unmodifiableList(Arrays.asList(records))
                : Collections.emptyList();
    }
}
