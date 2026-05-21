package esq.phonemod.phone.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.phone.components.ConversationHistoryComponent;
import esq.phonemod.phone.components.PhoneOwnerComponent;
import esq.phonemod.phone.messaging.CallRegistry;
import esq.phonemod.phone.messaging.ChatMessage;
import esq.phonemod.phone.messaging.PhoneRegistry;
import esq.phonemod.phone.messaging.TextMessage;

import javax.annotation.Nonnull;

/**
 * The phone UI page — a full-screen interactive page accessible to the player.
 * Extend {@link #buildPage} to populate tabs and bind actions.
 */
/**
 * The phone UI page — a full-screen interactive page accessible to the player.
 * Add tabs and additional bindings by extending {@code build()} after calling
 * super,
 * or by injecting content into {@code #Content} via {@code sendUpdate()}.
 */
public final class PhonePage extends InteractiveCustomUIPage<PhonePage.PhoneEventData> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String PHONE_UI = "Pages/Phone/Phone.ui";
    private static final String APPMENU_UI = "Pages/Phone/AppMenu.ui";
    private static final String PHONEBOXSELECTOR = "#AppContent";
    private PhoneStatesEnum currentState = PhoneStatesEnum.HOME;
    private String currentChatContact = null;
    private final String phoneNumber;
    // Cached from build()/handleDataEvent() for use in onIncomingMessage()
    private Ref<EntityStore> cachedRef = null;
    private Store<EntityStore> cachedStore = null;

    public PhonePage(@Nonnull PlayerRef playerRef, @Nonnull String phoneNumber) {
        super(playerRef, CustomPageLifetime.CanDismiss, PhoneEventData.CODEC);
        this.phoneNumber = phoneNumber;
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evb,
            @Nonnull Store<EntityStore> store) {
        this.cachedRef = ref;
        this.cachedStore = store;
        cmd.append(PHONE_UI);

        // Permanent bottom-bar binding — always present regardless of state
        evb.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#HomeButton",
                EventData.of("Action", "home"),
                false);

        // Restore call state if the player had a call active when they closed the phone
        String activePartner = CallRegistry.getActivePartner(phoneNumber);
        String pendingCallee = CallRegistry.getPendingCallee(phoneNumber);
        String pendingCaller = CallRegistry.getPendingCaller(phoneNumber);
        if (activePartner != null) {
            currentState = PhoneStatesEnum.ACTIVE_CALL;
            Calls.loadActiveCallState(activePartner, activePartner, cmd, evb);
        } else if (pendingCallee != null) {
            currentState = PhoneStatesEnum.ACTIVE_CALL;
            Calls.loadActiveCallState(pendingCallee, "Calling...", cmd, evb);
        } else if (pendingCaller != null) {
            currentState = PhoneStatesEnum.INCOMING_CALL;
            Calls.loadIncomingCallState(pendingCaller, pendingCaller, cmd, evb);
        } else {
            loadHomeState(cmd, evb);
        }
    }

    // ── Event handling ────────────────────────────────────────────────────────

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PhoneEventData data) {
        this.cachedRef = ref;
        this.cachedStore = store;
        if ("home".equals(data.action)) {
            currentState = PhoneStatesEnum.HOME;
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder evb = new UIEventBuilder();
            loadHomeState(cmd, evb);
            sendUpdate(cmd, evb, false);
            return;
        }

        if ("open_app".equals(data.action)) {
            openApp(ref, store, data.app);
            return;
        }

        if ("open_chat".equals(data.action) && data.contact != null) {
            currentState = PhoneStatesEnum.CHAT;
            currentChatContact = data.contact;
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder evb = new UIEventBuilder();
            Whatgram.loadChat(phoneNumber, data.contact, store, ref, cmd, evb);
            sendUpdate(cmd, evb, false);
            return;
        }

        if ("send_message".equals(data.action) && currentChatContact != null) {
            String body = data.messageValue;
            if (body == null || body.isBlank()) {
                sendUpdate(null, false);
                return;
            }
            String trimmedBody = body.trim();
            // Persist the sent message directly — we are already on the world thread.
            ConversationHistoryComponent history = store.ensureAndGetComponent(
                    ref, ConversationHistoryComponent.getComponentType());
            history.addMessage(phoneNumber, currentChatContact, new ChatMessage(true, phoneNumber, trimmedBody));
            store.putComponent(ref, ConversationHistoryComponent.getComponentType(), history);
            // Deliver to recipient (handles their persist + toast + live push).
            PhoneRegistry.deliver(currentChatContact,
                    new TextMessage(phoneNumber, phoneNumber, trimmedBody));
            // Re-render chat to show the sent message.
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder evb = new UIEventBuilder();
            Whatgram.loadChat(phoneNumber, currentChatContact, store, ref, cmd, evb);
            sendUpdate(cmd, evb, false);
            return;
        }

        if ("open_add_contact".equals(data.action)) {
            currentState = PhoneStatesEnum.CONTACT_ADD;
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder evb = new UIEventBuilder();
            Contacts.loadAddContactState(cmd, evb);
            sendUpdate(cmd, evb, false);
            return;
        }

        if ("contacts".equals(data.action)) {
            currentState = PhoneStatesEnum.CONTACTS;
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder evb = new UIEventBuilder();
            Contacts.loadContactsState(phoneNumber, store, ref, cmd, evb);
            sendUpdate(cmd, evb, false);
            return;
        }

        if ("save_contact".equals(data.action)) {
            String number = data.contactFormNumber;
            String name   = data.contactFormName;
            if (number != null && !number.isBlank() && name != null && !name.isBlank()) {
                PhoneOwnerComponent owner =
                        store.ensureAndGetComponent(ref, PhoneOwnerComponent.getComponentType());
                owner.addContact(phoneNumber, number.trim(), name.trim());
                store.putComponent(ref, PhoneOwnerComponent.getComponentType(), owner);
            }
            currentState = PhoneStatesEnum.CONTACTS;
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder evb = new UIEventBuilder();
            Contacts.loadContactsState(phoneNumber, store, ref, cmd, evb);
            sendUpdate(cmd, evb, false);
            return;
        }

        if ("remove_contact".equals(data.action) && data.contact != null) {
            PhoneOwnerComponent owner =
                    store.ensureAndGetComponent(ref, PhoneOwnerComponent.getComponentType());
            owner.removeContact(phoneNumber, data.contact);
            store.putComponent(ref, PhoneOwnerComponent.getComponentType(), owner);
            currentState = PhoneStatesEnum.CONTACTS;
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder evb = new UIEventBuilder();
            Contacts.loadContactsState(phoneNumber, store, ref, cmd, evb);
            sendUpdate(cmd, evb, false);
            return;
        }

        if ("start_call".equals(data.action)) {
            String target = data.contact != null ? data.contact : data.dialNumber;
            if (target != null && !target.isBlank()) {
                String trimmed = target.trim();
                CallRegistry.initiateCall(phoneNumber, trimmed, trimmed);
                // Immediately show calling screen so player can't fire start_call again
                currentState = PhoneStatesEnum.ACTIVE_CALL;
                UICommandBuilder cmd = new UICommandBuilder();
                UIEventBuilder evb = new UIEventBuilder();
                Calls.loadActiveCallState(trimmed, "Calling...", cmd, evb);
                sendUpdate(cmd, evb, false);
            } else {
                sendUpdate(null, false);
            }
            return;
        }

        if ("answer_call".equals(data.action)) {
            CallRegistry.answerCall(phoneNumber);
            return;
        }

        if ("decline_call".equals(data.action) || "hang_up".equals(data.action)) {
            CallRegistry.hangUp(phoneNumber);
            return;
        }

        LOGGER.atInfo().log("[PhonePage] Unhandled action: %s", data.action);
        sendUpdate(null, false);
    }

    // ── State loaders ─────────────────────────────────────────────────────────

    /**
     * Clears {@code #AppContent} and injects the AppMenu, wiring all its button
     * bindings into {@code evb}. Safe to call from both {@link #build} and
     * {@link #handleDataEvent} (via sendUpdate).
     */
    private void loadHomeState(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evb) {
        currentState = PhoneStatesEnum.HOME;
        cmd.clear(PHONEBOXSELECTOR);
        cmd.append(PHONEBOXSELECTOR, APPMENU_UI);
        AppMenu.buildEventBindings(evb);
    }





    // ── Live push ─────────────────────────────────────────────────────────────

    /** Called by {@link CallRegistry} when an incoming call arrives. */
    public void onIncomingCall(@Nonnull String callerNumber, @Nonnull String callerName) {
        currentState = PhoneStatesEnum.INCOMING_CALL;
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evb = new UIEventBuilder();
        Calls.loadIncomingCallState(callerNumber, callerName, cmd, evb);
        sendUpdate(cmd, evb, false);
    }

    /** Called by {@link CallRegistry} when the call is connected. */
    public void onCallAnswered(@Nonnull String partnerNumber, @Nonnull String partnerName) {
        currentState = PhoneStatesEnum.ACTIVE_CALL;
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evb = new UIEventBuilder();
        Calls.loadActiveCallState(partnerNumber, partnerName, cmd, evb);
        sendUpdate(cmd, evb, false);
    }

    /** Called by {@link CallRegistry} when the call ends (either side hangs up). */
    public void onCallEnded() {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evb = new UIEventBuilder();
        if (cachedRef != null && cachedStore != null) {
            Calls.loadCallsState(phoneNumber, cachedStore, cachedRef, cmd, evb);
            currentState = PhoneStatesEnum.CALLS;
        } else {
            loadHomeState(cmd, evb);
        }
        sendUpdate(cmd, evb, false);
    }

    /**
     * Called by {@link PhoneRegistry#deliver} when a message arrives for this player.
     * If the player is currently viewing the conversation with {@code fromNumber},
     * re-renders the chat immediately without requiring the player to reopen it.
     */
    public void onIncomingMessage(@Nonnull String fromNumber) {
        if (currentState == PhoneStatesEnum.CHAT
                && fromNumber.equals(currentChatContact)
                && cachedRef != null
                && cachedStore != null) {
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder evb = new UIEventBuilder();
            Whatgram.loadChat(phoneNumber, currentChatContact, cachedStore, cachedRef, cmd, evb);
            sendUpdate(cmd, evb, false);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void close(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            player.getPageManager().setPage(ref, store, Page.None);
        }
    }

    private void openApp(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String app) {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evb = new UIEventBuilder();
        switch (app) {
            case "whatgram" -> {
                currentState = PhoneStatesEnum.MESSAGES;
                Whatgram.loadConversationList(phoneNumber, store, ref, cmd, evb);
            }
            case "contacts" -> {
                currentState = PhoneStatesEnum.CONTACTS;
                Contacts.loadContactsState(phoneNumber, store, ref, cmd, evb);
            }
            case "gang" -> {
                currentState = PhoneStatesEnum.GANG;
                /* TODO */ }
            case "settings" -> {
                currentState = PhoneStatesEnum.SETTINGS;
                Settings.loadSettingsState(phoneNumber, cmd, evb);
            }
            case "calls" -> {
                currentState = PhoneStatesEnum.CALLS;
                Calls.loadCallsState(phoneNumber, store, ref, cmd, evb);
            }
            case "amason" -> {
                currentState = PhoneStatesEnum.MARKET;
                /* TODO */ }
            default -> {
                LOGGER.atWarning().log("[PhonePage] Unknown app: %s", app);
                sendUpdate(null, false);
                return;
            }
        }
        LOGGER.atInfo().log("[PhonePage] Opening app '%s', state -> %s", app, currentState);
        sendUpdate(cmd, evb, false);
    }

    // ── Event data codec ──────────────────────────────────────────────────────

    /**
     * Codec POJO for events fired by {@link PhonePage}.
     * Add fields here as new UI bindings are introduced.
     */
    public static final class PhoneEventData {

        /**
         * Action name sent by button bindings (e.g. {@code "close"},
         * {@code "open_app"}).
         */
        public String action;
        /** App identifier sent by AppMenu button bindings (e.g. {@code "whatgram"}). */
        public String app;
        /** Sender phone number passed by Whatgram chat-entry bindings. */
        public String contact;
        /** Message text captured from {@code #MessageInput.Value} via the {@code @} prefix. */
        public String messageValue;
        /** Phone number entered in the add-contact form. */
        public String contactFormNumber;
        /** Display name entered in the add-contact form. */
        public String contactFormName;
        /** Phone number entered from the calls dial pad. */
        public String dialNumber;
        public int STATE;
        public static final BuilderCodec<PhoneEventData> CODEC = BuilderCodec
                .builder(PhoneEventData.class, PhoneEventData::new)
                .append(
                        new KeyedCodec<>("Action", Codec.STRING),
                        (PhoneEventData o, String v) -> o.action = v,
                        (PhoneEventData o) -> o.action)
                .add()
                .append(
                        new KeyedCodec<>("App", Codec.STRING),
                        (PhoneEventData o, String v) -> o.app = v,
                        (PhoneEventData o) -> o.app)
                .add()
                .append(
                        new KeyedCodec<>("Contact", Codec.STRING),
                        (PhoneEventData o, String v) -> o.contact = v,
                        (PhoneEventData o) -> o.contact)
                .add()
                .append(
                        new KeyedCodec<>("@MessageValue", Codec.STRING),
                        (PhoneEventData o, String v) -> o.messageValue = v,
                        (PhoneEventData o) -> o.messageValue)
                .add()
                .append(
                        new KeyedCodec<>("@ContactFormNumber", Codec.STRING),
                        (PhoneEventData o, String v) -> o.contactFormNumber = v,
                        (PhoneEventData o) -> o.contactFormNumber)
                .add()
                .append(
                        new KeyedCodec<>("@ContactFormName", Codec.STRING),
                        (PhoneEventData o, String v) -> o.contactFormName = v,
                        (PhoneEventData o) -> o.contactFormName)
                .add()
                .append(
                        new KeyedCodec<>("@DialNumber", Codec.STRING),
                        (PhoneEventData o, String v) -> o.dialNumber = v,
                        (PhoneEventData o) -> o.dialNumber)
                .add()
                .append(
                        new KeyedCodec<>("State", Codec.INTEGER),
                        (PhoneEventData o, Integer s) -> o.STATE = s,
                        (PhoneEventData o) -> o.STATE)
                .add()
                .build();
    }
}
