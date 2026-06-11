package esq.phonemod.phone.apps.groupchat;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

/**
 * One message in a {@link GroupChatState}. Unlike {@code ChatMessage} (whose
 * {@code fromMe} is viewer-relative), a group message stores the sender's identity
 * so every viewer can render it correctly. Mutable fields required by
 * {@link BuilderCodec}.
 */
public final class GroupMessage {

    public String sender;
    public String senderName;
    public String body;
    public long timestamp;

    public GroupMessage() {
        this.sender = "";
        this.senderName = "";
        this.body = "";
        this.timestamp = 0L;
    }

    public GroupMessage(@Nonnull String sender, @Nonnull String senderName, @Nonnull String body, long timestamp) {
        this.sender = sender;
        this.senderName = senderName;
        this.body = body;
        this.timestamp = timestamp;
    }

    public static final BuilderCodec<GroupMessage> CODEC = BuilderCodec
            .builder(GroupMessage.class, GroupMessage::new)
            .append(new KeyedCodec<>("Sender", Codec.STRING), (o, v) -> o.sender = v, o -> o.sender)
            .add()
            .append(new KeyedCodec<>("SenderName", Codec.STRING), (o, v) -> o.senderName = v, o -> o.senderName)
            .add()
            .append(new KeyedCodec<>("Body", Codec.STRING), (o, v) -> o.body = v, o -> o.body)
            .add()
            .append(new KeyedCodec<>("Timestamp", Codec.LONG), (o, v) -> o.timestamp = v, o -> o.timestamp)
            .add()
            .build();
}
