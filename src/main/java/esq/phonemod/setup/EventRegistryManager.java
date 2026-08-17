package esq.phonemod.setup;

import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import esq.phonemod.device.assets.DeviceAsset;
import esq.phonemod.device.core.DeviceService;
import esq.phonemod.PhoneMod;
import esq.phonemod.phone.messaging.CallRegistry;
import esq.phonemod.phone.messaging.PhoneEventHandler;

public class EventRegistryManager {
    private final PhoneMod plugin;
    private PacketFilter voiceFilterHandle;

    public EventRegistryManager(PhoneMod plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, new PhoneEventHandler());
        // Intercept VoiceData packets globally to route audio privately during phone calls.
        voiceFilterHandle = PacketAdapters.registerInbound((PlayerPacketFilter) CallRegistry.VOICE_FILTER);
        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class, DeviceAsset.class, EventRegistryManager::onDeviceAssetsLoaded);
    }

    private static void onDeviceAssetsLoaded(
            LoadedAssetsEvent<String, DeviceAsset, DefaultAssetMap<String, DeviceAsset>> event) {
        DeviceService.get().invalidateShells();
    }

    public void shutdown() {
        PacketFilter handle = voiceFilterHandle;
        voiceFilterHandle = null;
        if (handle != null) {
            PacketAdapters.deregisterInbound(handle);
        }
    }
}
