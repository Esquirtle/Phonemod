package esq.phonemod.device.core;

import com.hypixel.hytale.logger.HytaleLogger;
import esq.phonemod.device.assets.DeviceAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves and caches runtime device shells.
 */
public final class DeviceRegistry {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Map<String, DeviceShell> shells = new ConcurrentHashMap<>();

    @Nullable
    public DeviceAsset getAsset(@Nonnull String assetId) {
        try {
            return DeviceAsset.getAssetMap().getAsset(assetId);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[DeviceRegistry] Failed to resolve device asset %s", assetId);
            return null;
        }
    }

    @Nonnull
    public DeviceShell getShell(@Nonnull String assetId) {
        return shells.computeIfAbsent(assetId, this::createShell);
    }

    public boolean hasAsset(@Nonnull String assetId) {
        return getAsset(assetId) != null;
    }

    @Nonnull
    private DeviceShell createShell(@Nonnull String assetId) {
        DeviceAsset asset = getAsset(assetId);
        if (asset == null) {
            throw new IllegalArgumentException("Unknown device asset: " + assetId);
        }
        return new DeviceShell(asset);
    }
}

