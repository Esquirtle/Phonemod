package esq.phonemod;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import esq.phonemod.setup.SetupManager;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * Phonemod - A Hytale server plugin providing an in-game phone device framework.
 *
 * @author esquirtle
 * @version 1.0.0
 */
public class PhoneMod extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static PhoneMod instance;
    private SetupManager setupManager;

    public PhoneMod(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static PhoneMod getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        LOGGER.at(Level.INFO).log("[Phonemod] Setting up...");
        this.setupManager = new SetupManager(this);
        LOGGER.at(Level.INFO).log("[Phonemod] Setup complete!");
    }

    @Override
    protected void start() {
        if (setupManager != null) {
            setupManager.initializeRuntime();
        }
        LOGGER.at(Level.INFO).log("[Phonemod] Started!");
        LOGGER.at(Level.INFO).log("=======================================");
        LOGGER.at(Level.INFO).log("=  ***    Powered by Phonemod    ***  =");
        LOGGER.at(Level.INFO).log("=======================================");
    }

    @Override
    protected void shutdown() {
        LOGGER.at(Level.INFO).log("[Phonemod] Shutting down...");
        if (setupManager != null) {
            setupManager.shutdown();
        }
        instance = null;
    }
}