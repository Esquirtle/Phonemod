package esq.phonemod.platform;

import esq.phonemod.api.DeviceAppContext;
import esq.phonemod.api.DeviceIdentity;
import esq.phonemod.api.SyncService;
import esq.phonemod.api.WorldAccess;
import esq.phonemod.phone.api.PhoneAppContext;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

final class RuntimeDeviceAppContext implements DeviceAppContext {
    private final PhoneAppContext legacy;
    RuntimeDeviceAppContext(PhoneAppContext legacy) { this.legacy = legacy; }
    public com.hypixel.hytale.server.core.universe.PlayerRef player() { return legacy.getPlayerRef(); }
    public DeviceIdentity identity() {
        var identities = RuntimeDevicePlatform.get().identities();
        try {
            var byId = identities.find(UUID.fromString(legacy.getPhoneNumber()));
            if (byId.isPresent()) return byId.get();
        } catch (IllegalArgumentException ignored) {}
        return identities.findAddress(legacy.getPhoneNumber()).orElseGet(() -> new DeviceIdentity(
                UUID.nameUUIDFromBytes(legacy.getPhoneNumber().getBytes(StandardCharsets.UTF_8)),
                legacy.getPhoneNumber(), false));
    }
    public void executeOnWorld(@Nonnull Consumer<WorldAccess> work) {
        var ref = legacy.getRef(); // plain field read — safe off-thread
        if (ref == null) return;
        ref.getStore().getExternalData().getWorld().execute(() -> {
            if (ref.isValid()) work.accept(new WorldAccess(ref, legacy.getStore()));
        });
    }
    public Optional<String> state(String key) { return Optional.ofNullable(legacy.getState(key)); }
    public void state(String key, String value) { legacy.setState(key, value); }
    public void notify(String title, String body) { legacy.sendNotification(title, body); }
    public SyncService sync() { return RuntimeDevicePlatform.get().sync(); }
}
