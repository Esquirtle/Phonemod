package esq.phonemod.api.v3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Complete immutable metadata and authorization declaration for a device app
 * (D-27: aliases + iconPath; no capabilities/uiPath/appButtonUiPath from v2).
 *
 * <p>{@code id} follows the v2 canonical pattern ({@code AppDescriptor.java:22}),
 * {@code [a-z0-9_.-]+:[a-z0-9_.-]+}.
 */
public record AppDescriptor(
        @Nonnull String id,
        @Nonnull Set<String> aliases,
        @Nonnull String name,
        @Nullable String iconPath,
        @Nonnull Set<String> permissions,
        @Nonnull AppVisibility visibility,
        @Nonnull Set<DeviceType> deviceTypes,
        boolean system,
        int sortOrder,
        @Nonnull Map<Backend, ViewRef> views) {

    public AppDescriptor {
        throw new UnsupportedOperationException("F4");
    }

    /** An empty {@code deviceTypes} set means "all devices" ({@code AppDescriptor.java:40-42}). */
    public boolean supports(@Nonnull DeviceType deviceType) {
        throw new UnsupportedOperationException("F4");
    }

    /**
     * The explicit view for {@code backend}, falling back to {@link Backend#LEGACY},
     * falling back to empty.
     */
    @Nonnull
    public java.util.Optional<ViewRef> viewFor(@Nonnull Backend backend) {
        throw new UnsupportedOperationException("F4");
    }

    @Nonnull
    public static Builder builder(@Nonnull String id) {
        throw new UnsupportedOperationException("F4");
    }

    public static final class Builder {

        Builder(@Nonnull String id) {
            throw new UnsupportedOperationException("F4");
        }

        @Nonnull
        public Builder aliases(@Nonnull Collection<String> aliases) {
            throw new UnsupportedOperationException("F4");
        }

        @Nonnull
        public Builder name(@Nonnull String name) {
            throw new UnsupportedOperationException("F4");
        }

        @Nonnull
        public Builder iconPath(@Nullable String iconPath) {
            throw new UnsupportedOperationException("F4");
        }

        @Nonnull
        public Builder permissions(@Nonnull Collection<String> permissions) {
            throw new UnsupportedOperationException("F4");
        }

        @Nonnull
        public Builder visibility(@Nonnull AppVisibility visibility) {
            throw new UnsupportedOperationException("F4");
        }

        @Nonnull
        public Builder deviceTypes(@Nonnull Collection<DeviceType> deviceTypes) {
            throw new UnsupportedOperationException("F4");
        }

        @Nonnull
        public Builder system(boolean system) {
            throw new UnsupportedOperationException("F4");
        }

        @Nonnull
        public Builder sortOrder(int sortOrder) {
            throw new UnsupportedOperationException("F4");
        }

        @Nonnull
        public Builder view(@Nonnull Backend backend, @Nonnull ViewRef view) {
            throw new UnsupportedOperationException("F4");
        }

        @Nonnull
        public AppDescriptor build() {
            throw new UnsupportedOperationException("F4");
        }
    }
}
