package esq.phonemod.phone.messaging;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.phone.animation.PhoneAnimationController;
import esq.phonemod.phone.sync.SyncService;
import esq.phonemod.phone.sync.room.RoomService;

import java.util.function.Consumer;

/**
 * Cleans up {@link PhoneRegistry} entries when a player disconnects.
 *
 * <p>Register in {@code EventRegistryManager.register()} via:
 * <pre>{@code
 * plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, new PhoneEventHandler());
 * }</pre>
 */
public final class PhoneEventHandler implements Consumer<PlayerDisconnectEvent> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public void accept(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        if (playerRef == null) {
            return;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) {
            return;
        }

        // Clean up any active/pending calls and sync subscriptions/rooms for all
        // phones owned by this player before unregistering.
        for (String phoneNumber : PhoneRegistry.getPhoneNumbersByRef(ref)) {
            CallRegistry.cleanupOnDisconnect(phoneNumber);
            SyncService.unsubscribePhone(phoneNumber);
            RoomService.leaveAll(phoneNumber);
        }
        PhoneRegistry.unregisterByRef(ref);
        // The entity is going away, so there is nothing to send a stop packet to — just drop
        // the remembered pose so the tracking map does not grow across reconnects.
        PhoneAnimationController.forget(playerRef.getUuid());
    }
}
