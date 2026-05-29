package esq.phonemod.phone.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.device.api.DevicePageHandle;
import esq.phonemod.phone.api.PhoneApp;
import esq.phonemod.phone.api.PhoneAppContext;
import esq.phonemod.phone.api.PhoneEvent;
import esq.phonemod.phone.apps.helpers.Calls;
import esq.phonemod.phone.core.PhoneService;
import esq.phonemod.phone.messaging.CallRegistry;
import java.util.HashMap;
import java.util.Map;

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
public final class PhonePage extends InteractiveCustomUIPage<PhonePage.PhoneEventData>
        implements DevicePageHandle {

    private static final int RINGTONE_NONKIA = SoundEvent.getAssetMap().getIndex("Ringtone_Nonkia");
    private static final int MESSAGE_SENT_SOUND = SoundEvent.getAssetMap().getIndex("Notification_Message_Sent");
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String PHONE_UI = "Pages/Phone/Phone.ui";
    private static final String PHONEBOXSELECTOR = "#AppContent";
    private PhoneStatesEnum currentState = PhoneStatesEnum.HOME;
    private PhoneApp<?> currentApp = null;
    private String currentAppId = null;
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
            // Initial open: Phone.ui already placed #APPHolder inside #AppContent —
            // populate it directly without clearing #AppContent first.
            AppMenu.build(cmd, evb);
        }
    }

    // ── Event handling ────────────────────────────────────────────────────────

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PhoneEventData data) {
        this.cachedRef = ref;
        this.cachedStore = store;
        LOGGER.atInfo().log("[PhonePage] handleDataEvent phone=%s app=%s action=%s state=%s", phoneNumber, data.app, data.action, currentAppId);
        if ("home".equals(data.action)) {
            currentState = PhoneStatesEnum.HOME;
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder evb = new UIEventBuilder();
            loadHomeState(cmd, evb);
            sendUpdate(cmd, evb, false);
            return;
        }

        if ("open_app".equals(data.action)) {
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder evb = new UIEventBuilder();
            PhoneApp<?> app = openApp(ref, store, data.app, cmd, evb);
            if (app == null) {
                sendUpdate(null, false);
                return;
            }
            sendUpdate(cmd, evb, false);
            return;
        }

        if ("open_chat".equals(data.action) && !"whatgram".equals(currentAppId)) {
            if (data.contact != null && !data.contact.isBlank()) {
                UICommandBuilder cmd = new UICommandBuilder();
                UIEventBuilder evb = new UIEventBuilder();
                PhoneApp<?> app = openApp(ref, store, "whatgram", cmd, evb);
                if (app == null) {
                    sendUpdate(null, false);
                    return;
                }
                PhoneAppContext ctx = createContext(ref, store, "whatgram");
                if (ctx != null) {
                    PhoneEvent event = toPhoneEvent(data);
                    UICommandBuilder innerCmd = new UICommandBuilder();
                    UIEventBuilder innerEvb = new UIEventBuilder();
                    if (app.handleEvent(ctx, event, innerCmd, innerEvb)) {
                        sendUpdate(innerCmd, innerEvb, false);
                        return;
                    }
                }
                sendUpdate(cmd, evb, false);
                return;
            }
            sendUpdate(null, false);
            return;
        }

        if ("answer_call".equals(data.action)) {
            CallRegistry.answerCall(phoneNumber);
            sendUpdate(null, false);
            return;
        }

        if ("decline_call".equals(data.action) || "hang_up".equals(data.action)) {
            CallRegistry.hangUp(phoneNumber);
            sendUpdate(null, false);
            return;
        }

        PhoneApp<?> app = currentApp;
        if (app != null) {
            PhoneAppContext ctx = createContext(ref, store, currentAppId);
            if (ctx != null) {
                PhoneEvent event = toPhoneEvent(data);
                UICommandBuilder cmd = new UICommandBuilder();
                UIEventBuilder evb = new UIEventBuilder();
                if (app.handleEvent(ctx, event, cmd, evb)) {
                    sendUpdate(cmd, evb, false);
                    return;
                }
            }
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
        currentApp = null;
        currentAppId = null;
        // #APPHolder was destroyed when an app loaded its UI into #AppContent.
        // Clear the app content and restore the home grid container inline so
        // AppMenu.build can populate it without needing AppMenu.ui.
        cmd.clear(PHONEBOXSELECTOR);
        cmd.appendInline(PHONEBOXSELECTOR, "Group #APPHolder { LayoutMode: LeftCenterWrap; }");
        AppMenu.build(cmd, evb);
    }





    // ── Live push ─────────────────────────────────────────────────────────────

    /** Called by {@link CallRegistry} when an incoming call arrives. */
    public void onIncomingCall(@Nonnull String callerNumber, @Nonnull String callerName) {
        if (cachedRef != null && cachedStore != null) {
            if (RINGTONE_NONKIA != 0) {
                SoundUtil.playSoundEvent2d(cachedRef, RINGTONE_NONKIA, SoundCategory.UI, cachedStore);
            }
            if (currentApp != null && currentAppId != null) {
                PhoneAppContext ctx = createContext(cachedRef, cachedStore, currentAppId);
                if (ctx != null) {
                    currentApp.onIncomingCall(ctx, callerNumber, callerName);
                    UICommandBuilder cmd = new UICommandBuilder();
                    UIEventBuilder evb = new UIEventBuilder();
                    currentApp.build(ctx, cmd, evb);
                    sendUpdate(cmd, evb, false);
                    return;
                }
            }
        }
        currentState = PhoneStatesEnum.INCOMING_CALL;
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evb = new UIEventBuilder();
        Calls.loadIncomingCallState(callerNumber, callerName, cmd, evb);
        sendUpdate(cmd, evb, false);
    }

    /** Called by {@link CallRegistry} when the call is connected. */
    public void onCallAnswered(@Nonnull String partnerNumber, @Nonnull String partnerName) {
        if (cachedRef != null && cachedStore != null && currentApp != null && currentAppId != null) {
            PhoneAppContext ctx = createContext(cachedRef, cachedStore, currentAppId);
            if (ctx != null) {
                currentApp.onClose(ctx);
                UICommandBuilder cmd = new UICommandBuilder();
                UIEventBuilder evb = new UIEventBuilder();
                currentApp.build(ctx, cmd, evb);
                sendUpdate(cmd, evb, false);
                return;
            }
        }
        currentState = PhoneStatesEnum.ACTIVE_CALL;
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evb = new UIEventBuilder();
        Calls.loadActiveCallState(partnerNumber, partnerName, cmd, evb);
        sendUpdate(cmd, evb, false);
    }

    /** Called by {@link CallRegistry} when the call ends (either side hangs up). */
    public void onCallEnded() {
        if (cachedRef != null && cachedStore != null && currentApp != null && currentAppId != null) {
            PhoneAppContext ctx = createContext(cachedRef, cachedStore, currentAppId);
            if (ctx != null) {
                currentApp.onClose(ctx);
                UICommandBuilder cmd = new UICommandBuilder();
                UIEventBuilder evb = new UIEventBuilder();
                currentApp.build(ctx, cmd, evb);
                sendUpdate(cmd, evb, false);
                return;
            }
        }
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
     * Called by {@link esq.phonemod.phone.messaging.PhoneRegistry#deliver} when a message arrives for this player.
     * If the player is currently viewing the conversation with {@code fromNumber},
     * re-renders the chat immediately without requiring the player to reopen it.
     */
    public void onIncomingMessage(@Nonnull String fromNumber) {
        LOGGER.atInfo().log("[PhonePage] onIncomingMessage phone=%s currentApp=%s from=%s", phoneNumber, currentAppId, fromNumber);
        if (currentApp != null && currentAppId != null && cachedRef != null && cachedStore != null) {
            PhoneAppContext ctx = createContext(cachedRef, cachedStore, currentAppId);
            if (ctx != null) {
                UICommandBuilder cmd = new UICommandBuilder();
                UIEventBuilder evb = new UIEventBuilder();
                if (currentApp.onIncomingMessage(ctx, fromNumber, cmd, evb)) {
                    LOGGER.atInfo().log("[PhonePage] dynamic incoming message handled for %s", currentAppId);
                    sendUpdate(cmd, evb, false);
                    return;
                }
                LOGGER.atInfo().log("[PhonePage] falling back to rebuild on incoming message for %s", currentAppId);
                currentApp.onIncomingMessage(ctx, fromNumber);
                currentApp.build(ctx, cmd, evb);
                sendUpdate(cmd, evb, false);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void close(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            player.getPageManager().setPage(ref, store, Page.None);
        }
    }

    private PhoneAppContext createContext(@Nonnull Ref<EntityStore> ref,
                                          @Nonnull Store<EntityStore> store,
                                          @Nonnull String appId) {
        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            LOGGER.atWarning().log("[PhonePage] PlayerRef missing for phone context");
            return null;
        }
        return new PhoneAppContext(ref, store, playerRef, phoneNumber, appId);
    }

    private PhoneEvent toPhoneEvent(@Nonnull PhoneEventData data) {
        Map<String, String> params = new HashMap<>();
        if (data.contact != null) {
            params.put("Contact", data.contact);
        }
        if (data.messageValue != null) {
            params.put("MessageValue", data.messageValue);
        }
        if (data.contactFormNumber != null) {
            params.put("ContactFormNumber", data.contactFormNumber);
        }
        if (data.contactFormName != null) {
            params.put("ContactFormName", data.contactFormName);
        }
        if (data.dialNumber != null) {
            params.put("DialNumber", data.dialNumber);
        }
        params.put("State", String.valueOf(data.STATE));
        return new PhoneEvent(data.action == null ? "" : data.action,
                data.app == null ? "" : data.app,
                params);
    }

    private PhoneApp<?> openApp(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String appId,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evb) {
        PhoneApp<?> app = PhoneService.get().getApp(appId);
        if (app == null) {
            LOGGER.atWarning().log("[PhonePage] Unknown app: %s", appId);
            return null;
        }

        PhoneAppContext ctx = createContext(ref, store, appId);
        if (ctx == null) {
            return null;
        }

        if (currentApp != null) {
            currentApp.onClose(ctx);
        }

        this.currentApp = app;
        this.currentAppId = appId;
        this.currentState = PhoneStatesEnum.APP;
        cmd.clear(PHONEBOXSELECTOR);
        app.onOpen(ctx, cmd, evb);
        app.build(ctx, cmd, evb);
        LOGGER.atInfo().log("[PhonePage] Opening app '%s', state -> %s", appId, currentState);
        return app;
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
