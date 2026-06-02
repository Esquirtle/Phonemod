package esq.phonemod.phone.sync;

import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Registered descriptor for one kind of synchronized channel — the schema and
 * policy shared by every channel of that kind.
 *
 * <p>An app declares one of these once (typically as a {@code static final}) and
 * registers it with {@link SyncService#registerType}. Channels of the type are then
 * created on demand by id.
 *
 * @param <S> the channel's shared state type, serialized via {@link #codec()}
 */
public final class SyncChannelType<S> {

    private final String typeId;
    private final BuilderCodec<S> codec;
    /** Builds the initial state for a brand-new channel, stamping in its id. */
    private final Function<String, S> stateFactory;
    /** Reads a channel's id back out of its state (used to rehydrate from disk). */
    private final Function<S, String> idAccessor;
    private final ChannelPersistence persistence;
    @Nullable
    private final MemberAccessor<S> memberAccessor;

    private SyncChannelType(@Nonnull String typeId,
            @Nonnull BuilderCodec<S> codec,
            @Nonnull Function<String, S> stateFactory,
            @Nonnull Function<S, String> idAccessor,
            @Nonnull ChannelPersistence persistence,
            @Nullable MemberAccessor<S> memberAccessor) {
        this.typeId = typeId;
        this.codec = codec;
        this.stateFactory = stateFactory;
        this.idAccessor = idAccessor;
        this.persistence = persistence;
        this.memberAccessor = memberAccessor;
    }

    /** A durable channel kind (survives restarts) — e.g. a chat group. */
    @Nonnull
    public static <S> SyncChannelType<S> persistent(@Nonnull String typeId,
            @Nonnull BuilderCodec<S> codec,
            @Nonnull Function<String, S> stateFactory,
            @Nonnull Function<S, String> idAccessor) {
        return new SyncChannelType<>(typeId, codec, stateFactory, idAccessor,
                ChannelPersistence.PERSISTENT, null);
    }

    /** A temporal channel kind (removed on dispose / startup) — e.g. a game lobby. */
    @Nonnull
    public static <S> SyncChannelType<S> ephemeral(@Nonnull String typeId,
            @Nonnull BuilderCodec<S> codec,
            @Nonnull Function<String, S> stateFactory,
            @Nonnull Function<S, String> idAccessor) {
        return new SyncChannelType<>(typeId, codec, stateFactory, idAccessor,
                ChannelPersistence.EPHEMERAL, null);
    }

    /** Returns a copy of this type that exposes its membership list to the room layer. */
    @Nonnull
    public SyncChannelType<S> withMembers(@Nonnull MemberAccessor<S> accessor) {
        return new SyncChannelType<>(typeId, codec, stateFactory, idAccessor, persistence, accessor);
    }

    @Nonnull
    public String typeId() {
        return typeId;
    }

    @Nonnull
    public BuilderCodec<S> codec() {
        return codec;
    }

    @Nonnull
    public S newState(@Nonnull String id) {
        return stateFactory.apply(id);
    }

    @Nonnull
    public String idOf(@Nonnull S state) {
        return idAccessor.apply(state);
    }

    @Nonnull
    public ChannelPersistence persistence() {
        return persistence;
    }

    @Nullable
    public MemberAccessor<S> memberAccessor() {
        return memberAccessor;
    }
}
