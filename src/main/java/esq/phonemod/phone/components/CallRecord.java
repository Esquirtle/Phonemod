package esq.phonemod.phone.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

/**
 * Immutable-ish record of a single phone call saved in {@link CallHistoryComponent}.
 * Mutable public fields are required by {@link BuilderCodec}.
 */
public final class CallRecord {

    public String contactNumber;
    public String displayName;
    /** Unix epoch milliseconds when the call ended (or was initiated for missed calls). */
    public long timestamp;
    public boolean outgoing;
    public boolean missed;

    /** Default constructor required by {@link BuilderCodec}. */
    public CallRecord() {}

    public CallRecord(@Nonnull String contactNumber,
                      @Nonnull String displayName,
                      long timestamp,
                      boolean outgoing,
                      boolean missed) {
        this.contactNumber = contactNumber;
        this.displayName   = displayName;
        this.timestamp     = timestamp;
        this.outgoing      = outgoing;
        this.missed        = missed;
    }

    public static final BuilderCodec<CallRecord> CODEC = BuilderCodec
            .builder(CallRecord.class, CallRecord::new)
            .append(new KeyedCodec<>("ContactNumber", Codec.STRING),
                    (o, v) -> o.contactNumber = v, o -> o.contactNumber)
            .add()
            .append(new KeyedCodec<>("DisplayName", Codec.STRING),
                    (o, v) -> o.displayName = v, o -> o.displayName)
            .add()
            .append(new KeyedCodec<>("Timestamp", Codec.LONG),
                    (o, v) -> o.timestamp = v, o -> o.timestamp)
            .add()
            .append(new KeyedCodec<>("Outgoing", Codec.BOOLEAN),
                    (o, v) -> o.outgoing = v, o -> o.outgoing)
            .add()
            .append(new KeyedCodec<>("Missed", Codec.BOOLEAN),
                    (o, v) -> o.missed = v, o -> o.missed)
            .add()
            .build();
}
