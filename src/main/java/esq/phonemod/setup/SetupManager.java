package esq.phonemod.setup;

import esq.phonemod.PhoneMod;
import esq.phonemod.device.api.DevicePageHandle;
import esq.phonemod.device.core.DeviceService;
import esq.phonemod.device.events.DeviceAppCloseEvent;
import esq.phonemod.device.events.DeviceCloseEvent;
import esq.phonemod.device.events.DeviceSettingsChangedEvent;
import esq.phonemod.device.events.PhoneEvents;
import esq.permission.api.AppPermissionService;
import esq.permission.service.RoleService;
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
import esq.phonemod.phone.sync.SyncService;
import esq.phonemod.phone.sync.Viewer;

public class SetupManager {
    private final PhoneMod plugin;
    private final AssetRegistryManager assetRegistryManager;
    private final ComponentRegistryManager componentRegistryManager;
    private final EventRegistryManager eventRegistryManager;
    private final CommandRegistryManager commandRegistryManager;
    private final InteractionSetupManager interactionSetupManager;


    public SetupManager(PhoneMod plugin) {
        this.plugin = plugin;
        DeviceService.initialize();
        PhoneService.initialize();
        AppPermissionService.initialize();
        RoleService.initialize();
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
            DevicePageHandle handle = event.getSession().getPageHandle();
            String themeId = event.getNewSettings().getThemeId();
            if (handle != null && themeId != null && !themeId.isBlank()) {
                handle.reapplyTheme(themeId);
            }
        });
    }

    public void registerApps() {
        PhoneService.get().registerApp(new WhatgramApp());
        PhoneService.get().registerApp(new ContactsApp());
        PhoneService.get().registerApp(new CallsApp());
        PhoneService.get().registerApp(new SettingsApp());
        PhoneService.get().registerApp(new AppStoreApp());
        PhoneService.get().registerApp(new GroupChatApp());
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
        componentRegistryManager.registerSystems();
        CallRegistry.startTimeoutWatcher();
        // Sync channels persist as JSON under the plugin data directory; bind the
        // store, then rehydrate persistent channels and clear stale ephemeral ones.
        SyncService.init(plugin.getDataDirectory());
        SyncService.loadAll();
    }

    public void registerCommands() {
        commandRegistryManager.register();
    }

    public void registerInteractions() {
        interactionSetupManager.register();
    }

    public void shutdown() {
        CallRegistry.stopTimeoutWatcher();
        SyncService.flushAll();
        PhoneEvents.unsubscribeAll(DeviceSettingsChangedEvent.class);
        PhoneEvents.unsubscribeAll(DeviceAppCloseEvent.class);
        PhoneEvents.unsubscribeAll(DeviceCloseEvent.class);
    }
}
