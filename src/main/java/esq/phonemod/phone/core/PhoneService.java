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

    public void registerApp(@Nonnull PhoneApp<?> app) {
        this.appRegistry.register(app);
    }

    public PhoneApp<?> getApp(@Nonnull String appId) {
        return this.appRegistry.get(appId);
    }

    @Nonnull
    public List<PhoneApp<?>> getApps() {
        return this.appRegistry.getApps();
    }
}
