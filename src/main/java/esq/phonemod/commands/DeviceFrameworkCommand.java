package esq.phonemod.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import esq.phonemod.api.DevicePlatform;
import esq.phonemod.device.assets.DeviceAsset;
import esq.phonemod.identity.IdentityRegistry;
import esq.phonemod.platform.RuntimeDevicePlatform;
import esq.phonemod.PhoneMod;
import esq.phonemod.operations.OperationalMetrics;
import esq.phonemod.phone.sync.SyncService;
import esq.phonemod.telemetry.PhonemodTelemetryService;
import esq.telemetry.TelemetryStatus;

import javax.annotation.Nonnull;
import java.util.UUID;

/** Supported v2 operational and migration control surface. */
public final class DeviceFrameworkCommand extends AbstractCommandCollection {
    public DeviceFrameworkCommand() {
        super("deviceframework", "Device platform status, migration, repair, and ownership controls");
        requirePermission("phonemod.admin.deviceframework");
        addSubCommand(new Status());
        addSubCommand(new Migration());
        addSubCommand(new Transfer());
        addSubCommand(new Release());
        addSubCommand(new TelemetryCommand());
    }

    /** {@code /deviceframework telemetry status|preview|enable|disable} — esq-telemetry controls. */
    private static final class TelemetryCommand extends AbstractCommandCollection {
        TelemetryCommand() {
            super("telemetry", "esq-telemetry status and controls");
            addSubCommand(new TelemetryStatusCmd());
            addSubCommand(new TelemetryPreviewCmd());
            addSubCommand(new TelemetryToggleCmd(true));
            addSubCommand(new TelemetryToggleCmd(false));
        }
    }

    private static final class TelemetryStatusCmd extends CommandBase {
        TelemetryStatusCmd() { super("status", "Shows whether esq telemetry is enabled"); }
        protected void executeSync(@Nonnull CommandContext context) {
            PhonemodTelemetryService telemetry = PhoneMod.getInstance().getTelemetryService();
            String state = telemetry.isEnabled() ? "ON"
                    : (telemetry.isNoop() && PhoneMod.config().telemetry().isEnabled()
                        ? "set to ON (needs a restart)" : "OFF");
            context.sendMessage(Message.raw("esq telemetry is " + state + "."));
            TelemetryStatus status = telemetry.status();
            if (!status.noop()) {
                context.sendMessage(Message.raw("installId=" + status.installId()
                        + " lastSend=" + (status.lastSendAttemptAt() != null ? status.lastSendAttemptAt() : "never")
                        + " lastResult=" + (status.lastSendResult() != null ? status.lastSendResult() : "none")
                        + " nextSend=" + (status.nextSendAt() != null ? status.nextSendAt() : "never")));
            }
        }
    }

    private static final class TelemetryPreviewCmd extends CommandBase {
        TelemetryPreviewCmd() { super("preview", "Shows the next telemetry batch without sending it"); }
        protected void executeSync(@Nonnull CommandContext context) {
            context.sendMessage(Message.raw(PhoneMod.getInstance().getTelemetryService().previewJson()));
        }
    }

    private static final class TelemetryToggleCmd extends CommandBase {
        private final boolean enable;
        TelemetryToggleCmd(boolean enable) {
            super(enable ? "enable" : "disable", "Turns esq telemetry " + (enable ? "on" : "off") + " (hot toggle, no restart)");
            this.enable = enable;
        }
        protected void executeSync(@Nonnull CommandContext context) {
            var outcome = PhoneMod.getInstance().getTelemetryService().setEnabled(enable);
            String result = !enable ? "esq telemetry disabled."
                    : (outcome == PhonemodTelemetryService.EnableOutcome.PENDING_RESTART
                        ? "esq telemetry set to enabled; takes effect after a restart."
                        : "esq telemetry enabled.");
            context.sendMessage(Message.raw(result));
        }
    }

    private static final class Status extends CommandBase {
        Status(){super("status","Show device platform health");}
        protected void executeSync(@Nonnull CommandContext context) {
            RuntimeDevicePlatform platform = RuntimeDevicePlatform.get();
            context.sendMessage(Message.raw("Phonemod API 2.0.0-alpha.1"));
            context.sendMessage(Message.raw("Apps: " + platform.apps().snapshot().size()
                    + " | Devices: " + platform.identities().snapshot().size()));
            int pendingSaves = platform.identities().pendingSaves() + SyncService.pendingSaves();
            context.sendMessage(Message.raw("Pending saves: " + pendingSaves
                    + " | Migration conflicts: " + platform.identities().conflictCount()));
            var metrics = OperationalMetrics.get().snapshot();
            context.sendMessage(Message.raw("Sessions: " + metrics.activeSessions()
                    + " | Calls: " + metrics.activeCalls() + " | Mailbox: " + metrics.mailboxDepth()));
            context.sendMessage(Message.raw("Rejected: " + metrics.rejectedActions()
                    + " | Rate limits: " + metrics.rateLimits() + " | Save failures: " + metrics.saveFailures()));
            var health = PhoneMod.getInstance().assetHealth();
            context.sendMessage(Message.raw("Device assets: " + DeviceAsset.getAssetMap().getAssetMap().size()
                    + " | Asset health: " + (health.healthy() ? "healthy" : "unhealthy")));
        }
    }

    private static final class Migration extends AbstractCommandCollection {
        Migration(){super("migration","Inspect or repair v1 to v2 migration state");
            addSubCommand(new DryRun()); addSubCommand(new MigrationStatus());
            addSubCommand(new Conflicts()); addSubCommand(new Repair());}
    }
    private static final class DryRun extends CommandBase {
        DryRun(){super("dry-run","Report migration work without mutation");}
        protected void executeSync(CommandContext c){ IdentityRegistry r=registry();
            c.sendMessage(Message.raw("Dry run: " + r.snapshot().size() + " identities, "
                    + r.conflictCount() + " known conflicts; no data changed.")); }
    }
    private static final class MigrationStatus extends CommandBase {
        MigrationStatus(){super("status","Show migration state");}
        protected void executeSync(CommandContext c){ IdentityRegistry r=registry();
            c.sendMessage(Message.raw("Schema: v2 | identities=" + r.snapshot().size()
                    + " conflicts=" + r.conflictCount() + " pendingSaves=" + r.pendingSaves())); }
    }
    private static final class Conflicts extends CommandBase {
        Conflicts(){super("conflicts","Show migration conflict count");}
        protected void executeSync(CommandContext c){c.sendMessage(Message.raw(
                "Migration conflicts: " + registry().conflictCount()));}
    }
    private static final class Repair extends CommandBase {
        Repair(){super("repair","Repair duplicate address index entries",true);}
        protected void executeSync(CommandContext c){c.sendMessage(Message.raw(
                "Repaired " + registry().repairAddressConflicts() + " identity conflict(s)."));}
    }
    private static final class Transfer extends CommandBase {
        private final RequiredArg<String> id; private final RequiredArg<String> owner;
        Transfer(){super("transfer","Transfer a personal device identity",true);
            id=withRequiredArg("identity","Stable device UUID",ArgTypes.STRING);
            owner=withRequiredArg("newOwner","New owner UUID",ArgTypes.STRING);}
        protected void executeSync(CommandContext c){try{
            boolean ok=registry().forceTransfer(UUID.fromString(id.get(c)),UUID.fromString(owner.get(c)));
            c.sendMessage(Message.raw(ok?"Device ownership transferred.":"Identity not found or shared."));
        }catch(IllegalArgumentException e){c.sendMessage(Message.raw("Invalid UUID."));}}
    }
    private static final class Release extends CommandBase {
        private final RequiredArg<String> id;
        Release(){super("release","Release a device address",true);
            id=withRequiredArg("identity","Stable device UUID",ArgTypes.STRING);}
        protected void executeSync(CommandContext c){try{
            boolean ok=registry().forceReleaseAddress(UUID.fromString(id.get(c)));
            c.sendMessage(Message.raw(ok?"Device address released.":"Identity not found."));
        }catch(IllegalArgumentException e){c.sendMessage(Message.raw("Invalid UUID."));}}
    }
    private static IdentityRegistry registry(){return RuntimeDevicePlatform.get().identities();}
}
