package esq.phonemod.setup;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.CommandRegistry;

import esq.phonemod.phone.commands.PhoneCommand;
import esq.phonemod.PhoneMod;

public class CommandRegistryManager {
    
    private final PhoneMod plugin;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public CommandRegistryManager(PhoneMod plugin) {
        this.plugin = plugin;
    }

    public void register() {
        LOGGER.atInfo().log("Registering commands...");
        plugin.getCommandRegistry().registerCommand(new PhoneCommand());
    }

    private void registerFactionCommands(CommandRegistry registry) {

    }
}
