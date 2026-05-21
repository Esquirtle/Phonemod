package esq.phonemod.phone.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/**
 * Root command {@code /pg} for Playground plugin test utilities.
 *
 * <p>Subcommands:
 * <ul>
 *   <li>{@code sendtext <number> <message>} — send a text as [Server]</li>
 * </ul>
 */
public final class PhoneCommand extends AbstractCommandCollection {

    public PhoneCommand() {
        super("pg", "Playground test commands");
        addSubCommand(new SendTextCommand());
    }
}
