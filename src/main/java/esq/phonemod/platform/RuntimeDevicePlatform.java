package esq.phonemod.platform;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import esq.phonemod.device.core.DeviceService;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import esq.phonemod.api.CallingService;
import esq.phonemod.api.DeviceIdentity;
import esq.phonemod.api.DevicePlatform;
import esq.phonemod.api.MessagingService;
import esq.phonemod.api.NotificationService;
import esq.phonemod.api.PresenceService;
import esq.phonemod.identity.IdentityRegistry;
import esq.phonemod.phone.messaging.CallRegistry;
import esq.phonemod.phone.messaging.PhoneRegistry;
import esq.phonemod.phone.messaging.TextMessage;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Host implementation installed for the lifetime of the runtime plugin. */
public final class RuntimeDevicePlatform implements DevicePlatform {
    private static RuntimeDevicePlatform instance;
    private final RuntimeAppService apps = new RuntimeAppService();
    private final IdentityRegistry identities = new IdentityRegistry();
    private final Map<String, String> stringChannels = new ConcurrentHashMap<>();

    private final esq.phonemod.api.DeviceService devices = new esq.phonemod.api.DeviceService() {
        public Optional<DeviceIdentity> find(UUID id) { return identities.find(id); }
        public Optional<DeviceIdentity> findByAddress(String address) { return identities.findAddress(address); }
        public Collection<DeviceIdentity> snapshot() { return identities.snapshot(); }
        public void openShared(com.hypixel.hytale.server.core.universe.PlayerRef playerRef,
                com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> ref,
                com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> store,
                String deviceAssetId, String stableKey) {
            DeviceIdentity identity = identities.ensureShared(stableKey);
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) throw new IllegalStateException("Player component unavailable");
            var session = DeviceService.get().createSession(playerRef, ref, store,
                    deviceAssetId, identity.id().toString());
            var page = DeviceService.get().createDevicePage(session);
            player.getWorld().execute(() -> {
                if (ref.isValid()) player.getPageManager().openCustomPage(
                        playerRef.getReference(), store, page);
            });
        }
        public Map<String, String> contacts(
                com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> ref,
                com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> store,
                String address) {
            return DeviceService.get().getContactService().getContacts(store, ref, address);
        }
    };
    private final PresenceService presence = identity -> identity.address() != null
            && PhoneRegistry.getPresenceEntry(identity.address()) != null;
    private final MessagingService messaging = (from, to, body) -> from.address() != null
            && PhoneRegistry.deliver(to, new TextMessage(from.address(), from.address(), body));
    private final CallingService calling = new CallingService() {
        public boolean call(DeviceIdentity from, String to) {
            return from.address() != null && CallRegistry.initiateCall(from.address(), to, to);
        }
        public void hangUp(DeviceIdentity endpoint) {
            if (endpoint.address() != null) CallRegistry.hangUp(endpoint.address());
        }
    };
    private final NotificationService notifications = (target, title, body) -> {
        if (target.address() == null) return;
        PhoneRegistry.PresenceEntry entry = PhoneRegistry.getPresenceEntry(target.address());
        if (entry != null) NotificationUtil.sendNotification(entry.playerRef().getPacketHandler(),
                Message.raw(title), Message.raw(body));
    };
    private final esq.phonemod.api.SyncService sync = new esq.phonemod.api.SyncService() {
        public void publish(String channel, String payload) {
            if (channel.isBlank() || channel.length() > 128 || payload.length() > 8192)
                throw new IllegalArgumentException("Invalid sync publication");
            stringChannels.put(channel, payload);
        }
        public void subscribe(String channel) {
            if (channel.isBlank() || channel.length() > 128)
                throw new IllegalArgumentException("Invalid sync channel");
            stringChannels.putIfAbsent(channel, "");
        }
    };

    public RuntimeDevicePlatform() {
        if (instance != null) throw new IllegalStateException("Runtime platform already exists");
        instance = this;
        DevicePlatform.install(this);
    }

    public static RuntimeDevicePlatform get() {
        if (instance == null) throw new IllegalStateException("Runtime platform is not installed");
        return instance;
    }

    public void load() { identities.load(); }
    public void shutdown() {
        identities.flush();
        DevicePlatform.uninstall(this);
        instance = null;
    }
    public IdentityRegistry identities() { return identities; }
    public RuntimeAppService apps() { return apps; }
    public esq.phonemod.api.DeviceService devices() { return devices; }
    public PresenceService presence() { return presence; }
    public MessagingService messaging() { return messaging; }
    public CallingService calling() { return calling; }
    public NotificationService notifications() { return notifications; }
    public esq.phonemod.api.SyncService sync() { return sync; }
}
