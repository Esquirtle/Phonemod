package esq.phonemod.ui.kit.host;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import esq.phonemod.api.DeviceIdentity;
import esq.phonemod.api.v3.DeviceSessionId;
import esq.phonemod.api.v3.DeviceType;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * Immutable snapshot of a session's identity, independent of any live app.
 * {@code player} is behind a {@link Supplier} because a real {@link PlayerRef}
 * cannot be constructed in tests ({@code PlayerRef.java:199}).
 */
public record DeviceSessionInfo(
        @Nonnull DeviceSessionId id,
        @Nonnull DeviceIdentity owner,
        @Nonnull Supplier<PlayerRef> player,
        @Nonnull DeviceIdentity identity,
        @Nonnull DeviceType deviceType,
        @Nonnull String address) {

    public DeviceSessionInfo {
        throw new UnsupportedOperationException("F4");
    }
}
