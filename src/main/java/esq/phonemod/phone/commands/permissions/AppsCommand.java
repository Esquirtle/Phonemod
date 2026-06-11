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
import esq.phonemod.phone.appstore.DeviceInstalledAppsComponent;
import esq.phonemod.phone.messaging.PhoneRegistry;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@code /permissions apps <player>} — lists the apps installed on each of a
 * player's active devices.
 */
final class AppsCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> targetPlayerArg;

    AppsCommand() {
        super("apps", "List installed apps for a player's active devices");
        this.targetPlayerArg = withRequiredArg("player", "Target player name", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        String targetName = targetPlayerArg.get(context);

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

        // We need an ECS store reference for the target; retrieve it from their first online entry.
        PhoneRegistry.OnlineEntry entry = PhoneRegistry.getOnlineEntry(devices.get(0));
        if (entry == null) {
            context.sendMessage(Message.translation("phonemod.permissions.apps.noDevices")
                    .insert(targetName));
            return;
        }

        context.sendMessage(Message.translation("phonemod.permissions.apps.header")
                .insert(targetName));
        for (String deviceId : devices) {
            DeviceInstalledAppsComponent comp = entry.store().getComponent(
                    entry.ref(), DeviceInstalledAppsComponent.getComponentType());
            Set<String> installed = comp != null ? comp.getInstalled(deviceId)
                    : Collections.emptySet();
            String appList = installed.stream()
                    .filter(id -> !"__seeded__".equals(id))
                    .sorted()
                    .collect(Collectors.joining(", "));
            context.sendMessage(Message.raw("  " + deviceId + ": "
                    + (appList.isEmpty() ? "(none)" : appList)));
        }
    }
}
