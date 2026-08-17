package esq.phonemod.phone.messaging;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

/**
 * Represents a single message in a conversation thread.
 * Mutable so it can be used as a {@link BuilderCodec} target.
 */
public final class ChatMessage {

    public boolean fromMe;
    public String fromName;
    public String body;
    public long sentAt;

    /** Required by {@link BuilderCodec}. */
    public ChatMessage() {}

    public ChatMessage(boolean fromMe, @Nonnull String fromName, @Nonnull String body) {
        this.fromMe = fromMe;
        this.fromName = fromName;
        this.body = body;
        this.sentAt = System.currentTimeMillis();
    }

    public boolean fromMe() { return fromMe; }
    public String fromName() { return fromName == null ? "" : fromName; }
    public String body() { return body == null ? "" : body; }
    /** Epoch millis when this message was created; 0 for legacy messages persisted before timestamps existed. */
    public long sentAt() { return sentAt; }

    public static final BuilderCodec<ChatMessage> CODEC = BuilderCodec
            .builder(ChatMessage.class, ChatMessage::new)
            .append(
                    new KeyedCodec<>("FromMe", Codec.BOOLEAN),
                    (o, v) -> o.fromMe = v,
                    o -> o.fromMe)
            .add()
            .append(
                    new KeyedCodec<>("FromName", Codec.STRING),
                    (o, v) -> o.fromName = v,
                    o -> o.fromName)
            .add()
            .append(
                    new KeyedCodec<>("Body", Codec.STRING),
                    (o, v) -> o.body = v,
                    o -> o.body)
            .add()
            .append(
                    // Optional: legacy messages persisted before timestamps decode as 0
                    // (missing keys silently default), which the UI renders as no time.
                    new KeyedCodec<>("SentAt", Codec.LONG),
                    (o, v) -> o.sentAt = v,
                    o -> o.sentAt)
            .add()
            .build();
}
