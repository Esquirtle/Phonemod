package esq.phonemod.phone.animation;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.phone.messaging.CallRegistry;
import esq.phonemod.phone.messaging.PhoneRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drives the held-phone body animations registered in {@code Server/Item/Animations/Phone.json}.
 *
 * <p>Nothing else in the plugin plays these: the {@code UsePhone} animation had been registered
 * since the original asset drop but no call site ever existed, so it never fired. This class is
 * that call site, for {@code UsePhone} and for the {@code PhoneCall} animation added alongside it.
 *
 * <h2>Why the state is derived rather than tracked</h2>
 * Both animations compete for the single {@link AnimationSlot#Action} slot on the player, so a
 * pair of independent play/stop calls would race: hanging up while the phone UI is still open
 * would stop the animation outright instead of falling back to {@code UsePhone}. Instead of
 * mirroring "is browsing" / "is on a call" flags here — a second copy of state that can drift
 * from the registries that own it — {@link #desiredAnimation} recomputes the answer from
 * {@link PhoneRegistry} and {@link CallRegistry} on every transition. Those two are the single
 * source of truth; this class only remembers what it last sent, so it can skip redundant packets.
 *
 * <p>Priority is {@code PhoneCall} over {@code UsePhone}: being on a call is the more specific
 * state, and a player on a call usually has the UI open too.
 *
 * <h2>Threading</h2>
 * Callers reach this class from the world thread (page callbacks) and from off it (the call
 * timeout watcher on {@code HytaleServer.SCHEDULED_EXECUTOR}, and Netty threads). Every variant
 * that is not already given a live {@link Store} hops through {@code World.execute} before
 * touching components.
 */
public final class PhoneAnimationController {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * The {@code ItemPlayerAnimations} asset id these animations live in — the value of
     * {@code PlayerAnimationsId} in {@code Server/Item/Items/Phone.json}. Passed as the
     * {@code itemAnimationsId} argument of
     * {@code AnimationUtils.playAnimation} (AnimationUtils.java:53-86), which is what tells the
     * client to resolve {@code animationId} against the Phone set rather than the default one.
     */
    private static final String ITEM_ANIMATIONS_ID = "Phone";

    /** Animation ids, as keyed in the {@code Animations} block of the Phone animation set. */
    private static final String ANIM_ON_CALL = "PhoneCall";
    private static final String ANIM_USING_PHONE = "UsePhone";

    /** Player UUID → the animation id last sent, so unchanged states send no packet. */
    private static final ConcurrentHashMap<UUID, String> playing = new ConcurrentHashMap<>();

    private PhoneAnimationController() {}

    // ── Transition hooks ──────────────────────────────────────────────────────

    /**
     * Re-evaluates the animation for the player owning {@code phoneNumber}. Safe to call from any
     * thread and when the number is unknown; resolves the player through
     * {@link PhoneRegistry#getPresenceEntry} and hops to their world thread.
     */
    public static void refresh(@Nonnull String phoneNumber) {
        PhoneRegistry.PresenceEntry entry = PhoneRegistry.getPresenceEntry(phoneNumber);
        if (entry == null) return;
        UUID uuid = entry.playerRef().getUuid();
        String desired = desiredAnimation(uuid);
        entry.world().execute(() -> apply(uuid, entry.ref(), entry.store(), desired));
    }

    /**
     * Drops the remembered animation for a departing player. The entity is going away, so no
     * stop packet is sent — this only prevents the map from growing without bound.
     */
    public static void forget(@Nonnull UUID uuid) {
        playing.remove(uuid);
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    /**
     * Returns the animation that should be showing for {@code uuid}, or {@code null} for none.
     *
     * <p>A player may own more than one phone ({@code PhoneRegistry.getPhoneNumbersByUuid}), so
     * every number they own is considered: any one of them being on a call, or having its UI
     * open, is enough.
     */
    @Nullable
    private static String desiredAnimation(@Nonnull UUID uuid) {
        boolean onCall = false;
        boolean usingPhone = false;
        for (String phoneNumber : PhoneRegistry.getPhoneNumbersByUuid(uuid)) {
            if (CallRegistry.isInCall(phoneNumber)) onCall = true;
            if (PhoneRegistry.getOnlineEntry(phoneNumber) != null) usingPhone = true;
        }
        if (onCall) return ANIM_ON_CALL;
        if (usingPhone) return ANIM_USING_PHONE;
        return null;
    }

    /** Sends the play/stop packet, but only when the state actually changed. World thread only. */
    private static void apply(@Nonnull UUID uuid,
                              @Nonnull Ref<EntityStore> ref,
                              @Nonnull Store<EntityStore> store,
                              @Nullable String desired) {
        if (Objects.equals(playing.get(uuid), desired)) return;

        // The player can disconnect between the transition and this hop. AnimationUtils only
        // asserts on a missing NetworkId (AnimationUtils.java:74, :148), which is a no-op with
        // assertions off and then NPEs on getId(), so check it here instead.
        if (store.getComponent(ref, NetworkId.getComponentType()) == null) {
            playing.remove(uuid);
            return;
        }

        try {
            if (desired == null) {
                AnimationUtils.stopAnimation(ref, AnimationSlot.Action, true, store);
                playing.remove(uuid);
            } else {
                AnimationUtils.playAnimation(
                        ref, AnimationSlot.Action, ITEM_ANIMATIONS_ID, desired, true, store);
                playing.put(uuid, desired);
            }
        } catch (RuntimeException error) {
            // An animation is cosmetic; never let it take down a call teardown or a page dismiss.
            LOGGER.atWarning().withCause(error).log(
                    "[PhoneAnimationController] Failed to apply animation %s", desired);
            playing.remove(uuid);
        }
    }
}
