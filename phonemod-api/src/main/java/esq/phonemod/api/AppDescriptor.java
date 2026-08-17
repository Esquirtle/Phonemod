package esq.phonemod.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Complete immutable metadata and authorization declaration for a device app. */
public record AppDescriptor(
        @Nonnull String id,
        @Nonnull Set<String> legacyAliases,
        @Nonnull String localizationKey,
        @Nullable String iconPath,
        @Nonnull String uiPath,
        @Nullable String appButtonUiPath,
        @Nonnull Set<String> capabilities,
        @Nonnull Set<String> permissions,
        @Nonnull Set<String> supportedDeviceTypes,
        boolean system,
        int sortOrder) {
    private static final Pattern CANONICAL = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_.-]+");
    private static final Pattern ALIAS = Pattern.compile("[a-z0-9_.:-]{1,64}");

    public AppDescriptor {
        if (id == null || !CANONICAL.matcher(id).matches())
            throw new IllegalArgumentException("App id must be namespaced: " + id);
        if (localizationKey == null || localizationKey.isBlank())
            throw new IllegalArgumentException("localizationKey is required");
        if (uiPath == null || uiPath.isBlank())
            throw new IllegalArgumentException("uiPath is required");
        legacyAliases = legacyAliases == null ? Set.of() : Set.copyOf(legacyAliases);
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        supportedDeviceTypes = supportedDeviceTypes == null ? Set.of() : Set.copyOf(supportedDeviceTypes);
        if (legacyAliases.contains(id) || legacyAliases.stream().anyMatch(a -> !ALIAS.matcher(a).matches()))
            throw new IllegalArgumentException("Invalid legacy alias");
    }

    public boolean supports(@Nonnull String deviceType) {
        return supportedDeviceTypes.isEmpty() || supportedDeviceTypes.contains(deviceType);
    }
}
