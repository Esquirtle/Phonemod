package esq.phonemod.phone.commands.permissions;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

/**
 * {@code /permissions player <player>} — shows a player's roles (groups) and
 * direct permission nodes.
 */
final class PlayerInfoCommand extends CommandBase {

    private final RequiredArg<String> playerArg;

    PlayerInfoCommand() {
        super("player", "Show a player's groups and direct permissions");
        this.playerArg = withRequiredArg("player", "Player name or UUID", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        String playerInput = playerArg.get(context);
        UUID uuid = PermCommandUtil.resolveUuid(playerInput, context);
        if (uuid == null) {
            return;
        }

        PermissionsModule perms = PermissionsModule.get();
        Set<String> groups = perms.getGroupsForUser(uuid);
        Set<String> direct = getDirectPermissions(uuid, perms);

        context.sendMessage(Message.translation("phonemod.permissions.player.header")
                .insert(playerInput));
        context.sendMessage(Message.translation("phonemod.permissions.player.groups")
                .insert(groups.isEmpty() ? Message.translation("phonemod.permissions.none")
                        : Message.raw(String.join(", ", groups))));
        context.sendMessage(Message.translation("phonemod.permissions.player.direct")
                .insert(direct.isEmpty() ? Message.translation("phonemod.permissions.none")
                        : Message.raw(String.join(", ", direct))));
    }

    private Set<String> getDirectPermissions(UUID uuid, PermissionsModule perms) {
        // PermissionsModule delegates getUserPermissions to the first provider.
        return perms.getFirstPermissionProvider().getUserPermissions(uuid);
    }

}
