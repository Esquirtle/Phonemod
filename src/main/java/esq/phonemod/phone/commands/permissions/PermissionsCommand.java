package esq.phonemod.phone.commands.permissions;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/**
 * Root command {@code /permissions} for the Phonemod permission framework.
 *
 * <p>Subcommand tree:
 * <pre>
 * /permissions
 *   player   — show a player's direct permissions and groups
 *   check    — check whether a player holds a specific node
 *   grant    — grant a permission node directly to a player
 *   revoke   — revoke a direct permission node from a player
 *   roles    — show a player's roles (groups)
 *   role     — manage roles (grant/revoke for players; list/perms/addperm/removeperm)
 *   apps     — show app install status for a player
 *   install  — admin-install an app onto a player's device
 *   uninstall — admin-uninstall an app from a player's device
 * </pre>
 *
 * <p>The root command requires {@code phonemod.admin.permissions}. All
 * subcommands inherit this permission gate.
 */
public final class PermissionsCommand extends AbstractCommandCollection {

    public PermissionsCommand() {
        super("permissions", "Phonemod permission framework admin commands");
        requirePermission("phonemod.admin.permissions");

        addSubCommand(new PlayerInfoCommand());
        addSubCommand(new CheckPermCommand());
        addSubCommand(new GrantPermCommand());
        addSubCommand(new RevokePermCommand());
        addSubCommand(new RolesCommand());
        addSubCommand(new RoleCollection());
        addSubCommand(new AppsCommand());
        addSubCommand(new AdminInstallCommand());
        addSubCommand(new AdminUninstallCommand());
    }
}
