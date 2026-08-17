package esq.phonemod.phone.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.phone.messaging.ChatMessage;
import esq.phonemod.PhoneMod;

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
                    .append(new KeyedCodec<>("SchemaVersion", com.hypixel.hytale.codec.Codec.INTEGER, true),
                            (data, value) -> data.schemaVersion = value, data -> data.schemaVersion).add()
                    .append(
                            new KeyedCodec<>("History",
                                    new MapCodec<>(
                                            new MapCodec<>(MESSAGE_LIST_CODEC, HashMap::new, false),
                                            HashMap::new, false)),
                            (data, value) -> data.history = value,
                            data -> data.history)
                    .add()
                    .append(
                            // own number → contact number → unread incoming count.
                            // Optional key: legacy data without it decodes to an empty map.
                            new KeyedCodec<>("Unread",
                                    new MapCodec<>(
                                            new MapCodec<>(com.hypixel.hytale.codec.Codec.INTEGER, HashMap::new, false),
                                            HashMap::new, false)),
                            (data, value) -> data.unread = value,
                            data -> data.unread)
                    .add()
                    .build();

    // ── Fields ────────────────────────────────────────────────────────────────

    /** own phone number → contact phone number → ordered message array (oldest first). */
    private Map<String, Map<String, ChatMessage[]>> history;
    /** own phone number → contact phone number → unread incoming message count. */
    private Map<String, Map<String, Integer>> unread;
    private int schemaVersion = 2;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Default constructor — required by {@link BuilderCodec}. */
    public ConversationHistoryComponent() {
        this.history = new HashMap<>();
        this.unread = new HashMap<>();
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
        this.unread = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> outer : source.unread.entrySet()) {
            this.unread.put(outer.getKey(), new HashMap<>(outer.getValue()));
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
        int limit = PhoneMod.config().messagesPerConversation();
        int retained = Math.min(existing.length, Math.max(0, limit - 1));
        ChatMessage[] updated = new ChatMessage[retained + 1];
        if (retained > 0) {
            System.arraycopy(existing, existing.length - retained, updated, 0, retained);
        }
        updated[retained] = message;
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
     * Returns the most recent message between {@code ownNumber} and {@code contactNumber},
     * or {@code null} if the conversation has no messages. Used by the conversation list to
     * render a last-message preview and relative timestamp.
     */
    @javax.annotation.Nullable
    public ChatMessage getLastMessage(@Nonnull String ownNumber, @Nonnull String contactNumber) {
        Map<String, ChatMessage[]> contacts = history.get(ownNumber);
        if (contacts == null) return null;
        ChatMessage[] msgs = contacts.get(contactNumber);
        return msgs != null && msgs.length > 0 ? msgs[msgs.length - 1] : null;
    }

    /**
     * Returns all contacts that have at least one message for the given owning phone number.
     */
    @Nonnull
    public Iterable<String> getContacts(@Nonnull String ownNumber) {
        Map<String, ChatMessage[]> contacts = history.get(ownNumber);
        return contacts != null ? Collections.unmodifiableSet(contacts.keySet()) : Collections.emptySet();
    }

    // ── Unread counts ─────────────────────────────────────────────────────────

    /** Unread incoming messages from {@code contactNumber} for {@code ownNumber}. */
    public int getUnread(@Nonnull String ownNumber, @Nonnull String contactNumber) {
        Map<String, Integer> contacts = unread.get(ownNumber);
        if (contacts == null) return 0;
        Integer count = contacts.get(contactNumber);
        return count != null ? count : 0;
    }

    /** Increments the unread count for a received message (caller persists via putComponent). */
    public void incrementUnread(@Nonnull String ownNumber, @Nonnull String contactNumber) {
        unread.computeIfAbsent(ownNumber, k -> new HashMap<>()).merge(contactNumber, 1, Integer::sum);
    }

    /** Clears the unread count once the conversation is opened/viewed. */
    public void clearUnread(@Nonnull String ownNumber, @Nonnull String contactNumber) {
        Map<String, Integer> contacts = unread.get(ownNumber);
        if (contacts != null) contacts.remove(contactNumber);
    }

    public void remapDevice(@Nonnull String oldAddress, @Nonnull String newAddress) {
        Map<String, ChatMessage[]> old = history.remove(oldAddress);
        if (old != null) history.putIfAbsent(newAddress, old);
        Map<String, Integer> oldUnread = unread.remove(oldAddress);
        if (oldUnread != null) unread.putIfAbsent(newAddress, oldUnread);
    }
}
