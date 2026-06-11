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

/** {@code /permissions grant <player> <node>} */
final class GrantPermCommand extends CommandBase {

    private final RequiredArg<String> playerArg;
    private final RequiredArg<String> nodeArg;

    GrantPermCommand() {
        super("grant", "Grant a permission node directly to a player");
        this.playerArg = withRequiredArg("player", "Player name or UUID", ArgTypes.STRING);
        this.nodeArg   = withRequiredArg("node", "Permission node to grant", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        String playerInput = playerArg.get(context);
        String node        = nodeArg.get(context);
        UUID uuid = PermCommandUtil.resolveUuid(playerInput, context);
        if (uuid == null) {
            return;
        }
        PermissionsModule.get().addUserPermission(uuid, Set.of(node));
        context.sendMessage(Message.translation("phonemod.permissions.grant.success")
                .insert(node)
                .insert(playerInput));
    }
}
