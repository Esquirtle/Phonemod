package esq.phonemod.phone.core;

import esq.phonemod.device.core.DeviceService;
import esq.phonemod.phone.api.PhoneApp;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Core phone platform service. Manages registered apps.
 *
 * <p>Page creation lives in the device framework ({@code DeviceService} /
 * {@code DevicePage}); the legacy {@code PhonePage} path has been removed.
 */
@Deprecated(since = "2.0", forRemoval = false)
public final class PhoneService {

    private static PhoneService instance;
    private final PhoneAppRegistry appRegistry;

    private PhoneService() {
        this.appRegistry = new PhoneAppRegistry();
    }

    public static void initialize() {
        DeviceService.initialize();
        if (instance == null) {
            instance = new PhoneService();
        }
    }

    @Nonnull
    public static PhoneService get() {
        if (instance == null) {
            throw new IllegalStateException("PhoneService must be initialized before use");
        }
        return instance;
    }

    /** Whether {@link #initialize()} has run. Lets host plugins tear down safely on shutdown. */
    public static boolean isInitialized() {
        return instance != null;
    }

    public void registerApp(@Nonnull PhoneApp<?> app) {
        this.appRegistry.register(app);
    }

    public void registerApp(@Nonnull PhoneApp<?> app, @Nonnull java.util.Set<String> aliases) {
        this.appRegistry.register(app, aliases);
    }

    /** Reverses {@link #registerApp}. Host plugins call this on shutdown to avoid stale apps on reload. */
    public void unregisterApp(@Nonnull String appId) {
        this.appRegistry.unregister(appId);
    }

    public PhoneApp<?> getApp(@Nonnull String appId) {
        return this.appRegistry.get(appId);
    }

    @Nonnull
    public List<PhoneApp<?>> getApps() {
        return this.appRegistry.getApps();
    }
}
