package esq.phonemod.setup;

import com.hypixel.hytale.logger.HytaleLogger;

import esq.phonemod.phone.commands.PhoneCommand;
import esq.phonemod.phone.commands.permissions.PermissionsCommand;
import esq.phonemod.PhoneMod;
import esq.phonemod.commands.DeviceFrameworkCommand;

public class CommandRegistryManager {
    
    private final PhoneMod plugin;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public CommandRegistryManager(PhoneMod plugin) {
        this.plugin = plugin;
    }

    public void register() {
        LOGGER.atInfo().log("Registering commands...");
        plugin.getCommandRegistry().registerCommand(new PhoneCommand());
        plugin.getCommandRegistry().registerCommand(new PermissionsCommand());
        plugin.getCommandRegistry().registerCommand(new DeviceFrameworkCommand());
        LOGGER.atWarning().log("/permissions is deprecated; use /deviceframework for Phonemod administration");
    }
}
