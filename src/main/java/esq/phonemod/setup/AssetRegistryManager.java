package esq.phonemod.setup;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;

import esq.phonemod.device.assets.DeviceAsset;
import esq.phonemod.device.assets.DeviceThemeAsset;
import esq.phonemod.PhoneMod;


public class AssetRegistryManager {
        private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
        private final PhoneMod plugin;

        public AssetRegistryManager(PhoneMod plugin) {
                this.plugin = plugin;
        }
        public void register() {
                LOGGER.atInfo().log("Registering Asset Registry");

                AssetRegistry.register(
                                HytaleAssetStore.builder(DeviceAsset.class,
                                                new DefaultAssetMap<String, DeviceAsset>())
                                                .setPath("Phonemod/Devices")
                                                .setCodec(DeviceAsset.CODEC)
                                                .setKeyFunction(DeviceAsset::getId)
                                                .build());

                AssetRegistry.register(
                                HytaleAssetStore.builder(DeviceThemeAsset.class,
                                                new DefaultAssetMap<String, DeviceThemeAsset>())
                                                .setPath("Phonemod/Themes")
                                                .setCodec(DeviceThemeAsset.CODEC)
                                                .setKeyFunction(DeviceThemeAsset::getId)
                                                .build());
        }
}
