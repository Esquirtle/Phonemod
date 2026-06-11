package esq.phonemod.phone.commands.permissions;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Shared utilities for the {@code /permissions} command tree.
 */
final class PermCommandUtil {

    private PermCommandUtil() {
    }

    /**
     * Resolves a player UUID from a name string. Tries UUID parse first,
     * then name lookup. Sends an error and returns {@code null} if not found.
     */
    @Nullable
    static UUID resolveUuid(@Nonnull String input, @Nonnull CommandContext context) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException ignored) {
            // fall through to name lookup
        }
        PlayerRef pr = Universe.get().getPlayer(input, NameMatching.EXACT_IGNORE_CASE);
        if (pr == null) {
            context.sendMessage(Message.translation("phonemod.permissions.error.playerNotFound")
                    .insert(input));
            return null;
        }
        return pr.getUuid();
    }
}
