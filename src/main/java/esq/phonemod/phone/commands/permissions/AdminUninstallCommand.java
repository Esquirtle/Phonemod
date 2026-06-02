package esq.phonemod.phone.commands.permissions;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.phone.api.PhoneApp;
import esq.phonemod.phone.appstore.AppInstallService;
import esq.phonemod.phone.appstore.DeviceInstalledAppsComponent;
import esq.phonemod.phone.core.PhoneService;
import esq.phonemod.phone.messaging.PhoneRegistry;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * {@code /permissions uninstall <player> <appId>} — admin-uninstalls an app
 * from every active device of the target player.
 *
 * <p>System apps cannot be uninstalled even by admins.
 */
final class AdminUninstallCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> targetPlayerArg;
    private final RequiredArg<String> appIdArg;

    AdminUninstallCommand() {
        super("uninstall", "Admin-uninstall an app from a player's devices");
        this.targetPlayerArg = withRequiredArg("player", "Target player name", ArgTypes.STRING);
        this.appIdArg        = withRequiredArg("appId", "App ID to uninstall", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        String targetName = targetPlayerArg.get(context);
        String appId      = appIdArg.get(context);

        // Refuse to uninstall system apps.
        PhoneApp<?> app = PhoneService.get().getApp(appId);
        if (app != null && app.isSystemApp()) {
            context.sendMessage(Message.translation("phonemod.permissions.apps.uninstall.systemProtected")
                    .insert(appId));
            return;
        }

        PlayerRef targetRef = Universe.get().getPlayer(targetName, NameMatching.EXACT_IGNORE_CASE);
        if (targetRef == null) {
            context.sendMessage(Message.translation("phonemod.permissions.error.playerNotFound")
                    .insert(targetName));
            return;
        }

        List<String> devices = PhoneRegistry.getPhoneNumbersByUuid(targetRef.getUuid());
        if (devices.isEmpty()) {
            context.sendMessage(Message.translation("phonemod.permissions.apps.noDevices")
                    .insert(targetName));
            return;
        }

        PhoneRegistry.OnlineEntry entry = PhoneRegistry.getOnlineEntry(devices.get(0));
        if (entry == null) {
            context.sendMessage(Message.translation("phonemod.permissions.apps.noDevices")
                    .insert(targetName));
            return;
        }

        int uninstalled = 0;
        for (String deviceId : devices) {
            boolean ok = AppInstallService.get().uninstall(entry.ref(), entry.store(), deviceId, appId);
            if (ok) {
                uninstalled++;
            }
        }

        context.sendMessage(Message.translation("phonemod.permissions.apps.uninstall.success")
                .insert(appId).insert(targetName).insert(String.valueOf(uninstalled)));
    }
}
