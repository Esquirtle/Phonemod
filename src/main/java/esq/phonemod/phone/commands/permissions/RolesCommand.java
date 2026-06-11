package esq.phonemod.phone.commands.permissions;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import esq.permission.service.RoleService;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

/** {@code /permissions roles <player>} — lists all roles assigned to a player. */
final class RolesCommand extends CommandBase {

    private final RequiredArg<String> playerArg;

    RolesCommand() {
        super("roles", "List all roles assigned to a player");
        this.playerArg = withRequiredArg("player", "Player name or UUID", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        String playerInput = playerArg.get(context);
        UUID uuid = PermCommandUtil.resolveUuid(playerInput, context);
        if (uuid == null) {
            return;
        }
        Set<String> roles = RoleService.get().getRoles(uuid);
        context.sendMessage(Message.translation("phonemod.permissions.roles.list")
                .insert(playerInput)
                .insert(roles.isEmpty()
                        ? Message.translation("phonemod.permissions.none")
                        : Message.raw(String.join(", ", roles))));
    }
}
