package esq.phonemod.api.v3;

import esq.phonemod.api.DeviceIdentity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Stable session identifier, derived (not random, D-26) from the device identity
 * and, for shared devices, the acting player. Rooms are keyed by this id
 * ({@link SyncChannels#joinRoom}/{@link SyncChannels#leaveRoom}), not by address.
 *
 * <p>F2 skeleton: every body throws {@code UnsupportedOperationException("F4")};
 * the derivation ({@code UUID.nameUUIDFromBytes} over
 * {@code "phonemod-session:" + deviceId [+ ":" + player]}) lands in F4.
 */
public record DeviceSessionId(@Nonnull UUID value) {

    public DeviceSessionId {
        throw new UnsupportedOperationException("F4");
    }

    /**
     * @param device the device identity the session belongs to
     * @param player the acting player, only meaningful (and only mixed into the
     *               derivation) when {@code device.shared()} is true; {@code null}
     *               otherwise
     */
    @Nonnull
    public static DeviceSessionId of(@Nonnull DeviceIdentity device, @Nullable UUID player) {
        throw new UnsupportedOperationException("F4");
    }

    @Nonnull
    public static DeviceSessionId parse(@Nonnull String text) {
        throw new UnsupportedOperationException("F4");
    }

    @Override
    @Nonnull
    public String toString() {
        throw new UnsupportedOperationException("F4");
    }
}
