package esq.phonemod.phone.apps.groupchat;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

/**
 * Shared state of one group chat: its id, display name, and ordered messages.
 * This is the object persisted as JSON and synchronized across all viewers.
 * Mutable fields required by {@link BuilderCodec}.
 */
public final class GroupChatState {

    /** Keep the synced view bounded; drop oldest beyond this many messages. */
    public String id;
    public int schemaVersion;
    public String name;
    public GroupMessage[] messages;

    public GroupChatState() {
        this.id = "";
        this.schemaVersion = 2;
        this.name = "";
        this.messages = new GroupMessage[0];
    }

    public GroupChatState(@Nonnull String id) {
        this();
        this.id = id;
    }

    /** Appends a message (trimming the oldest past {@link #MAX_MESSAGES}). */
    public void add(@Nonnull GroupMessage message) {
        GroupMessage[] next = Arrays.copyOf(messages, messages.length + 1);
        next[messages.length] = message;
        int limit = esq.phonemod.PhoneMod.config().persistentChannelMessages();
        if (next.length > limit) {
            next = Arrays.copyOfRange(next, next.length - limit, next.length);
        }
        this.messages = next;
    }

    @Nonnull
    public List<GroupMessage> messages() {
        return Arrays.asList(messages);
    }

    private static final ArrayCodec<GroupMessage> MESSAGES_CODEC =
            new ArrayCodec<>(GroupMessage.CODEC, GroupMessage[]::new);

    public static final BuilderCodec<GroupChatState> CODEC = BuilderCodec
            .builder(GroupChatState.class, GroupChatState::new)
            .append(new KeyedCodec<>("SchemaVersion", Codec.INTEGER, true),
                    (o, v) -> o.schemaVersion = v, o -> o.schemaVersion).add()
            .append(new KeyedCodec<>("Id", Codec.STRING), (o, v) -> o.id = v, o -> o.id)
            .add()
            .append(new KeyedCodec<>("Name", Codec.STRING), (o, v) -> o.name = v, o -> o.name)
            .add()
            .append(new KeyedCodec<>("Messages", MESSAGES_CODEC), (o, v) -> o.messages = v, o -> o.messages)
            .add()
            .build();
}
