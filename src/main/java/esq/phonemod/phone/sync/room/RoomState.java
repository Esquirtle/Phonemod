package esq.phonemod.phone.sync.room;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import esq.phonemod.phone.sync.MemberAccessor;
import esq.phonemod.phone.sync.SyncChannelType;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Ready-to-use room state: an id, display name, status, capacity and a member list
 * (phone numbers). Apps that need a lobby/group without a bespoke state type can use
 * this directly via {@link #ephemeralType}/{@link #persistentType}; apps with richer
 * state embed their own member list and supply a {@link MemberAccessor} instead.
 *
 * <p>Mutable public fields are required by {@link BuilderCodec}.
 */
public final class RoomState {

    public String id;
    public int schemaVersion;
    public String name;
    /** Free-form lifecycle marker, e.g. {@code "waiting"} / {@code "active"}. */
    public String status;
    public int capacity;
    public String[] members;

    /** Required by {@link BuilderCodec}. */
    public RoomState() {
        this.id = "";
        this.schemaVersion = 2;
        this.name = "";
        this.status = "waiting";
        this.capacity = 0;
        this.members = new String[0];
    }

    public RoomState(@Nonnull String id) {
        this();
        this.id = id;
    }

    @Nonnull
    public List<String> members() {
        return Arrays.asList(members);
    }

    public boolean isFull() {
        return capacity > 0 && members.length >= capacity;
    }

    // ── Codec ─────────────────────────────────────────────────────────────────

    private static final ArrayCodec<String> MEMBERS_CODEC =
            new ArrayCodec<>(Codec.STRING, String[]::new);

    public static final BuilderCodec<RoomState> CODEC = BuilderCodec
            .builder(RoomState.class, RoomState::new)
            .append(new KeyedCodec<>("SchemaVersion", Codec.INTEGER, true),
                    (o, v) -> o.schemaVersion = v, o -> o.schemaVersion).add()
            .append(new KeyedCodec<>("Id", Codec.STRING), (o, v) -> o.id = v, o -> o.id)
            .add()
            .append(new KeyedCodec<>("Name", Codec.STRING), (o, v) -> o.name = v, o -> o.name)
            .add()
            .append(new KeyedCodec<>("Status", Codec.STRING), (o, v) -> o.status = v, o -> o.status)
            .add()
            .append(new KeyedCodec<>("Capacity", Codec.INTEGER), (o, v) -> o.capacity = v, o -> o.capacity)
            .add()
            .append(new KeyedCodec<>("Members", MEMBERS_CODEC), (o, v) -> o.members = v, o -> o.members)
            .add()
            .build();

    /** Member accessor over the {@link #members} array. */
    public static final MemberAccessor<RoomState> MEMBERS = new MemberAccessor<>() {
        @Override
        @Nonnull
        public List<String> members(@Nonnull RoomState state) {
            return state.members();
        }

        @Override
        public void addMember(@Nonnull RoomState state, @Nonnull String phoneNumber) {
            List<String> list = new ArrayList<>(Arrays.asList(state.members));
            if (!list.contains(phoneNumber)) {
                list.add(phoneNumber);
                state.members = list.toArray(new String[0]);
            }
        }

        @Override
        public void removeMember(@Nonnull RoomState state, @Nonnull String phoneNumber) {
            List<String> list = new ArrayList<>(Arrays.asList(state.members));
            if (list.remove(phoneNumber)) {
                state.members = list.toArray(new String[0]);
            }
        }
    };

    // ── Type factories ──────────────────────────────────────────────────────

    /** A temporal room kind (lobby): removed when empty / on dispose. */
    @Nonnull
    public static SyncChannelType<RoomState> ephemeralType(@Nonnull String typeId) {
        return SyncChannelType
                .ephemeral(typeId, CODEC, RoomState::new, s -> s.id)
                .withMembers(MEMBERS);
    }

    /** A durable room kind (group): survives restarts. */
    @Nonnull
    public static SyncChannelType<RoomState> persistentType(@Nonnull String typeId) {
        return SyncChannelType
                .persistent(typeId, CODEC, RoomState::new, s -> s.id)
                .withMembers(MEMBERS);
    }
}
