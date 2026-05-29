package esq.phonemod.device.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.device.api.DevicePageHandle;
import esq.phonemod.device.core.DevicePageState;
import esq.phonemod.device.core.DeviceSession;
import esq.phonemod.phone.api.PhoneApp;
import esq.phonemod.phone.api.PhoneAppContext;
import esq.phonemod.phone.api.PhoneEvent;
import esq.phonemod.phone.api.PhoneEventActions;
import esq.phonemod.phone.apps.helpers.Calls;
import esq.phonemod.phone.core.PhoneService;
import esq.phonemod.phone.messaging.CallRegistry;
import org.bson.BsonDocument;
import org.bson.BsonValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic custom UI page backed by a device shell.
 */
public final class DevicePage extends InteractiveCustomUIPage<DevicePage.DeviceEventData>
        implements DevicePageHandle {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final DeviceSession session;

    public DevicePage(@Nonnull DeviceSession session) {
        super(session.getPlayerRef(), CustomPageLifetime.CanDismiss, DeviceEventData.CODEC);
        this.session = session;
        this.session.setPageHandle(this);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evb,
            @Nonnull Store<EntityStore> store) {
        session.updateStore(ref, store);
        cmd.append(session.getShell().getShellUiPath());

        String homeButton = session.getShell().getHomeButtonSelector();
        if (homeButton != null && !homeButton.isBlank()) {
            evb.addEventBinding(CustomUIEventBindingType.Activating,
                    homeButton,
                    EventData.of("Action", PhoneEventActions.HOME),
                    false);
        }

        if (session.getShell().hasCapability("calls")) {
            String activePartner = CallRegistry.getActivePartner(session.getDeviceId());
            String pendingCallee = CallRegistry.getPendingCallee(session.getDeviceId());
            String pendingCaller = CallRegistry.getPendingCaller(session.getDeviceId());
            if (activePartner != null) {
                session.setCurrentState(DevicePageState.ACTIVE_CALL);
                Calls.loadActiveCallState(activePartner, activePartner, cmd, evb);
                return;
            }
            if (pendingCallee != null) {
                session.setCurrentState(DevicePageState.ACTIVE_CALL);
                Calls.loadActiveCallState(pendingCallee, "Calling...", cmd, evb);
                return;
            }
            if (pendingCaller != null) {
                session.setCurrentState(DevicePageState.INCOMING_CALL);
                Calls.loadIncomingCallState(pendingCaller, pendingCaller, cmd, evb);
                return;
            }
        }

        buildAppMenu(cmd, evb);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            String rawData) {
        session.updateStore(ref, store);
        handleEventMap(ref, store, decodeEventMap(rawData));
    }

    private void handleEventMap(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull Map<String, String> data) {
        String action = data.getOrDefault("Action", "");
        LOGGER.atInfo().log("[DevicePage] handleDataEvent device=%s app=%s action=%s state=%s",
                session.getDeviceId(), data.get("App"), action, session.getCurrentAppId());

        if (PhoneEventActions.HOME.equals(action)) {
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder evb = new UIEventBuilder();
            loadHomeState(cmd, evb);
            sendUpdate(cmd, evb, false);
            return;
        }

        if (PhoneEventActions.OPEN_APP.equals(action)) {
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder evb = new UIEventBuilder();
            PhoneApp<?> app = openApp(ref, store, data.get("App"), cmd, evb);
            sendUpdate(app == null ? null : cmd, app == null ? null : evb, false);
            return;
        }

        if (PhoneEventActions.OPEN_CHAT.equals(action) && !"whatgram".equals(session.getCurrentAppId())) {
            String contact = param(data, "Contact");
            if (contact != null && !contact.isBlank()) {
                UICommandBuilder cmd = new UICommandBuilder();
                UIEventBuilder evb = new UIEventBuilder();
                PhoneApp<?> app = openApp(ref, store, "whatgram", cmd, evb);
                if (app != null) {
                    PhoneAppContext ctx = createContext(ref, store, "whatgram");
                    if (ctx != null) {
                        UICommandBuilder innerCmd = new UICommandBuilder();
                        UIEventBuilder innerEvb = new UIEventBuilder();
                        if (app.handleEvent(ctx, toPhoneEvent(data), innerCmd, innerEvb)) {
                            sendUpdate(innerCmd, innerEvb, false);
                            return;
                        }
                    }
                    sendUpdate(cmd, evb, false);
                    return;
                }
            }
            sendUpdate(null, false);
            return;
        }

        if (session.getShell().hasCapability("calls")
                && PhoneEventActions.ANSWER_CALL.equals(action)) {
            CallRegistry.answerCall(session.getDeviceId());
            sendUpdate(null, false);
            return;
        }

        if (session.getShell().hasCapability("calls")
                && (PhoneEventActions.DECLINE_CALL.equals(action) || PhoneEventActions.HANG_UP.equals(action))) {
            CallRegistry.hangUp(session.getDeviceId());
            sendUpdate(null, false);
            return;
        }

        PhoneApp<?> app = session.getCurrentApp();
        String appId = session.getCurrentAppId();
        if (app != null && appId != null) {
            PhoneAppContext ctx = createContext(ref, store, appId);
            if (ctx != null) {
                UICommandBuilder cmd = new UICommandBuilder();
                UIEventBuilder evb = new UIEventBuilder();
                if (app.handleEvent(ctx, toPhoneEvent(data), cmd, evb)) {
                    sendUpdate(cmd, evb, false);
                    return;
                }
            }
        }

        LOGGER.atInfo().log("[DevicePage] Unhandled action: %s", action);
        sendUpdate(null, false);
    }

    private void loadHomeState(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evb) {
        session.setCurrentState(DevicePageState.HOME);
        session.setCurrentApp(null);
        session.setCurrentAppId(null);
        cmd.clear(session.getShell().getContentSelector());
        cmd.appendInline(session.getShell().getContentSelector(),
                "Group " + session.getShell().getHomeHolderSelector() + " { LayoutMode: LeftCenterWrap; }");
        buildAppMenu(cmd, evb);
    }

    private void buildAppMenu(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evb) {
        String holder = session.getShell().getHomeHolderSelector();
        cmd.clear(holder);

        int index = 0;
        for (String appId : session.getShell().getDefaultApps()) {
            PhoneApp<?> app = PhoneService.get().getApp(appId);
            if (app == null) {
                LOGGER.atWarning().log("[DevicePage] Unknown default app %s for device %s",
                        appId, session.getDeviceAssetId());
                continue;
            }
            String buttonUI = app.getAppButtonUI();
            if (buttonUI == null || buttonUI.isBlank()) {
                LOGGER.atWarning().log("[DevicePage] app=%s has no button UI; skipped", app.getId());
                continue;
            }

            cmd.append(holder, buttonUI);
            String entrySelector = holder + "[" + index + "]";
            cmd.set(entrySelector + " #APPNAME.Text", app.getDisplayName());
            evb.addEventBinding(CustomUIEventBindingType.Activating,
                    entrySelector + " #APPBUTTON",
                    EventData.of("Action", PhoneEventActions.OPEN_APP).append("App", app.getId()),
                    false);
            index++;
        }
    }

    @Nullable
    private PhoneApp<?> openApp(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nullable String appId,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evb) {
        if (appId == null || appId.isBlank()) {
            return null;
        }
        PhoneApp<?> app = PhoneService.get().getApp(appId);
        if (app == null) {
            LOGGER.atWarning().log("[DevicePage] Unknown app: %s", appId);
            return null;
        }

        PhoneAppContext ctx = createContext(ref, store, appId);
        if (ctx == null) {
            return null;
        }

        PhoneApp<?> currentApp = session.getCurrentApp();
        if (currentApp != null) {
            currentApp.onClose(ctx);
        }

        session.setCurrentApp(app);
        session.setCurrentAppId(appId);
        session.setCurrentState(DevicePageState.APP);
        cmd.clear(session.getShell().getContentSelector());
        app.onOpen(ctx, cmd, evb);
        app.build(ctx, cmd, evb);
        return app;
    }

    @Nullable
    private PhoneAppContext createContext(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String appId) {
        return new PhoneAppContext(ref, store, session.getPlayerRef(), session.getDeviceId(), appId);
    }

    private PhoneEvent toPhoneEvent(@Nonnull Map<String, String> data) {
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = normalizeKey(entry.getKey());
            if (!"Action".equals(key) && !"App".equals(key)) {
                params.put(key, entry.getValue());
            }
        }
        params.putIfAbsent("State", data.getOrDefault("State", "0"));
        return new PhoneEvent(data.getOrDefault("Action", ""),
                data.getOrDefault("App", ""),
                params);
    }

    @Nullable
    private static String param(@Nonnull Map<String, String> data, @Nonnull String key) {
        String value = data.get(key);
        return value != null ? value : data.get("@" + key);
    }

    private static String normalizeKey(@Nonnull String key) {
        return key.startsWith("@") ? key.substring(1) : key;
    }

    @Nonnull
    private static Map<String, String> decodeEventMap(@Nonnull String rawData) {
        Map<String, String> result = new HashMap<>();
        BsonDocument document = BsonDocument.parse(rawData);
        for (Map.Entry<String, BsonValue> entry : document.entrySet()) {
            BsonValue value = entry.getValue();
            if (value == null || value.isNull()) {
                continue;
            }
            result.put(normalizeKey(entry.getKey()),
                    value.isString() ? value.asString().getValue() : value.toString());
        }
        return result;
    }

    @Override
    public void onIncomingMessage(@Nonnull String fromNumber) {
        PhoneApp<?> app = session.getCurrentApp();
        String appId = session.getCurrentAppId();
        if (app != null && appId != null) {
            PhoneAppContext ctx = createContext(session.getRef(), session.getStore(), appId);
            if (ctx != null) {
                UICommandBuilder cmd = new UICommandBuilder();
                UIEventBuilder evb = new UIEventBuilder();
                if (app.onIncomingMessage(ctx, fromNumber, cmd, evb)) {
                    sendUpdate(cmd, evb, false);
                    return;
                }
                app.onIncomingMessage(ctx, fromNumber);
                app.build(ctx, cmd, evb);
                sendUpdate(cmd, evb, false);
            }
        }
    }

    @Override
    public void onIncomingCall(@Nonnull String callerNumber, @Nonnull String callerName) {
        if (!session.getShell().hasCapability("calls")) {
            return;
        }
        playSound(session.getShell().getSoundProfile() != null
                ? session.getShell().getSoundProfile().getRingtone()
                : null);
        session.setCurrentState(DevicePageState.INCOMING_CALL);
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evb = new UIEventBuilder();
        Calls.loadIncomingCallState(callerNumber, callerName, cmd, evb);
        sendUpdate(cmd, evb, false);
    }

    @Override
    public void onCallAnswered(@Nonnull String partnerNumber, @Nonnull String partnerName) {
        if (!session.getShell().hasCapability("calls")) {
            return;
        }
        session.setCurrentState(DevicePageState.ACTIVE_CALL);
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evb = new UIEventBuilder();
        Calls.loadActiveCallState(partnerNumber, partnerName, cmd, evb);
        sendUpdate(cmd, evb, false);
    }

    @Override
    public void onCallEnded() {
        if (!session.getShell().hasCapability("calls")) {
            return;
        }
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evb = new UIEventBuilder();
        Calls.loadCallsState(session.getDeviceId(), session.getStore(), session.getRef(), cmd, evb);
        session.setCurrentState(DevicePageState.CALLS);
        sendUpdate(cmd, evb, false);
    }

    private void playSound(@Nullable String soundId) {
        if (soundId == null || soundId.isBlank()) {
            return;
        }
        int soundIndex = SoundEvent.getAssetMap().getIndex(soundId);
        if (soundIndex != 0) {
            SoundUtil.playSoundEvent2d(session.getRef(), soundIndex, SoundCategory.UI, session.getStore());
        }
    }

    public static final class DeviceEventData {
        public String action;
        public String app;
        public int state;

        public static final BuilderCodec<DeviceEventData> CODEC = BuilderCodec
                .builder(DeviceEventData.class, DeviceEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (data, value) -> data.action = value,
                        data -> data.action)
                .add()
                .append(new KeyedCodec<>("App", Codec.STRING),
                        (data, value) -> data.app = value,
                        data -> data.app)
                .add()
                .append(new KeyedCodec<>("State", Codec.INTEGER),
                        (data, value) -> data.state = value,
                        data -> data.state)
                .add()
                .build();
    }
}

