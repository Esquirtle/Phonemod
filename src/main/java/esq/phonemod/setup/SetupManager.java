package esq.phonemod.setup;

import esq.phonemod.PhoneMod;
import esq.phonemod.device.api.DevicePageHandle;
import esq.phonemod.device.core.DeviceService;
import esq.phonemod.device.events.DeviceSettingsChangedEvent;
import esq.phonemod.device.events.PhoneEvents;
import esq.phonemod.phone.apps.AppStoreApp;
import esq.phonemod.phone.apps.CallsApp;
import esq.phonemod.phone.apps.ContactsApp;
import esq.phonemod.phone.apps.SettingsApp;
import esq.phonemod.phone.apps.WhatgramApp;
import esq.phonemod.phone.core.PhoneService;

public class SetupManager {
    private final AssetRegistryManager assetRegistryManager;
    private final ComponentRegistryManager componentRegistryManager;
    private final EventRegistryManager eventRegistryManager;
    private final CommandRegistryManager commandRegistryManager;
    private final InteractionSetupManager interactionSetupManager;


    public SetupManager(PhoneMod plugin) {
        DeviceService.initialize();
        PhoneService.initialize();
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
        registerCommands();
        registerInteractions();
        registerApps();
        registerThemeListener();
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
    }
    public void registerAssets() {
        assetRegistryManager.register();
    }

    public void registerComponents() {
        componentRegistryManager.register();
        eventRegistryManager.register();
    }

    public void initializeRuntime() {
        componentRegistryManager.registerSystems();
    }

    public void registerCommands() {
        commandRegistryManager.register();
    }

    public void registerInteractions() {
        interactionSetupManager.register();
    }

    public void shutdown() {
        PhoneEvents.unsubscribeAll(DeviceSettingsChangedEvent.class);
    }
}
