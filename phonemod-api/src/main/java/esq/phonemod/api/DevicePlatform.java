package esq.phonemod.api;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Runtime locator for the single enabled Phonemod device platform. */
public interface DevicePlatform {
    AtomicReference<DevicePlatform> LOCATOR = new AtomicReference<>();

    @Nonnull AppService apps();
    @Nonnull DeviceService devices();
    @Nonnull PresenceService presence();
    @Nonnull MessagingService messaging();
    @Nonnull CallingService calling();
    @Nonnull NotificationService notifications();
    @Nonnull SyncService sync();

    static Optional<DevicePlatform> find() { return Optional.ofNullable(LOCATOR.get()); }
    static DevicePlatform require() {
        return find().orElseThrow(() -> new IllegalStateException("Phonemod device platform is not enabled"));
    }

    /** Host lifecycle hook; third-party plugins must not call this method. */
    static void install(@Nonnull DevicePlatform platform) {
        if (!LOCATOR.compareAndSet(null, platform))
            throw new IllegalStateException("A device platform is already installed");
    }

    /** Host lifecycle hook; only clears the exact installed instance. */
    static void uninstall(@Nonnull DevicePlatform platform) { LOCATOR.compareAndSet(platform, null); }
}
