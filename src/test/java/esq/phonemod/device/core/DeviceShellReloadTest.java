package esq.phonemod.device.core;

import esq.phonemod.device.assets.DeviceAsset;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Shell caching and reload semantics of {@link DeviceRegistry}, exercised through the
 * package-private resolver seam so no live asset store is needed. Two registries with
 * different resolvers must stay isolated, and {@code invalidateAll()} must drop stale
 * shells so a mutated backing asset is reflected on the next {@code getShell}.
 */
final class DeviceShellReloadTest {

    @Test
    void getShellReturnsTheSameCachedInstanceUntilInvalidated() {
        Map<String, DeviceAsset> backing = new HashMap<>();
        backing.put("Phone", asset("Phone", "handheld"));
        DeviceRegistry registry = new DeviceRegistry(backing::get);

        DeviceShell first = registry.getShell("Phone");
        DeviceShell second = registry.getShell("Phone");

        assertSame(first, second, "repeat resolution returns the cached shell");
    }

    @Test
    void invalidateAllDropsStaleShellsSoAMutatedAssetIsReflected() {
        Map<String, DeviceAsset> backing = new HashMap<>();
        backing.put("Phone", asset("Phone", "handheld"));
        DeviceRegistry registry = new DeviceRegistry(backing::get);

        DeviceShell original = registry.getShell("Phone");
        assertEquals("handheld", original.getDeviceType());

        // Swap the backing asset; the cache still serves the old shell until invalidated.
        backing.put("Phone", asset("Phone", "tablet"));
        assertSame(original, registry.getShell("Phone"));

        registry.invalidateAll();
        DeviceShell reloaded = registry.getShell("Phone");
        assertNotSame(original, reloaded, "a fresh shell is built after invalidation");
        assertEquals("tablet", reloaded.getDeviceType(), "the new shell reflects the mutated asset");
    }

    @Test
    void tworegistriesWithDifferentResolversNeverCrossContaminate() {
        DeviceRegistry a = new DeviceRegistry(id -> asset(id, "from-A"));
        DeviceRegistry b = new DeviceRegistry(id -> asset(id, "from-B"));

        assertEquals("from-A", a.getShell("Phone").getDeviceType());
        assertEquals("from-B", b.getShell("Phone").getDeviceType());

        a.invalidateAll();
        assertEquals("from-B", b.getShell("Phone").getDeviceType(),
                "invalidating one registry must not disturb the other");
    }

    /** Builds a minimal {@link DeviceAsset} with the fields {@link DeviceShell} requires. */
    private static DeviceAsset asset(String id, String deviceType) {
        try {
            DeviceAsset asset = new DeviceAsset();
            set(asset, "id", id);
            set(asset, "deviceType", deviceType);
            set(asset, "shellUiPath", "UI/Shell.ui");
            set(asset, "contentSelector", "#Content");
            set(asset, "homeHolderSelector", "#Home");
            set(asset, "metadataKey", "meta." + id);
            return asset;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void set(DeviceAsset asset, String field, String value)
            throws ReflectiveOperationException {
        Field f = DeviceAsset.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(asset, value);
    }
}
