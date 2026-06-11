package esq.phonemod.phone.appstore;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Persistent ECS component that tracks which apps are installed on each device
 * owned by a player.
 *
 * <p>The outer map key is the device ID (e.g. {@code "555-0001"}); the inner
 * set contains the app IDs that are installed on that device.
 *
 * <p>System apps are installed idempotently on every device open and cannot
 * appear in this set until the seeding logic has run — see
 * {@link AppInstallService#seedDefaults}.
 *
 * <p>KeyedCodec ID: {@code "DeviceInstalledApps"} — globally unique.
 */
public final class DeviceInstalledAppsComponent implements Component<EntityStore> {

    // ── Codec ─────────────────────────────────────────────────────────────────

    public static final BuilderCodec<DeviceInstalledAppsComponent> CODEC =
            BuilderCodec.builder(DeviceInstalledAppsComponent.class, DeviceInstalledAppsComponent::new)
                    .append(
                            new KeyedCodec<>("DeviceInstalledApps",
                                    new MapCodec<>(
                                            new SetCodec<String, HashSet<String>>(Codec.STRING, HashSet::new, false),
                                            HashMap::new, false)),
                            (comp, value) -> comp.installedByDeviceId = value,
                            comp -> comp.installedByDeviceId)
                    .add()
                    .build();

    // ── State ─────────────────────────────────────────────────────────────────

    private Map<String, Set<String>> installedByDeviceId;

    // ── ComponentType holder ──────────────────────────────────────────────────

    private static ComponentType<EntityStore, DeviceInstalledAppsComponent> componentType;

    public static ComponentType<EntityStore, DeviceInstalledAppsComponent> getComponentType() {
        return componentType;
    }

    public static void setComponentType(ComponentType<EntityStore, DeviceInstalledAppsComponent> type) {
        componentType = type;
    }

    // ── Constructors ──────────────────────────────────────────────────────────

    public DeviceInstalledAppsComponent() {
        this.installedByDeviceId = new HashMap<>();
    }

    public DeviceInstalledAppsComponent(@Nonnull DeviceInstalledAppsComponent source) {
        this.installedByDeviceId = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : source.installedByDeviceId.entrySet()) {
            this.installedByDeviceId.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new DeviceInstalledAppsComponent(this);
    }

    // ── API ───────────────────────────────────────────────────────────────────

    /**
     * Returns the mutable set of installed app IDs for {@code deviceId}.
     * Creates an empty set if none exists — callers must persist the component
     * after mutating.
     */
    @Nonnull
    public Set<String> getOrCreate(@Nonnull String deviceId) {
        return installedByDeviceId.computeIfAbsent(deviceId, ignored -> new HashSet<>());
    }

    /**
     * Returns an unmodifiable view of installed apps for {@code deviceId}.
     * Returns an empty set if no entry exists yet.
     */
    @Nonnull
    public Set<String> getInstalled(@Nonnull String deviceId) {
        Set<String> apps = installedByDeviceId.get(deviceId);
        return apps != null ? java.util.Collections.unmodifiableSet(apps) : java.util.Collections.emptySet();
    }

    /** Returns {@code true} if {@code appId} is installed on {@code deviceId}. */
    public boolean isInstalled(@Nonnull String deviceId, @Nonnull String appId) {
        Set<String> apps = installedByDeviceId.get(deviceId);
        return apps != null && apps.contains(appId);
    }

    /**
     * Adds {@code appId} to the installed set for {@code deviceId}.
     * Callers must persist the component after calling this method.
     */
    public void install(@Nonnull String deviceId, @Nonnull String appId) {
        installedByDeviceId.computeIfAbsent(deviceId, ignored -> new HashSet<>()).add(appId);
    }

    /**
     * Removes {@code appId} from the installed set for {@code deviceId}.
     * Callers must persist the component after calling this method.
     */
    public void uninstall(@Nonnull String deviceId, @Nonnull String appId) {
        Set<String> apps = installedByDeviceId.get(deviceId);
        if (apps != null) {
            apps.remove(appId);
        }
    }
}
