package esq.phonemod.setup;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;

import esq.phonemod.device.assets.DeviceAsset;
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

                //AssetRegistry.register(
                //                HytaleAssetStore.builder(DialogAsset.class, new DefaultAssetMap<String, DialogAsset>())
                //                                .setPath("Dialog/Dialogs")
                //                                .setCodec(DialogAsset.CODEC)
                //                                .setKeyFunction(DialogAsset::getId)
                //                                .build());
                //
                //AssetRegistry.register(
                //                HytaleAssetStore.builder(DialogChainAsset.class,
                //                                new DefaultAssetMap<String, DialogChainAsset>())
                //                                .setPath("Dialog/DialogChain")
                //                                .setCodec(DialogChainAsset.CODEC)
                //                                .setKeyFunction(DialogChainAsset::getId)
                //                                .build());
                //
                //AssetRegistry.register(
                //                HytaleAssetStore.builder(Skill.class, new DefaultAssetMap<String, Skill>())
                //                                .setPath("Hero/Skills")
                //                                .setCodec(Skill.CODEC)
                //                                .setKeyFunction(Skill::getId)
                //                                .build());
                //
                //AssetRegistry.register(
                //                HytaleAssetStore.builder(HeroClass.class, new DefaultAssetMap<String, HeroClass>())
                //                                .setPath("Hero/Class")
                //                                .setCodec(HeroClass.CODEC)
                //                                .setKeyFunction(HeroClass::getId)
                //                                .build());
                //
                //AssetRegistry.register(
                //                HytaleAssetStore.builder(TalentPage.class, new DefaultAssetMap<String, TalentPage>())
                //                                .setPath("Talents/TalentPages")
                //                                .setCodec(TalentPage.CODEC)
                //                                .setKeyFunction(TalentPage::getId)
                //                                .build());
                //
                //AssetRegistry.register(
                //                HytaleAssetStore.builder(Talent.class, new DefaultAssetMap<String, Talent>())
                //                                .setPath("Talents/Talent")
                //                                .setCodec(Talent.CODEC)
                //                                .setKeyFunction(Talent::getId)
                //                                .build());
                //
                //AssetRegistry.register(
                //                HytaleAssetStore.builder(HeroRace.class, new DefaultAssetMap<String, HeroRace>())
                //                                .setPath("Hero/Race")
                //                                .setCodec(HeroRace.CODEC)
                //                                .setKeyFunction(HeroRace::getId)
                //                                .build());
                //
                //AssetRegistry.register(
                //                HytaleAssetStore.builder(HeroRaceVariant.class,
                //                                new DefaultAssetMap<String, HeroRaceVariant>())
                //                                .setPath("Hero/RaceVariants")
                //                                .setCodec(HeroRaceVariant.CODEC)
                //                                .setKeyFunction(HeroRaceVariant::getId)
                //                                .build());
                //
                //AssetRegistry.register(
                //                HytaleAssetStore.builder(ItemModifierAsset.class,
                //                                new DefaultAssetMap<String, ItemModifierAsset>())
                //                                .setPath("RpgItems/Modifiers")
                //                                .setCodec(ItemModifierAsset.CODEC)
                //                                .setKeyFunction(ItemModifierAsset::getId)
                //                                .build());
                //AssetRegistry.register(
                //                HytaleAssetStore.builder(ProfessionAsset.class,
                //                                new DefaultAssetMap<String, ProfessionAsset>())
                //                                .setPath("Professions/Profession")
                //                                .setCodec(ProfessionAsset.CODEC)
                //                                .setKeyFunction(ProfessionAsset::getId)
                //                                .build());
                //AssetRegistry.register(
                //                HytaleAssetStore.builder(ProfessionRecipeAsset.class,
                //                                new DefaultAssetMap<String, ProfessionRecipeAsset>())
                //                                .setPath("Professions/Recipes")
                //                                .setCodec(ProfessionRecipeAsset.CODEC)
                //                                .setKeyFunction(ProfessionRecipeAsset::getId)
                //                                .build());
                //AssetRegistry.register(
                //                HytaleAssetStore.builder(EnemySpawnerAsset.class,
                //                                new DefaultAssetMap<String, EnemySpawnerAsset>())
                //                                .setPath("PinaSpawners/Spawner")
                //                                .setCodec(EnemySpawnerAsset.CODEC)
                //                                .setKeyFunction(EnemySpawnerAsset::getId)
                //                                .build());
        }
}
