package esq.phonemod.telemetry;

import com.hypixel.hytale.logger.HytaleLogger;
import esq.phonemod.PhoneMod;
import esq.phonemod.config.PhonemodConfig;
import esq.phonemod.platform.RuntimeDevicePlatform;
import esq.telemetry.Telemetry;
import esq.telemetry.TelemetryStatus;
import esq.telemetry.hytale.HytaleTelemetry;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Wires one {@link Telemetry} instance for {@code esq.phonemod:Phonemod} (esq-telemetry
 * integration; same shape as PinaCore-RP's {@code PinaTelemetryService}). Constructed as the
 * first action of {@link PhoneMod#setup()}, before {@code SetupManager} builds anything, so
 * it can capture errors from the rest of setup; closed as the last action of
 * {@link PhoneMod#shutdown()}.
 */
public final class PhonemodTelemetryService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Telemetry telemetry;

    public PhonemodTelemetryService(@Nonnull PhoneMod plugin, @Nonnull PhonemodConfig config) {
        this.telemetry = HytaleTelemetry.start(plugin, config.telemetry(), this::snapshotUsage);
        printConsoleNoticeIfPending();
    }

    public boolean isEnabled() {
        return telemetry.isEnabled();
    }

    public boolean isNoop() {
        return telemetry.isNoop();
    }

    public TelemetryStatus status() {
        return telemetry.status();
    }

    public String previewJson() {
        return telemetry.previewJson();
    }

    public enum EnableOutcome { LIVE, PENDING_RESTART }

    public EnableOutcome setEnabled(boolean on) {
        EnableOutcome outcome = (on && telemetry.isNoop()) ? EnableOutcome.PENDING_RESTART : EnableOutcome.LIVE;
        telemetry.setEnabled(on);
        PhoneMod.config().telemetry().setEnabled(on);
        PhoneMod.getInstance().saveConfig().exceptionally(error -> {
            LOGGER.at(Level.SEVERE).withCause(error).log("[esq-telemetry] could not write config.json");
            return null;
        });
        return outcome;
    }

    public void close() {
        telemetry.close();
    }

    private void printConsoleNoticeIfPending() {
        if (!telemetry.isEnabled()) return;
        if (telemetry.notices().pending("console")) {
            var participants = telemetry.notices().participants();
            String plugins = participants.isEmpty() ? "esq.phonemod:Phonemod" : String.join(", ", participants);
            String installId = telemetry.status().installId();
            LOGGER.at(Level.INFO).log(
                "[esq-telemetry] esq telemetry is ON for: %s (install id: %s). It sends error "
                    + "fingerprints (type + code location, sanitised message, no player identity), "
                    + "usage counts and module ids -- never player names, chat, messages or call "
                    + "content. First send is delayed 10 minutes so you can read this. Disable any "
                    + "time with /deviceframework telemetry disable.",
                plugins, installId);
            telemetry.notices().acknowledge("console");
        } else {
            LOGGER.at(Level.INFO).log("[esq-telemetry] esq telemetry is enabled (/deviceframework telemetry status for details)");
        }
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
