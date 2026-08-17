package esq.phonemod.identity;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

/** Versioned universe-scoped identity registry document. */
public final class IdentityDocument {
    static final int CURRENT_SCHEMA = 2;
    private static final ArrayCodec<IdentityRecord> RECORDS =
            new ArrayCodec<>(IdentityRecord.CODEC, IdentityRecord[]::new);
    public static final BuilderCodec<IdentityDocument> CODEC = BuilderCodec
            .builder(IdentityDocument.class, IdentityDocument::new)
            .append(new KeyedCodec<>("SchemaVersion", Codec.INTEGER),
                    (o, v) -> o.schemaVersion = v, o -> o.schemaVersion).add()
            .append(new KeyedCodec<>("Records", RECORDS),
                    (o, v) -> o.records = v, o -> o.records).add()
            .build();

    int schemaVersion = CURRENT_SCHEMA;
    IdentityRecord[] records = new IdentityRecord[0];
}
