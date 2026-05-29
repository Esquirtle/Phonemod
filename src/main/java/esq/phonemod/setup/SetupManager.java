package esq.phonemod.setup;

import esq.phonemod.PhoneMod;
import esq.phonemod.device.core.DeviceService;
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
    }

    public void registerApps() {
        PhoneService.get().registerApp(new WhatgramApp());
        PhoneService.get().registerApp(new ContactsApp());
        PhoneService.get().registerApp(new CallsApp());
        PhoneService.get().registerApp(new SettingsApp());
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

    }
}
