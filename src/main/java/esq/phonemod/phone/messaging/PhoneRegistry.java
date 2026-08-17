package esq.phonemod.phone.messaging;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.device.api.DevicePageHandle;
import esq.phonemod.device.components.DeviceSettings;
import esq.phonemod.device.components.DeviceSettingsComponent;
import esq.phonemod.device.events.PhoneNotification;
import esq.phonemod.device.events.PhoneNotifications;
import esq.phonemod.device.events.PhoneEvents;
import esq.phonemod.device.events.PhoneMessageReceivedEvent;
import esq.phonemod.device.events.PhoneMessageSentEvent;
import esq.phonemod.phone.components.ConversationHistoryComponent;
import esq.phonemod.PhoneMod;
import esq.phonemod.config.PhonemodConfig;
import esq.phonemod.security.TokenBucket;
import com.hypixel.hytale.server.core.HytaleServer;
import esq.phonemod.api.event.MessageEvent;
import esq.phonemod.operations.OperationalMetrics;

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

    /**
     * Sound event ID for an incoming message. Resolved to an index lazily at call time
     * (see {@link #deliver}) because asset indices are not guaranteed to be loaded when
     * this class is first referenced during plugin {@code setup()}.
     */
    private static final String MESSAGE_RECEIVED_SOUND_ID = "Notification_Message_Received";
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** phone number → session entry for players whose phone UI is currently OPEN. */
    private static final ConcurrentHashMap<String, OnlineEntry> online = new ConcurrentHashMap<>();

    /**
     * phone number → last-known session entry for players who are CONNECTED and own this
     * phone, whether or not the phone UI is currently open. Lets messages/notifications
     * reach a player whose phone is closed (toast + persisted history) rather than being
     * deferred to the offline queue. Cleared on disconnect.
     */
    private static final ConcurrentHashMap<String, PresenceEntry> presence = new ConcurrentHashMap<>();
    /** Immutable, network-thread-safe preferences refreshed on open and setting changes. */
    private static final ConcurrentHashMap<String, PreferenceSnapshot> preferences =
            new ConcurrentHashMap<>();

    private static final OfflineMailboxStore mailboxes = new OfflineMailboxStore();
    private static final ConcurrentHashMap<String, TokenBucket> messageLimiters = new ConcurrentHashMap<>();

    private PhoneRegistry() {}

    public static void initMailboxes() { mailboxes.init(); }
    public static void flushMailboxes() { mailboxes.flush(); }

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
        PresenceEntry presenceEntry = new PresenceEntry(ref, playerRef, store, world);
        OnlineEntry entry = new OnlineEntry(ref, playerRef, store, world, page);
        online.put(phoneNumber, entry);
        presence.put(phoneNumber, presenceEntry);
        DeviceSettingsComponent settings = store.getComponent(
                ref, DeviceSettingsComponent.getComponentType());
        updatePreferences(phoneNumber, settings != null ? settings.get(phoneNumber) : null);
        LOGGER.atFine().log("[PhoneRegistry] Registered phone %s", phoneNumber);

        // Drain any messages that arrived while this number was offline into the
        // recipient's persistent conversation history, so they appear in the chat
        // thread (not just the conversation list) the next time the phone opens.
        List<OfflineMailboxStore.OfflineMessage> pending = mailboxes.drain(phoneNumber);
        if (pending != null && !pending.isEmpty()) {
            final List<OfflineMailboxStore.OfflineMessage> snapshot = List.copyOf(pending);
            world.execute(() -> {
                long cutoff = offlineCutoffMillis();
                int delivered = 0;
                for (OfflineMailboxStore.OfflineMessage queued : snapshot) {
                    if (queued.queuedAtMillis() < cutoff) continue;
                    TextMessage tm = queued.message();
                    persistMessage(store, ref, phoneNumber, tm.fromNumber(),
                            new ChatMessage(false, tm.fromName(), tm.body()));
                    delivered++;
                }
                LOGGER.atFine().log("[PhoneRegistry] Delivered %d queued message(s) to %s",
                        delivered, phoneNumber);
            });
        }
    }

    /**
     * Removes the OPEN-UI entry for {@code phoneNumber}, but only if it still refers to the
     * given page instance. Called from {@code DevicePage.onDismiss} when the phone UI closes.
     * Presence is intentionally retained so the player keeps receiving toasts while connected.
     * The page-instance guard avoids a race where a freshly re-opened page is wiped by the
     * dismiss of the page it replaced.
     */
    public static void unregisterPage(@Nonnull String phoneNumber, @Nonnull DevicePageHandle page) {
        online.computeIfPresent(phoneNumber, (k, e) -> {
            if (e.page() == page) {
                LOGGER.atFine().log("[PhoneRegistry] Phone %s UI closed", phoneNumber);
                return null;
            }
            return e;
        });
    }

    /**
     * Removes all entries (open + presence) whose {@link Ref} matches the given ref.
     * Called when a player disconnects — the phone number is not known at that point.
     */
    public static void unregisterByRef(@Nonnull Ref<EntityStore> ref) {
        online.values().removeIf(e -> e.ref().equals(ref));
        presence.entrySet().removeIf(entry -> {
            if (entry.getValue().ref().equals(ref)) {
                preferences.remove(entry.getKey());
                LOGGER.atFine().log("[PhoneRegistry] Unregistered phone %s (disconnect)", entry.getKey());
                return true;
            }
            return false;
        });
    }

    public static boolean isAuthorizedPage(@Nonnull String phoneNumber,
                                           @Nonnull Ref<EntityStore> ref,
                                           @Nonnull DevicePageHandle page) {
        OnlineEntry entry = online.get(phoneNumber);
        return entry != null && entry.ref().equals(ref) && entry.page() == page;
    }

    /** Publishes a fresh immutable preference snapshot after a world-thread mutation. */
    public static void updatePreferences(@Nonnull String phoneNumber,
            @Nullable DeviceSettings settings) {
        preferences.put(phoneNumber, PreferenceSnapshot.from(settings));
    }

    @Nonnull
    public static PreferenceSnapshot getPreferences(@Nonnull String phoneNumber) {
        return preferences.getOrDefault(phoneNumber, PreferenceSnapshot.DEFAULT);
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
    public static boolean deliver(@Nonnull String toNumber, @Nonnull TextMessage message) {
        PhonemodConfig config = PhoneMod.config();
        if (toNumber.isBlank() || toNumber.length() > 64
                || message.fromNumber().isBlank() || message.fromNumber().length() > 64
                || message.fromName().isBlank() || message.fromName().length() > config.maxContactNameCharacters()
                || message.body().isBlank() || message.body().length() > config.maxMessageCharacters()) {
            LOGGER.atWarning().log("[PhoneRegistry] Rejected invalid message envelope");
            return false;
        }
        TokenBucket limiter = messageLimiters.computeIfAbsent(message.fromNumber(), ignored ->
                new TokenBucket(config.messagesPerSecond(), config.messageBurst()));
        if (!limiter.tryAcquire()) {
            OperationalMetrics.get().rateLimited();
            LOGGER.atWarning().log("[PhoneRegistry] Message rate limit exceeded sender=%s",
                    message.fromNumber());
            return false;
        }
        MessageEvent.Pre pre = HytaleServer.get().getEventBus().dispatchFor(MessageEvent.Pre.class)
                .dispatch(new MessageEvent.Pre(message.fromNumber(), toNumber, message.body().length()));
        if (pre.isCancelled()) return false;

        if (!config.offlineMessagingEnabled()
                && online.get(toNumber) == null && presence.get(toNumber) == null) {
            LOGGER.atFine().log("[PhoneRegistry] recipient %s offline; offline messaging disabled",
                    toNumber);
            return false;
        }
        LOGGER.atFine().log("[PhoneRegistry] deliver to=%s from=%s", toNumber, message.fromNumber());
        PhoneEvents.post(new PhoneMessageSentEvent(message.fromNumber(), toNumber, message.body()));
        PhoneEvents.post(new PhoneMessageReceivedEvent(
                toNumber, message.fromNumber(), message.fromName(), message.body()));
        HytaleServer.get().getEventBus().dispatchFor(MessageEvent.Post.class)
                .dispatch(new MessageEvent.Post(message.fromNumber(), toNumber, message.body().length()));

        OnlineEntry openEntry = online.get(toNumber);
        if (openEntry != null) {
            // Phone UI open: persist, toast (+sound), and live-push the chat view.
            deliverToLoaded(openEntry, toNumber, message, true);
            return true;
        }

        PresenceEntry presentEntry = presence.get(toNumber);
        if (presentEntry != null) {
            // Connected but phone closed: persist and toast (+sound); no live push.
            // The message is in history, so it shows when they next open the phone.
            deliverToLoaded(presentEntry, toNumber, message, false);
            return true;
        }

        // Fully offline: queue for delivery when the owner next connects/opens their phone
        // (drained into persistent history by register()).
        mailboxes.enqueue(toNumber, message, System.currentTimeMillis());
        LOGGER.atFine().log("[PhoneRegistry] recipient %s offline; queued message", toNumber);
        return true;
    }

    /**
     * Persists {@code message} to the recipient's history and shows a toast (+sound), running
     * on the recipient's world thread. When {@code livePush} is true and the phone UI is open,
     * also pushes the incoming message into the live chat view.
     */
    private static void deliverToLoaded(@Nonnull PresenceEntry entry,
                                        @Nonnull String toNumber,
                                        @Nonnull TextMessage message,
                                        boolean livePush) {
        final ChatMessage receivedMsg = new ChatMessage(false, message.fromName(), message.body());
        entry.world().execute(() -> {
            persistMessage(entry.store(), entry.ref(), toNumber, message.fromNumber(), receivedMsg);
            PreferenceSnapshot preference = getPreferences(toNumber);
            if (preference.notificationsEnabled()) {
                try {
                    Message body = preference.messagePreviewEnabled()
                            ? Message.raw(message.body())
                            : Message.raw("New message");
                    PhoneNotification notification = PhoneNotification
                            .info(Message.raw(message.fromName()).color("#43D69A"), body);
                    if (preference.soundEnabled()) {
                        notification = notification.withSound(MESSAGE_RECEIVED_SOUND_ID);
                    }
                    PhoneNotifications.send(
                            entry.playerRef(), entry.ref(), entry.store(), notification);
                } catch (Exception e) {
                    LOGGER.atWarning().withCause(e).log(
                            "[PhoneRegistry] Failed to notify %s", toNumber);
                }
            }
            if (livePush) {
                OnlineEntry current = online.get(toNumber);
                if (current != null && current.ref().equals(entry.ref())) {
                    current.page().onIncomingMessage(message.fromNumber());
                }
            }
        });
    }

    private static void deliverToLoaded(@Nonnull OnlineEntry entry,
                                        @Nonnull String toNumber,
                                        @Nonnull TextMessage message,
                                        boolean livePush) {
        deliverToLoaded(new PresenceEntry(entry.ref(), entry.playerRef(), entry.store(), entry.world()),
                toNumber, message, livePush);
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
     * Returns an unmodifiable view of messages queued for {@code phoneNumber} while it was
     * offline and not yet drained into persistent history. For an online phone this is
     * normally empty (messages go straight to {@link ConversationHistoryComponent}); the
     * Whatgram conversation list enumerates contacts from history and merges this as a
     * fallback.
     */
    @Nonnull
    public static List<TextMessage> getInbox(@Nonnull String phoneNumber) {
        return mailboxes.peek(phoneNumber);
    }

    // ── Lookup ────────────────────────────────────────────────────────────────

    @Nullable
    public static OnlineEntry getOnlineEntry(@Nonnull String phoneNumber) {
        return online.get(phoneNumber);
    }

    @Nullable
    public static PresenceEntry getPresenceEntry(@Nonnull String phoneNumber) {
        return presence.get(phoneNumber);
    }

    /** Returns a read-only snapshot of all currently registered phone sessions. */
    @Nonnull
    public static Collection<OnlineEntry> getOnlineEntries() {
        return Collections.unmodifiableCollection(online.values());
    }

    public static int activePresenceCount() { return presence.size(); }

    public static int mailboxDepth() { return mailboxes.depth(); }

    /**
     * Returns all phone numbers currently registered to the given player UUID.
     * A player can have multiple phones open simultaneously, one per physical phone item.
     */
    @Nonnull
    public static List<String> getPhoneNumbersByUuid(@Nonnull java.util.UUID uuid) {
        List<String> result = new ArrayList<>();
        for (java.util.Map.Entry<String, PresenceEntry> entry : presence.entrySet()) {
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
        for (java.util.Map.Entry<String, PresenceEntry> entry : presence.entrySet()) {
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

    public record PresenceEntry(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull World world
    ) {}

    public record PreferenceSnapshot(
            boolean notificationsEnabled,
            boolean messagePreviewEnabled,
            boolean incomingCallsEnabled,
            boolean soundEnabled
    ) {
        private static final PreferenceSnapshot DEFAULT =
                new PreferenceSnapshot(true, true, true, true);

        @Nonnull
        static PreferenceSnapshot from(@Nullable DeviceSettings settings) {
            if (settings == null) return DEFAULT;
            return new PreferenceSnapshot(
                    settings.isNotificationsEnabled(),
                    settings.isMessagePreviewEnabled(),
                    settings.isIncomingCallsEnabled(),
                    !"Silent".equals(settings.getSoundProfileId()));
        }
    }

    private static long offlineCutoffMillis() {
        long days = PhoneMod.config().offlineMessageRetentionDays();
        return System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(days);
    }
}
