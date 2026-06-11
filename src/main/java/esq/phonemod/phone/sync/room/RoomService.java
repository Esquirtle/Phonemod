package esq.phonemod.phone.sync.room;

import com.hypixel.hytale.logger.HytaleLogger;
import esq.phonemod.phone.sync.ChannelPersistence;
import esq.phonemod.phone.sync.MemberAccessor;
import esq.phonemod.phone.sync.SyncChannel;
import esq.phonemod.phone.sync.SyncChannelType;
import esq.phonemod.phone.sync.SyncService;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Membership + matchmaking on top of {@link SyncService}. A "room" is just a sync
 * channel whose type exposes a {@link MemberAccessor} (via
 * {@link SyncChannelType#withMembers}); membership changes are ordinary state
 * mutations, so they persist and broadcast like everything else.
 *
 * <p>Lifecycle policy is the channel type's {@link ChannelPersistence}: an
 * EPHEMERAL room (lobby) is disposed — and its JSON removed — once the last member
 * leaves; a PERSISTENT room (group) is kept.
 */
public final class RoomService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private RoomService() {
    }

    /** Creates a room with a generated id and {@code host} as its first member. */
    @Nonnull
    public static <S> String createRoom(@Nonnull SyncChannelType<S> type, @Nonnull String host) {
        requireMembers(type);
        String id = UUID.randomUUID().toString().substring(0, 8);
        SyncService.getOrCreate(type, id);
        join(type, id, host);
        LOGGER.atFine().log("[RoomService] Created room %s/%s host=%s", type.typeId(), id, host);
        return id;
    }

    /** Adds {@code phoneNumber} to a room. Returns false if already a member. */
    public static <S> boolean join(@Nonnull SyncChannelType<S> type,
            @Nonnull String id,
            @Nonnull String phoneNumber) {
        MemberAccessor<S> accessor = requireMembers(type);
        boolean[] added = {false};
        SyncService.publish(type, id, state -> {
            if (!accessor.members(state).contains(phoneNumber)) {
                accessor.addMember(state, phoneNumber);
                added[0] = true;
            }
        });
        return added[0];
    }

    /**
     * Removes {@code phoneNumber} from a room, broadcasting the change. An EPHEMERAL
     * room with no remaining members is disposed (its JSON is deleted).
     */
    public static <S> void leave(@Nonnull SyncChannelType<S> type,
            @Nonnull String id,
            @Nonnull String phoneNumber) {
        MemberAccessor<S> accessor = requireMembers(type);
        if (SyncService.get(type, id) == null) {
            return;
        }
        SyncService.publish(type, id, state -> accessor.removeMember(state, phoneNumber));

        SyncChannel<S> after = SyncService.get(type, id);
        if (after != null
                && type.persistence() == ChannelPersistence.EPHEMERAL
                && accessor.members(after.read()).isEmpty()) {
            SyncService.dispose(type, id);
        }
    }

    /** All current room ids of a type. */
    @Nonnull
    public static <S> List<String> list(@Nonnull SyncChannelType<S> type) {
        List<String> ids = new ArrayList<>();
        for (SyncChannel<S> channel : SyncService.channelsOfType(type)) {
            ids.add(channel.id());
        }
        return ids;
    }

    /**
     * Matchmaking: joins {@code phoneNumber} to an existing room of {@code type} with
     * spare capacity (or the one they are already in), creating a fresh room when
     * none is available. Returns the room id.
     */
    @Nonnull
    public static <S> String findOpen(@Nonnull SyncChannelType<S> type,
            @Nonnull String phoneNumber,
            int capacity) {
        MemberAccessor<S> accessor = requireMembers(type);
        for (SyncChannel<S> channel : SyncService.channelsOfType(type)) {
            List<String> members = accessor.members(channel.read());
            if (members.contains(phoneNumber)) {
                return channel.id();
            }
            if (capacity <= 0 || members.size() < capacity) {
                join(type, channel.id(), phoneNumber);
                return channel.id();
            }
        }
        return createRoom(type, phoneNumber);
    }

    /** Removes a phone from every room it belongs to (any room type). For disconnect. */
    public static void leaveAll(@Nonnull String phoneNumber) {
        for (SyncChannel<?> channel : SyncService.allChannels()) {
            leaveIfMember(channel, phoneNumber);
        }
    }

    private static <S> void leaveIfMember(@Nonnull SyncChannel<S> channel, @Nonnull String phoneNumber) {
        MemberAccessor<S> accessor = channel.type().memberAccessor();
        if (accessor == null) {
            return;
        }
        if (accessor.members(channel.read()).contains(phoneNumber)) {
            leave(channel.type(), channel.id(), phoneNumber);
        }
    }

    @Nonnull
    private static <S> MemberAccessor<S> requireMembers(@Nonnull SyncChannelType<S> type) {
        MemberAccessor<S> accessor = type.memberAccessor();
        if (accessor == null) {
            throw new IllegalArgumentException(
                    "Channel type '" + type.typeId() + "' is not a room; build it with withMembers(...)");
        }
        return accessor;
    }
}
