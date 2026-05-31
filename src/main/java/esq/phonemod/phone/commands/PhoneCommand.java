package esq.phonemod.phone.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/**
 * Root command {@code /phonemod} for Phonemod admin/debug utilities.
 *
 * <p>Subcommands:
 * <ul>
 *   <li>{@code sendtext <number> <message>} — send a text as [Server]</li>
 * </ul>
 */
public final class PhoneCommand extends AbstractCommandCollection {

    public PhoneCommand() {
        super("phonemod", "Phonemod admin/debug commands");
        addSubCommand(new SendTextCommand());
    }
}
