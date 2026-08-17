package esq.phonemod.setup;

import esq.phonemod.PhoneMod;
import esq.phonemod.device.api.DevicePageHandle;
import esq.phonemod.device.core.DeviceService;
import esq.phonemod.device.events.DeviceAppCloseEvent;
import esq.phonemod.device.events.DeviceCloseEvent;
import esq.phonemod.device.events.DeviceSettingsChangedEvent;
import esq.phonemod.device.events.PhoneEvents;
import esq.phonemod.phone.apps.AppStoreApp;
import esq.phonemod.phone.apps.CallsApp;
import esq.phonemod.phone.apps.ContactsApp;
import esq.phonemod.phone.apps.GroupChatApp;
import esq.phonemod.phone.apps.SettingsApp;
import esq.phonemod.phone.apps.WhatgramApp;
import esq.phonemod.phone.appstore.AppCatalogService;
import esq.phonemod.phone.appstore.AppInstallService;
import esq.phonemod.phone.core.PhoneService;
import esq.phonemod.phone.messaging.CallRegistry;
import esq.phonemod.phone.messaging.PhoneRegistry;
import esq.phonemod.phone.sync.SyncService;
import esq.phonemod.phone.sync.Viewer;
import esq.phonemod.platform.RuntimeDevicePlatform;
import esq.phonemod.phone.sync.LegacySyncBridge;
import esq.phonemod.phone.sync.RuntimeLegacySyncBridge;
import esq.phonemod.operations.AssetHealthService;
import esq.phonemod.operations.OperationalMetrics;

public class SetupManager {
    private final PhoneMod plugin;
    private final AssetRegistryManager assetRegistryManager;
    private final ComponentRegistryManager componentRegistryManager;
    private final EventRegistryManager eventRegistryManager;
    private final CommandRegistryManager commandRegistryManager;
    private final InteractionSetupManager interactionSetupManager;
    private final RuntimeDevicePlatform devicePlatform;
    private final RuntimeLegacySyncBridge legacySyncBridge;
    private final AssetHealthService assetHealth = new AssetHealthService();


    public SetupManager(PhoneMod plugin) {
        this.plugin = plugin;
        DeviceService.initialize();
        PhoneService.initialize();
        this.devicePlatform = new RuntimeDevicePlatform();
        this.legacySyncBridge = new RuntimeLegacySyncBridge();
        LegacySyncBridge.install(legacySyncBridge);
        AppInstallService.initialize();
        AppCatalogService.initialize();
        this.assetRegistryManager = new AssetRegistryManager(plugin);
        this.componentRegistryManager = new ComponentRegistryManager(plugin);
        this.eventRegistryManager = new EventRegistryManager(plugin);
        this.commandRegistryManager = new CommandRegistryManager(plugin);
        this.interactionSetupManager = new InteractionSetupManager(plugin);
        this.start();

    }
    protected void start() {
        registerAssets();
        registerComponents();
        registerEvents();
        registerCommands();
        registerInteractions();
        registerApps();
        registerThemeListener();
        registerSyncTypes();
        registerSyncListeners();
    }

    /** Registers the channel types apps synchronize over. Must run before {@link #initializeRuntime}. */
    public void registerSyncTypes() {
        SyncService.registerType(GroupChatApp.CHATS);
    }

    /**
     * Auto-unsubscribes views from sync channels on app switch (DeviceAppCloseEvent)
     * and on device close (DeviceCloseEvent), so apps only ever call subscribe().
     */
    public void registerSyncListeners() {
        PhoneEvents.subscribe(DeviceAppCloseEvent.class, event ->
                SyncService.unsubscribeViewer(
                        new Viewer(event.getSession().getDeviceId(), event.getAppId())));
        PhoneEvents.subscribe(DeviceCloseEvent.class, event ->
                SyncService.unsubscribePhone(event.getSession().getDeviceId()));
    }

    /**
     * Re-applies theme colors to a live device page whenever its settings change,
     * so a theme switch in the Settings app takes effect immediately.
     */
    public void registerThemeListener() {
        PhoneEvents.subscribe(DeviceSettingsChangedEvent.class, event -> {
            PhoneRegistry.updatePreferences(
                    event.getSession().getDeviceId(), event.getNewSettings());
            DevicePageHandle handle = event.getSession().getPageHandle();
            String themeId = event.getNewSettings().getThemeId();
            if (themeId == null || themeId.isBlank()) {
                themeId = event.getSession().getShell().getDefaultThemeId();
            }
            if (handle != null && themeId != null && !themeId.isBlank()) {
                handle.reapplyTheme(themeId);
            }
        });
    }

    public void registerApps() {
        devicePlatform.apps().registerLegacyApp(plugin, "phonemod:whatgram", java.util.Set.of("whatgram"), new WhatgramApp());
        devicePlatform.apps().registerLegacyApp(plugin, "phonemod:contacts", java.util.Set.of("contacts"), new ContactsApp());
        devicePlatform.apps().registerLegacyApp(plugin, "phonemod:calls", java.util.Set.of("calls"), new CallsApp());
        devicePlatform.apps().registerLegacyApp(plugin, "phonemod:settings", java.util.Set.of("settings"), new SettingsApp());
        devicePlatform.apps().registerLegacyApp(plugin, "phonemod:appstore", java.util.Set.of("appstore"), new AppStoreApp());
        devicePlatform.apps().registerLegacyApp(plugin, "phonemod:groupchat", java.util.Set.of("groupchat"), new GroupChatApp());
    }
    public void registerAssets() {
        assetRegistryManager.register();
    }

    public void registerComponents() {
        componentRegistryManager.register();
    }

    public void registerEvents() {
        eventRegistryManager.register();
    }

    public void initializeRuntime() {
        devicePlatform.load();
        PhoneRegistry.initMailboxes();
        assetHealth.validate();
        componentRegistryManager.registerSystems();
        CallRegistry.startTimeoutWatcher();
        // Sync channels persist as JSON under the plugin data directory; bind the
        // store, then rehydrate persistent channels and clear stale ephemeral ones.
        SyncService.init(plugin.getDataDirectory());
        SyncService.loadAll();
        OperationalMetrics.get().bind(PhoneRegistry::activePresenceCount,
                CallRegistry::activeSessionCount, PhoneRegistry::mailboxDepth,
                SyncService::pendingSaves, devicePlatform.identities()::conflictCount);
    }

    public AssetHealthService.Report assetHealth() { return assetHealth.report(); }

    public void registerCommands() {
        commandRegistryManager.register();
    }

    public void registerInteractions() {
        interactionSetupManager.register();
    }

    public void shutdown() {
        CallRegistry.stopTimeoutWatcher();
        SyncService.flushAll();
        PhoneRegistry.flushMailboxes();
        eventRegistryManager.shutdown();
        devicePlatform.shutdown();
        LegacySyncBridge.uninstall(legacySyncBridge);
        PhoneEvents.unsubscribeAll(DeviceSettingsChangedEvent.class);
        PhoneEvents.unsubscribeAll(DeviceAppCloseEvent.class);
        PhoneEvents.unsubscribeAll(DeviceCloseEvent.class);
    }
}
