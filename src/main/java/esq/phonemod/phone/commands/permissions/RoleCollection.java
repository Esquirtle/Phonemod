package esq.phonemod.phone.commands.permissions;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import esq.permission.service.RoleService;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

/**
 * {@code /permissions role} — sub-collection for role management.
 *
 * <pre>
 * /permissions role grant <player> <role>
 * /permissions role revoke <player> <role>
 * /permissions role list
 * /permissions role perms <role>
 * /permissions role addperm <role> <node>
 * /permissions role removeperm <role> <node>
 * </pre>
 */
final class RoleCollection extends AbstractCommandCollection {

    RoleCollection() {
        super("role", "Manage roles (permission groups)");
        addSubCommand(new GrantRoleCommand());
        addSubCommand(new RevokeRoleCommand());
        addSubCommand(new ListRolesCommand());
        addSubCommand(new RolePermsCommand());
        addSubCommand(new AddRolePermCommand());
        addSubCommand(new RemoveRolePermCommand());
    }

    // ── grant ────────────────────────────────────────────────────────────────

    private static final class GrantRoleCommand extends CommandBase {
        private final RequiredArg<String> playerArg;
        private final RequiredArg<String> roleArg;

        GrantRoleCommand() {
            super("grant", "Grant a role to a player");
            this.playerArg = withRequiredArg("player", "Player name or UUID", ArgTypes.STRING);
            this.roleArg   = withRequiredArg("role", "Role name", ArgTypes.STRING);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String playerInput = playerArg.get(context);
            String role        = roleArg.get(context);
            UUID uuid = PermCommandUtil.resolveUuid(playerInput, context);
            if (uuid == null) {
                return;
            }
            RoleService.get().grantRole(uuid, role);
            context.sendMessage(Message.translation("phonemod.permissions.role.grant.success")
                    .insert(role).insert(playerInput));
        }
    }

    // ── revoke ────────────────────────────────────────────────────────────────

    private static final class RevokeRoleCommand extends CommandBase {
        private final RequiredArg<String> playerArg;
        private final RequiredArg<String> roleArg;

        RevokeRoleCommand() {
            super("revoke", "Revoke a role from a player");
            this.playerArg = withRequiredArg("player", "Player name or UUID", ArgTypes.STRING);
            this.roleArg   = withRequiredArg("role", "Role name", ArgTypes.STRING);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String playerInput = playerArg.get(context);
            String role        = roleArg.get(context);
            UUID uuid = PermCommandUtil.resolveUuid(playerInput, context);
            if (uuid == null) {
                return;
            }
            RoleService.get().revokeRole(uuid, role);
            context.sendMessage(Message.translation("phonemod.permissions.role.revoke.success")
                    .insert(role).insert(playerInput));
        }
    }

    // ── list ──────────────────────────────────────────────────────────────────

    private static final class ListRolesCommand extends CommandBase {
        ListRolesCommand() {
            super("list", "List all known roles on this server");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            Set<String> roles = RoleService.get().getKnownRoles();
            context.sendMessage(Message.translation("phonemod.permissions.role.list.header"));
            if (roles.isEmpty()) {
                context.sendMessage(Message.translation("phonemod.permissions.none"));
            } else {
                context.sendMessage(Message.raw(String.join(", ", roles)));
            }
        }
    }

    // ── perms ─────────────────────────────────────────────────────────────────

    private static final class RolePermsCommand extends CommandBase {
        private final RequiredArg<String> roleArg;

        RolePermsCommand() {
            super("perms", "Show permission nodes granted to a role");
            this.roleArg = withRequiredArg("role", "Role name", ArgTypes.STRING);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String role = roleArg.get(context);
            Set<String> perms = com.hypixel.hytale.server.core.permissions.PermissionsModule
                    .get().getFirstPermissionProvider().getGroupPermissions(role);
            context.sendMessage(Message.translation("phonemod.permissions.role.perms.header")
                    .insert(role));
            context.sendMessage(perms.isEmpty()
                    ? Message.translation("phonemod.permissions.none")
                    : Message.raw(String.join(", ", perms)));
        }
    }

    // ── addperm ───────────────────────────────────────────────────────────────

    private static final class AddRolePermCommand extends CommandBase {
        private final RequiredArg<String> roleArg;
        private final RequiredArg<String> nodeArg;

        AddRolePermCommand() {
            super("addperm", "Add a permission node to a role");
            this.roleArg = withRequiredArg("role", "Role name", ArgTypes.STRING);
            this.nodeArg = withRequiredArg("node", "Permission node", ArgTypes.STRING);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String role = roleArg.get(context);
            String node = nodeArg.get(context);
            RoleService.get().addRolePermission(role, node);
            context.sendMessage(Message.translation("phonemod.permissions.role.addperm.success")
                    .insert(node).insert(role));
        }
    }

    // ── removeperm ────────────────────────────────────────────────────────────

    private static final class RemoveRolePermCommand extends CommandBase {
        private final RequiredArg<String> roleArg;
        private final RequiredArg<String> nodeArg;

        RemoveRolePermCommand() {
            super("removeperm", "Remove a permission node from a role");
            this.roleArg = withRequiredArg("role", "Role name", ArgTypes.STRING);
            this.nodeArg = withRequiredArg("node", "Permission node", ArgTypes.STRING);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String role = roleArg.get(context);
            String node = nodeArg.get(context);
            RoleService.get().removeRolePermission(role, node);
            context.sendMessage(Message.translation("phonemod.permissions.role.removeperm.success")
                    .insert(node).insert(role));
        }
    }
}
