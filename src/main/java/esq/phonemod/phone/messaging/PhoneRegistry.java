package esq.phonemod.phone.messaging;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import esq.phonemod.device.api.DevicePageHandle;
import esq.phonemod.phone.components.ConversationHistoryComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for active phone sessions and in-memory message inboxes.
 *
 * <ul>
 *   <li>{@link #register} is called by the phone interaction when a player opens their phone.</li>
 *   <li>{@link #unregisterByRef} is called by {@link PhoneEventHandler} on player disconnect.</li>
 *   <li>{@link #deliver} persists to both parties' {@link ConversationHistoryComponent},
 *       sends a toast, and live-pushes the chat UI if the recipient has the conversation open.</li>
 * </ul>
 */
public final class PhoneRegistry {

    private static final int MESSAGE_RECEIVED_SOUND = SoundEvent.getAssetMap()
            .getIndex("Notification_Message_Received");
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** phone number → session entry for players whose phone is currently open. */
    private static final ConcurrentHashMap<String, OnlineEntry> online = new ConcurrentHashMap<>();

    /** phone number → received messages this session (for inbox conversation list). */
    private static final ConcurrentHashMap<String, List<TextMessage>> inboxes = new ConcurrentHashMap<>();

    private PhoneRegistry() {}

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Registers a phone session. Must be called before {@code openCustomPage} so the
     * page instance is available for live push updates.
     */
    public static void register(@Nonnull String phoneNumber,
                                @Nonnull Ref<EntityStore> ref,
                                @Nonnull PlayerRef playerRef,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull World world,
                                @Nonnull DevicePageHandle page) {
        online.put(phoneNumber, new OnlineEntry(ref, playerRef, store, world, page));
        LOGGER.atInfo().log("[PhoneRegistry] Registered phone %s", phoneNumber);
    }

    /**
     * Removes the online entry whose {@link Ref} matches the given ref.
     * Called when a player disconnects — the phone number is not known at that point.
     */
    public static void unregisterByRef(@Nonnull Ref<EntityStore> ref) {
        online.entrySet().removeIf(entry -> {
            if (entry.getValue().ref().equals(ref)) {
                LOGGER.atInfo().log("[PhoneRegistry] Unregistered phone %s (disconnect)", entry.getKey());
                return true;
            }
            return false;
        });
    }

    // ── Delivery ──────────────────────────────────────────────────────────────

    /**
     * Delivers a message to {@code toNumber}:
     * <ol>
     *   <li>Eagerly persists the received copy to the recipient's component.</li>
     *   <li>Eagerly persists the sent copy to the sender's component.</li>
     *   <li>Sends a toast notification to the recipient.</li>
     *   <li>Live-pushes the chat UI if the recipient has that conversation open.</li>
     * </ol>
     */
    public static void deliver(@Nonnull String toNumber, @Nonnull TextMessage message) {
        LOGGER.atInfo().log("[PhoneRegistry] deliver to=%s from=%s body=%s", toNumber, message.fromNumber(), message.body());
        inboxes.computeIfAbsent(toNumber, k -> Collections.synchronizedList(new ArrayList<>()))
               .add(message);

        OnlineEntry recipientEntry = online.get(toNumber);
        if (recipientEntry != null) {
            final ChatMessage receivedMsg = new ChatMessage(false, message.fromName(), message.body());
            final OnlineEntry re = recipientEntry;
            re.world().execute(() -> {
                persistMessage(re.store(), re.ref(), toNumber, message.fromNumber(), receivedMsg);
                try {
                    NotificationUtil.sendNotification(
                            re.playerRef().getPacketHandler(),
                            Message.raw(message.fromName()).color("#43D69A"),
                            Message.raw(message.body())
                    );
                    if (MESSAGE_RECEIVED_SOUND != 0) {
                        SoundUtil.playSoundEvent2d(
                                re.ref(),
                                MESSAGE_RECEIVED_SOUND,
                                SoundCategory.UI,
                                re.store());
                    }
                } catch (Exception e) {
                    LOGGER.atWarning().withCause(e).log("[PhoneRegistry] Failed to notify %s", toNumber);
                }
                LOGGER.atInfo().log("[PhoneRegistry] push incoming message to page for %s from=%s", toNumber, message.fromNumber());
                re.page().onIncomingMessage(message.fromNumber());
            });
        } else {
            LOGGER.atInfo().log("[PhoneRegistry] recipient %s is offline or not registered", toNumber);
        }
    }

    // ── Persistence helpers ───────────────────────────────────────────────────

    private static void persistMessage(@Nonnull Store<EntityStore> store,
                                        @Nonnull Ref<EntityStore> ref,
                                        @Nonnull String ownNumber,
                                        @Nonnull String contactNumber,
                                        @Nonnull ChatMessage message) {
        ConversationHistoryComponent component =
                store.ensureAndGetComponent(ref, ConversationHistoryComponent.getComponentType());
        component.addMessage(ownNumber, contactNumber, message);
        store.putComponent(ref, ConversationHistoryComponent.getComponentType(), component);
    }

    // ── Inbox ─────────────────────────────────────────────────────────────────

    /**
     * Returns an unmodifiable view of received messages this session.
     * Used by the Whatgram conversation list to enumerate contacts.
     */
    @Nonnull
    public static List<TextMessage> getInbox(@Nonnull String phoneNumber) {
        List<TextMessage> inbox = inboxes.get(phoneNumber);
        return inbox != null ? Collections.unmodifiableList(inbox) : Collections.emptyList();
    }

    // ── Lookup ────────────────────────────────────────────────────────────────

    @Nullable
    public static OnlineEntry getOnlineEntry(@Nonnull String phoneNumber) {
        return online.get(phoneNumber);
    }

    /** Returns a read-only snapshot of all currently registered phone sessions. */
    @Nonnull
    public static Collection<OnlineEntry> getOnlineEntries() {
        return Collections.unmodifiableCollection(online.values());
    }

    /**
     * Returns all phone numbers currently registered to the given player UUID.
     * A player can have multiple phones open simultaneously, one per physical phone item.
     */
    @Nonnull
    public static List<String> getPhoneNumbersByUuid(@Nonnull java.util.UUID uuid) {
        List<String> result = new ArrayList<>();
        for (java.util.Map.Entry<String, OnlineEntry> entry : online.entrySet()) {
            if (entry.getValue().playerRef().getUuid().equals(uuid)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Returns all phone numbers currently registered to the given entity ref.
     * Used during disconnect cleanup to find all sessions for a departing player.
     */
    @Nonnull
    public static List<String> getPhoneNumbersByRef(@Nonnull Ref<EntityStore> ref) {
        List<String> result = new ArrayList<>();
        for (java.util.Map.Entry<String, OnlineEntry> entry : online.entrySet()) {
            if (entry.getValue().ref().equals(ref)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    // ── Types ─────────────────────────────────────────────────────────────────

    public record OnlineEntry(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull World world,
            @Nonnull DevicePageHandle page
    ) {}
}
