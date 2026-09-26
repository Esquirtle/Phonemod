package esq.phonemod.telemetry;

import esq.phonemod.PhoneMod;
import esq.phonemod.config.PhonemodConfig;
import esq.phonemod.platform.RuntimeDevicePlatform;
import esq.telemetry.Telemetry;
import esq.telemetry.hytale.HytaleTelemetry;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wires one {@link Telemetry} instance for {@code esq.phonemod:Phonemod} (esq-telemetry
 * integration; library 1.1.0). Constructed as the first action of {@link PhoneMod#setup()},
 * before {@code SetupManager} builds anything, so it can capture errors from the rest of
 * setup; closed as the last action of {@link PhoneMod#shutdown()}.
 *
 * <p>Unified telemetry (D-255/D-256): the shared {@code /telemetry} command/page, console
 * notice, and enable/disable toggle now live entirely in {@code esq-telemetry} 1.1.0
 * ({@code esq.telemetry.hytale.HytaleTelemetry}/{@code TelemetryHub}) — this class only
 * starts the shared {@link Telemetry} instance and exposes the usage snapshot this plugin owns.
 */
public final class PhonemodTelemetryService {
    private final Telemetry telemetry;

    public PhonemodTelemetryService(@Nonnull PhoneMod plugin, @Nonnull PhonemodConfig config) {
        this.telemetry = HytaleTelemetry.start(plugin, config.telemetry(), this::snapshotUsage);
    }

    public void close() {
        telemetry.close();
    }

    /**
     * Sampled on the telemetry thread only — D-179: no player identity, no messages, no call
     * content. {@link RuntimeDevicePlatform#apps()}/{@link RuntimeDevicePlatform#identities()}
     * are plain in-memory registries (same ones {@code DeviceFrameworkCommand}'s own
     * {@code status} subcommand reads synchronously), never the ECS Store.
     */
    private Map<String, Object> snapshotUsage() {
        Map<String, Object> usage = new LinkedHashMap<>();
        try {
            RuntimeDevicePlatform platform = RuntimeDevicePlatform.get();
            usage.put("installedApps", platform.apps().snapshot().size());
            usage.put("activeDevices", platform.identities().snapshot().size());
        } catch (Throwable ignored) {
            // Platform not ready yet (very early boot) -- an empty usage map is fine.
        }
        return usage;
    }
}
