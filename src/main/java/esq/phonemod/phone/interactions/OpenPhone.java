package esq.phonemod.phone.interactions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.device.core.DeviceService;
import esq.phonemod.device.core.DeviceSession;
import esq.phonemod.device.core.DeviceShell;
import esq.phonemod.device.ui.DevicePage;
import esq.phonemod.phone.components.PhoneOwnerComponent;
import esq.phonemod.phone.messaging.PhoneRegistry;
import org.bson.BsonString;

import javax.annotation.Nonnull;
import java.util.Random;

/**
 * Interaction that opens the phone device page for the activating player.
 * Register in setup with key {@code "open_phone"}.
 *
 * <p>The device shell and metadata key are read from the {@code Phone} device
 * asset so no constants are hard-coded here. Physical phone item metadata uses
 * the key specified by {@link DeviceShell#getMetadataKey()} (default
 * {@code "PhoneNumber"}).
 *
 * <p>Link to an item's {@code Interactions} block in its JSON definition:
 * <pre>{@code
 * "Secondary": {
 *   "Interactions": [{ "Type": "open_phone" }]
 * }
 * }</pre>
 */
public final class OpenPhone extends SimpleInstantInteraction {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Random RANDOM = new Random();

    public static final BuilderCodec<OpenPhone> CODEC = BuilderCodec
            .builder(OpenPhone.class, OpenPhone::new, SimpleInstantInteraction.CODEC)
            .build();

    @Override
    protected void firstRun(@Nonnull InteractionType type,
                            @Nonnull InteractionContext context,
                            @Nonnull CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        var commandBuffer = context.getCommandBuffer();

        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            LOGGER.atWarning().log("[OpenPhone] PlayerRef is null — cannot open device page");
            return;
        }

        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
            LOGGER.atWarning().log("[OpenPhone] Player component is null — cannot open device page");
            return;
        }

        ItemStack heldItem = context.getHeldItem();
        if (heldItem == null) {
            LOGGER.atWarning().log("[OpenPhone] No held item — cannot open device page");
            return;
        }

        // Resolve the device shell for the default phone asset. This gives us the
        // metadata key without hard-coding "PhoneNumber" in this class.
        DeviceShell shell;
        try {
            shell = DeviceService.get().createShell(DeviceService.DEFAULT_PHONE_ASSET_ID);
        } catch (IllegalArgumentException e) {
            LOGGER.atWarning().withCause(e).log("[OpenPhone] Default phone device asset not available; falling back");
            return;
        }

        String metadataKey = shell.getMetadataKey();
        String existingId = heldItem.getFromMetadataOrNull(metadataKey, Codec.STRING);
        final String deviceId;

        if (existingId != null) {
            deviceId = existingId;
        } else {
            // Generate a new device ID and stamp it into the item metadata.
            deviceId = generatePhoneNumber();
            ItemStack stamped = heldItem.withMetadata(metadataKey, new BsonString(deviceId));
            context.getHeldItemContainer().setItemStackForSlot(context.getHeldItemSlot(), stamped);
            context.setHeldItem(stamped);
            LOGGER.atInfo().log("[OpenPhone] Assigned new device ID %s to item (key=%s)", deviceId, metadataKey);
        }

        // Build the session and page on the tick thread so the page reference is
        // ready before PhoneRegistry.register stores it for live push callbacks.
        var store = commandBuffer.getStore();
        DeviceSession session = DeviceService.get().createSession(
                playerRef, ref, store, DeviceService.DEFAULT_PHONE_ASSET_ID, deviceId);
        DevicePage devicePage = DeviceService.get().createDevicePage(session);
        World world = player.getWorld();
        PhoneRegistry.register(deviceId, ref, playerRef, store, world, devicePage);

        final boolean needsComponent = store.getComponent(ref, PhoneOwnerComponent.getComponentType()) == null;

        world.execute(() -> {
            if (needsComponent) {
                store.putComponent(ref, PhoneOwnerComponent.getComponentType(), new PhoneOwnerComponent());
            }
            player.getPageManager().openCustomPage(
                    playerRef.getReference(),
                    store,
                    devicePage);
        });
    }

    /** Generates a device ID in the format {@code XXX-XXXX}. */
    private static String generatePhoneNumber() {
        String id;
        do {
            int prefix = 100 + RANDOM.nextInt(900);
            int suffix = RANDOM.nextInt(10_000);
            id = String.format("%03d-%04d", prefix, suffix);
        } while (PhoneRegistry.getOnlineEntry(id) != null);
        return id;
    }
}
