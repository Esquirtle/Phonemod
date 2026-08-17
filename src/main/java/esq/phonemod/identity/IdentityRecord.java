package esq.phonemod.identity;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/** Codec DTO for one authoritative identity entry. */
public final class IdentityRecord {
    public static final BuilderCodec<IdentityRecord> CODEC = BuilderCodec
            .builder(IdentityRecord.class, IdentityRecord::new)
            .append(new KeyedCodec<>("Id", Codec.UUID_STRING), (o, v) -> o.id = v, o -> o.id).add()
            .append(new KeyedCodec<>("Address", Codec.STRING, true), (o, v) -> o.address = v, o -> o.address).add()
            .append(new KeyedCodec<>("Owner", Codec.UUID_STRING, true), (o, v) -> o.owner = v, o -> o.owner).add()
            .append(new KeyedCodec<>("Shared", Codec.BOOLEAN), (o, v) -> o.shared = v, o -> o.shared).add()
            .append(new KeyedCodec<>("CreatedAt", Codec.LONG),
                    (o, v) -> o.createdAt = v, o -> o.createdAt).add()
            .build();

    UUID id;
    String address;
    UUID owner;
    boolean shared;
    long createdAt;

    public IdentityRecord() {}

    IdentityRecord(UUID id, String address, UUID owner, boolean shared, long createdAt) {
        this.id = id; this.address = address; this.owner = owner;
        this.shared = shared; this.createdAt = createdAt;
    }

    @Nonnull public UUID id() { return id; }
    @Nullable public String address() { return address; }
    @Nullable public UUID owner() { return owner; }
    public boolean shared() { return shared; }
    public long createdAt() { return createdAt; }

    IdentityRecord copy() { return new IdentityRecord(id, address, owner, shared, createdAt); }
}
