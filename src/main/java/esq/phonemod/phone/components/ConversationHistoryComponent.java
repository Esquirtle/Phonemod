package esq.phonemod.phone.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.phone.messaging.ChatMessage;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistent component that stores a player's full conversation history across server restarts.
 *
 * <p>History is keyed by contact phone number, and each entry stores an ordered array of
 * {@link ChatMessage} objects serialized via {@link ChatMessage#CODEC}.
 */
public final class ConversationHistoryComponent implements Component<EntityStore> {

    // ── Codec ─────────────────────────────────────────────────────────────────

    private static final ArrayCodec<ChatMessage> MESSAGE_LIST_CODEC =
            new ArrayCodec<>(ChatMessage.CODEC, ChatMessage[]::new);

    public static final BuilderCodec<ConversationHistoryComponent> CODEC =
            BuilderCodec.builder(ConversationHistoryComponent.class, ConversationHistoryComponent::new)
                    .append(
                            new KeyedCodec<>("History",
                                    new MapCodec<>(
                                            new MapCodec<>(MESSAGE_LIST_CODEC, HashMap::new, false),
                                            HashMap::new, false)),
                            (data, value) -> data.history = value,
                            data -> data.history)
                    .add()
                    .build();

    // ── Fields ────────────────────────────────────────────────────────────────

    /** own phone number → contact phone number → ordered message array (oldest first). */
    private Map<String, Map<String, ChatMessage[]>> history;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Default constructor — required by {@link BuilderCodec}. */
    public ConversationHistoryComponent() {
        this.history = new HashMap<>();
    }

    /** Copy constructor used by {@link #clone()}. */
    public ConversationHistoryComponent(@Nonnull ConversationHistoryComponent source) {
        this.history = new HashMap<>();
        for (Map.Entry<String, Map<String, ChatMessage[]>> outer : source.history.entrySet()) {
            Map<String, ChatMessage[]> innerCopy = new HashMap<>();
            for (Map.Entry<String, ChatMessage[]> inner : outer.getValue().entrySet()) {
                innerCopy.put(inner.getKey(), inner.getValue().clone());
            }
            this.history.put(outer.getKey(), innerCopy);
        }
    }

    // ── Component impl ────────────────────────────────────────────────────────

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new ConversationHistoryComponent(this);
    }

    // ── ComponentType ─────────────────────────────────────────────────────────

    private static ComponentType<EntityStore, ConversationHistoryComponent> COMPONENT_TYPE;

    public static ComponentType<EntityStore, ConversationHistoryComponent> getComponentType() {
        return COMPONENT_TYPE;
    }

    public static void setComponentType(
            @Nonnull ComponentType<EntityStore, ConversationHistoryComponent> type) {
        COMPONENT_TYPE = type;
    }

    // ── API ───────────────────────────────────────────────────────────────────

    /**
     * Appends a message to the conversation with the given contact.
     * Returns {@code this} for convenience (mutates in place — caller must call
     * {@code store.putComponent} to persist).
     */
    @Nonnull
    public ConversationHistoryComponent addMessage(@Nonnull String ownNumber,
                                                    @Nonnull String contactNumber,
                                                    @Nonnull ChatMessage message) {
        Map<String, ChatMessage[]> contacts = history.computeIfAbsent(ownNumber, k -> new HashMap<>());
        ChatMessage[] existing = contacts.getOrDefault(contactNumber, new ChatMessage[0]);
        ChatMessage[] updated = Arrays.copyOf(existing, existing.length + 1);
        updated[existing.length] = message;
        contacts.put(contactNumber, updated);
        return this;
    }

    /**
     * Returns an unmodifiable ordered list of messages between {@code ownNumber} and
     * {@code contactNumber}, or an empty list if no history exists.
     */
    @Nonnull
    public List<ChatMessage> getMessages(@Nonnull String ownNumber, @Nonnull String contactNumber) {
        Map<String, ChatMessage[]> contacts = history.get(ownNumber);
        if (contacts == null) return Collections.emptyList();
        ChatMessage[] msgs = contacts.get(contactNumber);
        return msgs != null ? Collections.unmodifiableList(Arrays.asList(msgs)) : Collections.emptyList();
    }

    /**
     * Returns all contacts that have at least one message for the given owning phone number.
     */
    @Nonnull
    public Iterable<String> getContacts(@Nonnull String ownNumber) {
        Map<String, ChatMessage[]> contacts = history.get(ownNumber);
        return contacts != null ? Collections.unmodifiableSet(contacts.keySet()) : Collections.emptySet();
    }
}
