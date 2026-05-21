package esq.phonemod.setup;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;

import esq.phonemod.PhoneMod;
import esq.phonemod.phone.interactions.OpenPhone;

public class InteractionSetupManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final PhoneMod plugin;

    public InteractionSetupManager(PhoneMod plugin) {
        this.plugin = plugin;
    }

    public void register() {
        try {
            LOGGER.atInfo().log("Registering interactions...");
            plugin.getCodecRegistry(Interaction.CODEC)
                    .register("open_phone", OpenPhone.class, OpenPhone.CODEC);
        } catch (Exception e) {
            LOGGER.atSevere().log("Failed to register interactions!");
        }

    }
}