package esq.phonemod.setup;

import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import esq.phonemod.PhoneMod;
import esq.phonemod.phone.messaging.CallRegistry;
import esq.phonemod.phone.messaging.PhoneEventHandler;

public class EventRegistryManager {
    private final PhoneMod plugin;

    public EventRegistryManager(PhoneMod plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, new PhoneEventHandler());
        // Intercept VoiceData packets globally to route audio privately during phone calls.
        PacketAdapters.registerInbound((PlayerPacketFilter) CallRegistry.VOICE_FILTER);
    }
}
