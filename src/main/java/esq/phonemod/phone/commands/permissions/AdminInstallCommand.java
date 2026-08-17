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
import esq.phonemod.phone.appstore.AppInstallService;
import esq.phonemod.phone.messaging.PhoneRegistry;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * {@code /permissions install <player> <appId>} — admin-installs an app onto
 * every active device of the target player, bypassing the permission gate.
 *
 * <p>Uses {@link AppInstallService#install} but the admin already holds
 * {@code phonemod.admin.permissions} which means they are trusted to bypass
 * the permission check. We use {@code AppInstallService.install} with the
 * target player's UUID so the check is correct — or we bypass by granting
 * the perm first. For simplicity, admin force-install installs unconditionally
 * by writing directly to the component.
 */
final class AdminInstallCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> targetPlayerArg;
    private final RequiredArg<String> appIdArg;

    AdminInstallCommand() {
        super("install", "Admin-install an app onto a player's devices");
        this.targetPlayerArg = withRequiredArg("player", "Target player name", ArgTypes.STRING);
        this.appIdArg        = withRequiredArg("appId", "App ID to install", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        String targetName = targetPlayerArg.get(context);
        String appId      = appIdArg.get(context);

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

        PhoneRegistry.PresenceEntry entry = PhoneRegistry.getPresenceEntry(devices.get(0));
        if (entry == null) {
            context.sendMessage(Message.translation("phonemod.permissions.apps.noDevices")
                    .insert(targetName));
            return;
        }

        entry.world().execute(() -> {
            if (!entry.ref().isValid()) return;
            int installed = 0;
            for (String deviceId : devices) {
                esq.phonemod.phone.appstore.DeviceInstalledAppsComponent comp =
                        entry.store().ensureAndGetComponent(entry.ref(),
                                esq.phonemod.phone.appstore.DeviceInstalledAppsComponent
                                        .getComponentType());
                comp.install(deviceId, appId);
                entry.store().putComponent(entry.ref(),
                        esq.phonemod.phone.appstore.DeviceInstalledAppsComponent.getComponentType(), comp);
                installed++;
            }
            context.sendMessage(Message.translation("phonemod.permissions.apps.install.success")
                    .insert(appId).insert(targetName).insert(String.valueOf(installed)));
        });
    }
}
