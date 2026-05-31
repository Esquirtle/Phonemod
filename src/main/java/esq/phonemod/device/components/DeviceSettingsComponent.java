package esq.phonemod.device.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Persisted mutable settings keyed by physical device ID.
 */
public final class DeviceSettingsComponent implements Component<EntityStore> {

    public static final BuilderCodec<DeviceSettingsComponent> CODEC = BuilderCodec
            .builder(DeviceSettingsComponent.class, DeviceSettingsComponent::new)
            .append(new KeyedCodec<>("SettingsByDeviceId",
                            new MapCodec<>(DeviceSettings.CODEC, HashMap::new, false)),
                    (component, value) -> component.settingsByDeviceId = value,
                    component -> component.settingsByDeviceId)
            .add()
            .build();

    private static ComponentType<EntityStore, DeviceSettingsComponent> componentType;

    private Map<String, DeviceSettings> settingsByDeviceId;

    public DeviceSettingsComponent() {
        this.settingsByDeviceId = new HashMap<>();
    }

    public DeviceSettingsComponent(@Nonnull DeviceSettingsComponent source) {
        this.settingsByDeviceId = new HashMap<>();
        for (Map.Entry<String, DeviceSettings> entry : source.settingsByDeviceId.entrySet()) {
            this.settingsByDeviceId.put(entry.getKey(), new DeviceSettings(entry.getValue()));
        }
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new DeviceSettingsComponent(this);
    }

    public static ComponentType<EntityStore, DeviceSettingsComponent> getComponentType() {
        return componentType;
    }

    public static void setComponentType(ComponentType<EntityStore, DeviceSettingsComponent> type) {
        componentType = type;
    }

    @Nonnull
    public DeviceSettings getOrCreate(@Nonnull String deviceId) {
        return settingsByDeviceId.computeIfAbsent(deviceId, ignored -> new DeviceSettings());
    }

    @Nullable
    public DeviceSettings get(@Nonnull String deviceId) {
        return settingsByDeviceId.get(deviceId);
    }

    public void put(@Nonnull String deviceId, @Nonnull DeviceSettings settings) {
        settingsByDeviceId.put(deviceId, settings);
    }

    public void clear(@Nonnull String deviceId) {
        settingsByDeviceId.remove(deviceId);
    }
}

