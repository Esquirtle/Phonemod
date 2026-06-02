package esq.phonemod.phone.sync;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Lets the room layer read and mutate the membership list embedded in an app's own
 * channel state {@code S}, without the framework knowing the concrete state type.
 * Supplied on a {@link SyncChannelType} via {@link SyncChannelType#withMembers}.
 */
public interface MemberAccessor<S> {

    @Nonnull
    List<String> members(@Nonnull S state);

    void addMember(@Nonnull S state, @Nonnull String phoneNumber);

    void removeMember(@Nonnull S state, @Nonnull String phoneNumber);
}
