package esq.phonemod;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import esq.phonemod.config.PhonemodConfig;
import esq.phonemod.setup.SetupManager;
import esq.phonemod.operations.AssetHealthService;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Phonemod - A Hytale server plugin providing an in-game phone device framework.
 *
 * @author esquirtle
 * @version 2.0.0-alpha.1
 */
public class PhoneMod extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static PhoneMod instance;
    private SetupManager setupManager;
    private final Config<PhonemodConfig> config;
    private final Object configSaveLock = new Object();
    private CompletableFuture<Void> pendingConfigSave = CompletableFuture.completedFuture(null);

    public PhoneMod(@Nonnull JavaPluginInit init) {
        super(init);
        this.config = withConfig(PhonemodConfig.CODEC);
        instance = this;
    }

    public static PhoneMod getInstance() {
        return instance;
    }

    public static PhonemodConfig config() {
        PhoneMod plugin = instance;
        if (plugin == null) {
            throw new IllegalStateException("Phonemod is not active");
        }
        return plugin.config.get();
    }

    public AssetHealthService.Report assetHealth() {
        return setupManager == null
                ? new AssetHealthService.Report(false, 0, java.util.List.of("plugin not started"))
                : setupManager.assetHealth();
    }

    /**
     * Persists runtime configuration edits in submission order. Encoding happens when the
     * queued save begins, so a burst of UI edits converges on the latest validated values
     * without allowing concurrent writes to race the same file.
     */
    @Nonnull
    public CompletableFuture<Void> saveConfig() {
        synchronized (configSaveLock) {
            pendingConfigSave = pendingConfigSave.handle((ignored, failure) -> null)
                    .thenCompose(ignored -> config.save());
            return pendingConfigSave;
        }
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
        synchronized (configSaveLock) {
            try {
                pendingConfigSave.join();
            } catch (RuntimeException error) {
                LOGGER.at(Level.SEVERE).withCause(error)
                        .log("[Phonemod] Configuration save did not complete cleanly");
            }
        }
        if (setupManager != null) {
            setupManager.shutdown();
        }
        instance = null;
    }
}
