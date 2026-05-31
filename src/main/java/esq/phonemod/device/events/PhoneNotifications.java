package esq.phonemod.device.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import esq.phonemod.phone.api.PhoneAppContext;

import javax.annotation.Nonnull;

/**
 * Convenience helpers for sending {@link PhoneNotification} toasts.
 *
 * <p>These methods wrap {@link NotificationUtil} and optional sound playback
 * into a single call. Call them from the world thread when sound playback is
 * requested; calling from any other thread is safe for the notification toast
 * but will skip sound.
 *
 * <p>Usage:
 * <pre>{@code
 * PhoneNotifications.send(ctx.getPlayerRef(),
 *         PhoneNotification.info(
 *                 Message.translation("myapp.alert_title"),
 *                 Message.raw(alertBody)));
 * }</pre>
 */
public final class PhoneNotifications {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private PhoneNotifications() {
    }

    /**
     * Sends {@code notification} to the player identified by {@code playerRef}.
     * If the notification has a {@link PhoneNotification#getSoundId()}, it is
     * played as a UI sound event. Sound playback requires valid
     * {@code ref}/{@code store} context and will silently no-op if the sound
     * index cannot be resolved.
     *
     * @param playerRef the player to notify
     * @param notification  the notification to send
     */
    public static void send(@Nonnull PlayerRef playerRef,
            @Nonnull PhoneNotification notification) {
        NotificationUtil.sendNotification(
                playerRef.getPacketHandler(),
                notification.getTitle(),
                notification.getBody(),
                notification.getStyle());
    }

    /**
     * Sends {@code notification} to the player identified by {@code ctx}.
     * Also plays any associated sound via the context's ref/store.
     */
    public static void send(@Nonnull PhoneAppContext ctx,
            @Nonnull PhoneNotification notification) {
        send(ctx.getPlayerRef(), ctx.getRef(), ctx.getStore(), notification);
    }

    /**
     * Sends {@code notification} as a toast and, if it carries a sound id, plays it as a
     * UI sound for the given entity. Must run on the entity's world thread when a sound is
     * requested; the toast alone is safe from any thread.
     */
    public static void send(@Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PhoneNotification notification) {
        NotificationUtil.sendNotification(
                playerRef.getPacketHandler(),
                notification.getTitle(),
                notification.getBody(),
                notification.getStyle());

        String soundId = notification.getSoundId();
        if (soundId != null && !soundId.isBlank()) {
            int index = SoundEvent.getAssetMap().getIndex(soundId);
            if (index != 0) {
                try {
                    SoundUtil.playSoundEvent2d(ref, index, SoundCategory.UI, store);
                } catch (Exception e) {
                    LOGGER.atWarning().withCause(e).log(
                            "[PhoneNotifications] Failed to play sound %s", soundId);
                }
            }
        }
    }
}
