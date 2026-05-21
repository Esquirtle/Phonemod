package esq.phonemod.setup;

import esq.phonemod.PhoneMod;

public class SetupManager {
    private final AssetRegistryManager assetRegistryManager;
    private final ComponentRegistryManager componentRegistryManager;
    private final EventRegistryManager eventRegistryManager;
    private final CommandRegistryManager commandRegistryManager;
    private final InteractionSetupManager interactionSetupManager;


    public SetupManager(PhoneMod plugin) {
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
