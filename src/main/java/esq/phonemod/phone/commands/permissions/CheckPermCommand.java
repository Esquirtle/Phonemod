package esq.phonemod.phone.commands.permissions;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;

import javax.annotation.Nonnull;
import java.util.UUID;

/** {@code /permissions check <player> <node>} */
final class CheckPermCommand extends CommandBase {

    private final RequiredArg<String> playerArg;
    private final RequiredArg<String> nodeArg;

    CheckPermCommand() {
        super("check", "Check whether a player holds a specific permission node");
        this.playerArg = withRequiredArg("player", "Player name or UUID", ArgTypes.STRING);
        this.nodeArg   = withRequiredArg("node", "Permission node", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        String playerInput = playerArg.get(context);
        String node        = nodeArg.get(context);
        UUID uuid = PermCommandUtil.resolveUuid(playerInput, context);
        if (uuid == null) {
            return;
        }
        boolean has = PermissionsModule.get().hasPermission(uuid, node);
        String key = has
                ? "phonemod.permissions.check.has"
                : "phonemod.permissions.check.lacks";
        context.sendMessage(Message.translation(key)
                .insert(playerInput)
                .insert(node));
    }
}
