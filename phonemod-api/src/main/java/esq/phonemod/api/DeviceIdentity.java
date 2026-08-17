package esq.phonemod.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/** Stable device identity; the human-facing address is optional and mutable. */
public record DeviceIdentity(@Nonnull UUID id, @Nullable String address, boolean shared) {
    public DeviceIdentity {
        if (id == null) throw new IllegalArgumentException("id is required");
        if (address != null && (address.isBlank() || address.length() > 64)) {
            throw new IllegalArgumentException("Invalid device address");
        }
        if (shared && address != null && address.isBlank()) address = null;
    }

    @Nonnull public Optional<String> optionalAddress() { return Optional.ofNullable(address); }
}
