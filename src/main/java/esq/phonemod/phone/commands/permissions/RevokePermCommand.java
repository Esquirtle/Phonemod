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

/** {@code /permissions revoke <player> <node>} */
final class RevokePermCommand extends CommandBase {

    private final RequiredArg<String> playerArg;
    private final RequiredArg<String> nodeArg;

    RevokePermCommand() {
        super("revoke", "Revoke a direct permission node from a player");
        this.playerArg = withRequiredArg("player", "Player name or UUID", ArgTypes.STRING);
        this.nodeArg   = withRequiredArg("node", "Permission node to revoke", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        String playerInput = playerArg.get(context);
        String node        = nodeArg.get(context);
        UUID uuid = PermCommandUtil.resolveUuid(playerInput, context);
        if (uuid == null) {
            return;
        }
        PermissionsModule.get().removeUserPermission(uuid, Set.of(node));
        context.sendMessage(Message.translation("phonemod.permissions.revoke.success")
                .insert(node)
                .insert(playerInput));
    }
}
