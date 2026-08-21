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
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import esq.phonemod.device.api.DevicePageHandle;
import esq.phonemod.device.components.DeviceSettings;
import esq.phonemod.device.components.DeviceSettingsComponent;
import esq.phonemod.device.core.DevicePageState;
import esq.phonemod.device.core.DeviceSession;
import esq.phonemod.device.routing.DefaultPhoneRouter;
import esq.phonemod.device.routing.PhoneRouter;
import esq.phonemod.device.routing.RouteContext;
import esq.phonemod.device.routing.RouterHost;
import esq.phonemod.device.theme.ThemeService;
import esq.phonemod.device.events.DeviceAppCloseEvent;
import esq.phonemod.device.events.DeviceAppOpenEvent;
import esq.phonemod.device.events.DeviceCloseEvent;
import esq.phonemod.device.events.DeviceOpenEvent;
import esq.phonemod.device.events.PhoneEvents;
import esq.phonemod.phone.api.PhoneApp;
import esq.phonemod.phone.api.PhoneAppContext;
import esq.phonemod.phone.api.PhoneEvent;
import esq.phonemod.phone.api.PhoneEventActions;
import esq.phonemod.phone.apps.AppStoreApp;
import esq.phonemod.phone.apps.helpers.Calls;
import esq.phonemod.phone.appstore.AppInstallService;
import esq.phonemod.phone.appstore.DeviceInstalledAppsComponent;
import esq.phonemod.phone.core.PhoneService;
import esq.phonemod.phone.messaging.CallRegistry;
import esq.phonemod.phone.messaging.CallRestoreDecision;
import esq.phonemod.phone.messaging.PhoneRegistry;
import esq.phonemod.PhoneMod;
import esq.phonemod.security.AppOpenAuthorizer;
import esq.phonemod.security.UiEventDecoder;
import esq.phonemod.security.UiEventValidator;
import esq.phonemod.operations.OperationalMetrics;
import esq.phonemod.platform.RuntimeDevicePlatform;
import esq.phonemod.api.DeviceIdentity;
import esq.phonemod.api.event.AppOpenEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Generic custom UI page backed by a device shell.
 */
public final class DevicePage extends InteractiveCustomUIPage<DevicePage.DeviceEventData>
        implements DevicePageHandle, RouterHost {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Shared, immutable router holding all built-in phone routes. Stateless and
     * thread-safe (per-interaction state lives in {@link RouteContext}), so a
     * single instance serves every open device.
     */
    private static final PhoneRouter ROUTER = DefaultPhoneRouter.createDefault();

    private final DeviceSession session;
    private volatile boolean active = true;

    public DevicePage(@Nonnull DeviceSession session) {
        super(session.getPlayerRef(), CustomPageLifetime.CanDismiss, DeviceEventData.CODEC);
        this.session = session;
        this.session.setPageHandle(this);
    }

    /** Returns the runtime session backing this page. */
    @Nonnull
    public DeviceSession getSession() {
        return session;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evb,
            @Nonnull Store<EntityStore> store) {
        session.updateStore(ref, store);
        DeviceIdentity identity = resolveIdentity();
        if (identity != null) {
            var pre = HytaleServer.get().getEventBus().dispatchFor(esq.phonemod.api.event.DeviceOpenEvent.Pre.class)
                    .dispatch(new esq.phonemod.api.event.DeviceOpenEvent.Pre(
                            session.getPlayerRef().getUuid(), identity));
            if (pre.isCancelled()) return;
        }
        PhoneEvents.post(new DeviceOpenEvent(session));
        if (identity != null) HytaleServer.get().getEventBus()
                .dispatchFor(esq.phonemod.api.event.DeviceOpenEvent.Post.class)
                .dispatch(new esq.phonemod.api.event.DeviceOpenEvent.Post(
                        session.getPlayerRef().getUuid(), identity));
        cmd.append(session.getShell().getShellUiPath());
        ThemeService.applyShell(cmd, session.getShell(), resolveThemeId());

        // Seed installed apps on every open (idempotent for system apps,
        // once-only for non-system defaults — Decision D).
        UUID playerUuid = resolvePlayerUuid(ref, store);
        if (playerUuid != null) {
            AppInstallService.get().seedDefaults(
                    ref, store, playerUuid,
                    session.getDeviceId(),
                    session.getShell().getDefaultApps());
        }

        String homeButton = session.getShell().getHomeButtonSelector();
        if (homeButton != null && !homeButton.isBlank()) {
            evb.addEventBinding(CustomUIEventBindingType.Activating,
                    homeButton,
                    EventData.of("Action", PhoneEventActions.HOME),
                    true);
        }

        if (session.getShell().hasCapability("calls")) {
            String activePartner = CallRegistry.getActivePartner(session.getDeviceId());
            String pendingCallee = CallRegistry.getPendingCallee(session.getDeviceId());
            String pendingCaller = CallRegistry.getPendingCaller(session.getDeviceId());
            String content = session.getShell().getContentSelector();
            CallRestoreDecision.Restore restore = CallRestoreDecision.resolve(
                    activePartner, pendingCallee, pendingCaller);
            switch (restore.kind()) {
                case ACTIVE -> {
                    session.setCurrentState(DevicePageState.ACTIVE_CALL);
                    Calls.loadActiveCallState(session.getDeviceId(), restore.value(),
                            restore.value(), content, cmd, evb);
                    return;
                }
                case OUTGOING -> {
                    session.setCurrentState(DevicePageState.ACTIVE_CALL);
                    Calls.loadActiveCallState(session.getDeviceId(), restore.value(),
                            "Calling...", content, cmd, evb);
                    return;
                }
                case INCOMING -> {
                    session.setCurrentState(DevicePageState.INCOMING_CALL);
                    Calls.loadIncomingCallState(restore.value(), restore.value(), content, cmd, evb);
                    return;
                }
                case NONE -> { }
            }
        }

        loadHome(cmd, evb);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            String rawData) {
        session.updateStore(ref, store);
        try {
            if (!isAuthorizedSession(ref, store)) {
                rejectAction("session_not_authorized");
                return;
            }
            if (!session.tryAcquireAction()) {
                rejectAction("rate_limited");
                return;
            }
            UiEventValidator.ValidationResult rawResult =
                    UiEventValidator.validateRaw(rawData, PhoneMod.config());
            if (!rawResult.allowed()) {
                rejectAction(rawResult.reason());
                return;
            }
            Map<String, String> event = UiEventDecoder.decode(rawData);
            UiEventValidator.ValidationResult fieldResult =
                    UiEventValidator.validateFields(event, PhoneMod.config());
            if (!fieldResult.allowed()) {
                rejectAction(fieldResult.reason());
                return;
            }
            String suppliedApp = event.get("App");
            if (suppliedApp != null && !PhoneEventActions.OPEN_APP.equals(event.get("Action"))
                    && session.getCurrentAppId() != null
                    && !session.getCurrentAppId().equals(suppliedApp)) {
                rejectAction("stale_app_state");
                return;
            }
            handleEventMap(ref, store, event);
        } catch (RuntimeException error) {
            LOGGER.atWarning().log(
                    "[DevicePage] Rejected malformed UI event device=%s reason=%s",
                    session.getDeviceId(), error.getMessage());
            sendUpdate(null, null, false);
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        super.onDismiss(ref, store);
        active = false;
        session.updateStore(ref, store);
        closeCurrentApp(ref, store);
        // Mark the phone UI as closed so we stop pushing to a dismissed page and a
        // re-open never orphans this one. Presence is kept so the player still gets
        // toasts while connected. Guarded by page identity to avoid wiping a page
        // that already replaced this one.
        PhoneRegistry.unregisterPage(session.getDeviceId(), this);
        session.setPageHandle(null);
        // Notify listeners (e.g. the sync layer drops this phone's subscriptions)
        // that the device page is gone.
        PhoneEvents.post(new DeviceCloseEvent(session));
    }

    private void handleEventMap(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull Map<String, String> data) {
        String action = data.getOrDefault("Action", "");
        LOGGER.atFine().log("[DevicePage] handleDataEvent device=%s app=%s action=%s state=%s",
                session.getDeviceId(), data.get("App"), action, session.getCurrentAppId());

        // Declarative dispatch: the router matches the action to a Route
        // (guards -> resolvers -> handlers) and applies the resulting UI flush
        // through this page's RouterHost methods.
        ROUTER.route(new RouteContext(ref, store, session, action, data, this));
    }

    @Override
    public void loadHomeState(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evb) {
        closeCurrentApp(session.getRef(), session.getStore());
        session.setCurrentState(DevicePageState.HOME);
        cmd.clear(session.getShell().getContentSelector());
        loadHome(cmd, evb);
    }

    /**
     * Builds the home screen — the app grid ({@code #APPHolder}) plus its app
     * buttons — into the (already-empty) content area. The home-holder layout is
     * defined here once and reused by both initial {@link #build} and Home
     * navigation, so it no longer duplicates the shell {@code .ui}. The status
     * bar is not touched: it lives outside the content area and is persistent.
     */
    private void loadHome(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evb) {
        DeviceSettings settings = resolveDeviceSettings();
        String layoutMode = settings != null && "List".equals(settings.getAppLayout())
                ? "TopScrolling" : "LeftCenterWrap";
        cmd.set(session.getShell().getContentSelector() + ".Background",
                resolveWallpaper(settings));
        cmd.appendInline(session.getShell().getContentSelector(),
                "Group " + session.getShell().getHomeHolderSelector()
                        + " { FlexWeight: 1; Padding: (Full: 8); LayoutMode: "
                        + layoutMode + "; }");
        buildAppMenu(cmd, evb);
    }

    /**
     * Builds the home grid from the player's installed apps (Decision B).
     *
     * <p>Compatible system apps are always shown first in device-asset declaration
     * order, followed by compatible installed non-system apps sorted by
     * {@link PhoneApp#getSortOrder()}. Hidden-permission apps are suppressed. The
     * grid iterates the {@link DeviceInstalledAppsComponent} rather than only the
     * static {@code defaultApps} array so that the home screen reflects actual
     * installs.
     */
    private void buildAppMenu(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evb) {
        String holder = session.getShell().getHomeHolderSelector();
        cmd.clear(holder);

        // Build ordered list: system apps first, then sorted installed non-system apps.
        List<String> appIds = new ArrayList<>();
        List<String> installedAppIds = new ArrayList<>();

        // System apps — always present in device-asset declaration order.
        for (String appId : session.getShell().getDefaultApps()) {
            PhoneApp<?> app = PhoneService.get().getApp(appId);
            if (app != null && app.isSystemApp() && supportsCurrentDevice(app)) {
                appIds.add(appId);
            }
        }

        // Non-system installed apps from the persistent component.
        DeviceInstalledAppsComponent comp = session.getStore().getComponent(
                session.getRef(), DeviceInstalledAppsComponent.getComponentType());
        if (comp != null) {
            Set<String> installed = comp.getInstalled(session.getDeviceId());
            for (String appId : installed) {
                if ("__seeded__".equals(appId)) {
                    continue;
                }
                PhoneApp<?> app = PhoneService.get().getApp(appId);
                if (app != null && !app.isSystemApp() && supportsCurrentDevice(app)) {
                    installedAppIds.add(appId);
                }
            }
        }
        installedAppIds.sort(Comparator
                .comparingInt((String appId) -> PhoneService.get().getApp(appId).getSortOrder())
                .thenComparing(appId -> appId));
        appIds.addAll(installedAppIds);

        int index = 0;
        UUID playerUuid = resolvePlayerUuid(session.getRef(), session.getStore());

        for (String appId : appIds) {
            PhoneApp<?> app = PhoneService.get().getApp(appId);
            if (app == null) {
                continue;
            }
            // Suppress hidden-permission apps (player lacks perm AND visibility = hidden).
            String requiredNode = app.getRequiredPermission();
            if (requiredNode != null && "hidden".equals(app.getPermissionVisibility())
                    && playerUuid != null
                    && !PermissionsModule.get().hasPermission(playerUuid, requiredNode)) {
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
                    true);
            index++;
        }
    }

    @Override
    @Nullable
    public PhoneApp<?> openApp(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nullable String appId,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evb) {
        if (appId == null || appId.isBlank()) {
            return null;
        }
        PhoneApp<?> app = PhoneService.get().getApp(appId);
        boolean appKnown = app != null;
        UUID actorUuid = resolvePlayerUuid(ref, store);
        Set<String> supportedDeviceTypes =
                appKnown ? Set.of(app.getSupportedDeviceTypes()) : Set.of();
        String deviceType = session.getShell().getDeviceType();
        boolean sessionOwnedByActor = isAuthorizedSession(ref, store);
        boolean installed = appKnown
                && AppInstallService.get().isInstalled(ref, store, session.getDeviceId(), appId);
        Set<String> requiredPermissions = appKnown ? app.getRequiredPermissions() : Set.of();
        java.util.function.Predicate<String> hasPermission =
                node -> actorUuid != null && PermissionsModule.get().hasPermission(actorUuid, node);

        AppOpenAuthorizer.Decision decision = AppOpenAuthorizer.authorize(
                appKnown, supportedDeviceTypes, deviceType, sessionOwnedByActor,
                installed, requiredPermissions, hasPermission);
        switch (decision) {
            case ALLOW:
                break;
            case MISSING_PERMISSION: {
                // Decision C: re-verify permission at launch time (one check per open).
                PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
                if (pr != null) {
                    pr.sendMessage(Message.translation("phonemod.appstore.launch.denied"));
                }
                return null;
            }
            case UNKNOWN_APP:
                LOGGER.atWarning().log("[DevicePage] Unknown app: %s", appId);
                return null;
            case INCOMPATIBLE_DEVICE:
                LOGGER.atWarning().log("[DevicePage] App %s does not support device type %s",
                        appId, session.getShell().getDeviceType());
                return null;
            case NOT_OWNER:
                LOGGER.atWarning().log("[DevicePage] Unauthorized app open device=%s app=%s",
                        session.getDeviceId(), appId);
                return null;
            case NOT_INSTALLED:
                LOGGER.atWarning().log("[DevicePage] App is not installed device=%s app=%s",
                        session.getDeviceId(), appId);
                return null;
            default:
                return null;
        }

        UUID openingPlayer = resolvePlayerUuid(ref, store);
        if (openingPlayer == null) return null;
        AppOpenEvent.Pre pre = HytaleServer.get().getEventBus().dispatchFor(AppOpenEvent.Pre.class)
                .dispatch(new AppOpenEvent.Pre(openingPlayer, session.getDeviceId(), appId));
        if (pre.isCancelled()) return null;

        closeCurrentApp(ref, store);

        session.setCurrentApp(app);
        session.setCurrentAppId(appId);
        session.setCurrentState(DevicePageState.APP);

        PhoneAppContext ctx = createContext(ref, store, appId);
        if (ctx == null) {
            session.setCurrentApp(null);
            session.setCurrentAppId(null);
            session.setCurrentState(DevicePageState.HOME);
            return null;
        }

        // Inject device type so apps can filter the catalog without a session ref.
        ctx.setState(AppStoreApp.DEVICE_TYPE_STATE_KEY, session.getShell().getDeviceType());

        PhoneEvents.post(new DeviceAppOpenEvent(session, appId));
        cmd.clear(session.getShell().getContentSelector());
        app.onOpen(ctx, cmd, evb);
        app.build(ctx, cmd, evb);
        ThemeService.apply(cmd, resolveThemeId(), session.getShell().getDefaultThemeId(),
                Map.of("appContent", session.getShell().getContentSelector()));
        ThemeService.apply(cmd, resolveThemeId(), session.getShell().getDefaultThemeId(),
                app.getThemeableSelectors());
        HytaleServer.get().getEventBus().dispatchFor(AppOpenEvent.Post.class)
                .dispatch(new AppOpenEvent.Post(openingPlayer, session.getDeviceId(), appId));
        return app;
    }

    private boolean supportsCurrentDevice(@Nonnull PhoneApp<?> app) {
        String[] supported = app.getSupportedDeviceTypes();
        if (supported == null || supported.length == 0) {
            return true;
        }
        String deviceType = session.getShell().getDeviceType();
        for (String type : supported) {
            if (deviceType.equals(type)) {
                return true;
            }
        }
        return false;
    }

    private void closeCurrentApp(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        PhoneApp<?> currentApp = session.getCurrentApp();
        String currentAppId = session.getCurrentAppId();
        if (currentApp == null || currentAppId == null) {
            session.setCurrentApp(null);
            session.setCurrentAppId(null);
            return;
        }

        PhoneAppContext ctx = createContext(ref, store, currentAppId);
        if (ctx != null) {
            currentApp.onClose(ctx);
        }
        PhoneEvents.post(new DeviceAppCloseEvent(session, currentAppId));
        session.setCurrentApp(null);
        session.setCurrentAppId(null);
    }

    @Override
    @Nullable
    public PhoneAppContext createContext(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String appId) {
        return new PhoneAppContext(ref, store, session.getPlayerRef(), session.getDeviceId(), appId,
                session.getShell().getContentSelector());
    }

    /**
     * Flushes a UI update. {@link RouterHost} entry point used by the router to
     * apply a {@code RouteResult}; {@code (null, null)} mirrors the legacy
     * acknowledgement-only {@code sendUpdate(null, false)}.
     */
    @Override
    public void sendUpdate(@Nullable UICommandBuilder cmd, @Nullable UIEventBuilder evb) {
        sendUpdate(cmd, evb, false);
    }

    @Override
    @Nonnull
    public PhoneEvent toPhoneEvent(@Nonnull Map<String, String> data) {
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

    private static String normalizeKey(@Nonnull String key) {
        return key.startsWith("@") ? key.substring(1) : key;
    }

    private boolean isAuthorizedSession(@Nonnull Ref<EntityStore> ref,
                                        @Nonnull Store<EntityStore> store) {
        UUID playerUuid = resolvePlayerUuid(ref, store);
        if (!active || playerUuid == null || !session.isOwnedBy(playerUuid)) return false;
        PhoneRegistry.OnlineEntry current = PhoneRegistry.getOnlineEntry(session.getDeviceId());
        return current == null || PhoneRegistry.isAuthorizedPage(session.getDeviceId(), ref, this);
    }

    @Nullable
    private DeviceIdentity resolveIdentity() {
        var identities = RuntimeDevicePlatform.get().identities();
        try {
            var byId = identities.find(UUID.fromString(session.getDeviceId()));
            if (byId.isPresent()) return byId.get();
        } catch (IllegalArgumentException ignored) {}
        return identities.findAddress(session.getDeviceId()).orElse(null);
    }

    private void rejectAction(@Nonnull String reason) {
        OperationalMetrics.get().rejectedAction();
        if ("rate_limited".equals(reason)) OperationalMetrics.get().rateLimited();
        LOGGER.atWarning().log("[DevicePage] Rejected UI action device=%s reason=%s",
                session.getDeviceId(), reason);
        sendUpdate(null, null, false);
    }

    @Override
    public void onIncomingMessage(@Nonnull String fromNumber) {
        if (session.getCurrentState() != DevicePageState.APP) {
            return;
        }
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
        DeviceSettings settings = resolveDeviceSettings();
        playSound(settings != null && "Silent".equals(settings.getSoundProfileId())
                ? null : session.getShell().getSoundProfile() != null
                ? session.getShell().getSoundProfile().getRingtone()
                : null);
        PhoneApp<?> app = session.getCurrentApp();
        String appId = session.getCurrentAppId();
        if (app != null && appId != null) {
            PhoneAppContext ctx = createContext(session.getRef(), session.getStore(), appId);
            if (ctx != null) {
                app.onIncomingCall(ctx, callerNumber, callerName);
            }
        }
        session.setCurrentState(DevicePageState.INCOMING_CALL);
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evb = new UIEventBuilder();
        Calls.loadIncomingCallState(callerNumber, callerName,
                session.getShell().getContentSelector(), cmd, evb);
        sendUpdate(cmd, evb, false);
    }

    @Override
    public void onOutgoingCall(@Nonnull String calleeNumber, @Nonnull String calleeName) {
        if (!session.getShell().hasCapability("calls")) {
            return;
        }
        session.setCurrentState(DevicePageState.ACTIVE_CALL);
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evb = new UIEventBuilder();
        // Reuse the active-call overlay with a "Calling…" label until the callee answers;
        // this matches what build() shows for a pending outgoing call on reopen.
        Calls.loadActiveCallState(session.getDeviceId(), calleeNumber, "Calling… " + calleeName,
                session.getShell().getContentSelector(), cmd, evb);
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
        Calls.loadActiveCallState(session.getDeviceId(), partnerNumber, partnerName,
                session.getShell().getContentSelector(), cmd, evb);
        sendUpdate(cmd, evb, false);
    }

    @Override
    public void onCallEnded() {
        if (!session.getShell().hasCapability("calls")) {
            return;
        }
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evb = new UIEventBuilder();
        Calls.loadCallsState(session.getDeviceId(), session.getStore(), session.getRef(),
                session.getShell().getContentSelector(), cmd, evb);
        session.setCurrentState(DevicePageState.CALLS);
        sendUpdate(cmd, evb, false);
    }

    @Override
    public void refreshApp(@Nonnull String appId) {
        // Generic sync re-render: repaint the app only if it is the one open right
        // now (the viewer may have navigated elsewhere since subscribing). Mirrors
        // openApp's clear -> build -> theme so the rebuild is clean and themed.
        if (session.getCurrentState() != DevicePageState.APP
                || !appId.equals(session.getCurrentAppId())) {
            return;
        }
        PhoneApp<?> app = session.getCurrentApp();
        if (app == null) {
            return;
        }
        PhoneAppContext ctx = createContext(session.getRef(), session.getStore(), appId);
        if (ctx == null) {
            return;
        }
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evb = new UIEventBuilder();
        cmd.clear(session.getShell().getContentSelector());
        app.build(ctx, cmd, evb);
        ThemeService.apply(cmd, resolveThemeId(), session.getShell().getDefaultThemeId(),
                Map.of("appContent", session.getShell().getContentSelector()));
        ThemeService.apply(cmd, resolveThemeId(), session.getShell().getDefaultThemeId(),
                app.getThemeableSelectors());
        sendUpdate(cmd, evb, false);
    }

    /**
     * Resolves the theme id for this device: the player's saved {@link DeviceSettings}
     * theme if present, otherwise the device asset's default theme.
     */
    @Nullable
    private String resolveThemeId() {
        DeviceSettings settings = resolveDeviceSettings();
        if (settings != null && settings.getThemeId() != null
                && !settings.getThemeId().isBlank()) {
            return settings.getThemeId();
        }
        return session.getShell().getDefaultThemeId();
    }

    @Nullable
    private DeviceSettings resolveDeviceSettings() {
        var type = DeviceSettingsComponent.getComponentType();
        if (type == null) return null;
        DeviceSettingsComponent component =
                session.getStore().getComponent(session.getRef(), type);
        return component != null ? component.get(session.getDeviceId()) : null;
    }

    @Nonnull
    private String resolveWallpaper(@Nullable DeviceSettings settings) {
        String wallpaper = settings != null ? settings.getWallpaper() : null;
        if ("Dark".equals(wallpaper)) return "#0f1724";
        if ("None".equals(wallpaper)) return "#151c26";
        String assetDefault = session.getShell().getDefaultWallpaper();
        return assetDefault != null && !assetDefault.isBlank() ? assetDefault : "#151c26";
    }

    @Override
    public void reapplyTheme(@Nonnull String themeId) {
        UICommandBuilder cmd = new UICommandBuilder();
        ThemeService.applyShell(cmd, session.getShell(), themeId);
        ThemeService.apply(cmd, themeId, session.getShell().getDefaultThemeId(),
                Map.of("appContent", session.getShell().getContentSelector()));
        PhoneApp<?> app = session.getCurrentApp();
        if (app != null) {
            ThemeService.apply(cmd, themeId, session.getShell().getDefaultThemeId(),
                    app.getThemeableSelectors());
        }
        sendUpdate(cmd, null, false);
    }

    /**
     * Resolves the UUID of the player entity at {@code ref}, or {@code null} if
     * the entity has no {@link PlayerRef} (non-player entity, already despawned).
     */
    @Nullable
    private UUID resolvePlayerUuid(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store) {
        PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
        return pr != null ? pr.getUuid() : null;
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
