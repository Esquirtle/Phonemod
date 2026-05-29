package esq.phonemod.phone.interactions;

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
import esq.phonemod.phone.components.PhoneOwnerComponent;
import esq.phonemod.phone.core.PhoneService;
import esq.phonemod.phone.messaging.PhoneRegistry;
import esq.phonemod.phone.ui.PhonePage;
import org.bson.BsonDocument;
import org.bson.BsonString;

import javax.annotation.Nonnull;
import java.util.Random;

/**
 * Interaction that opens the {@link PhonePage} for the activating player.
 * Register in setup with key {@code "open_phone"}.
 *
 * <p>Phone numbers are stored in the item's metadata under the key
 * {@code "PhoneNumber"} so each physical phone item has its own unique number.
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
    private static final String PHONE_NUMBER_KEY = "PhoneNumber";
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
            LOGGER.atWarning().log("[OpenPhone] PlayerRef is null — cannot open phone page");
            return;
        }

        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
            LOGGER.atWarning().log("[OpenPhone] Player component is null — cannot open phone page");
            return;
        }

        // Read the phone number from item metadata during tick (read-safe)
        ItemStack heldItem = context.getHeldItem();
        if (heldItem == null) {
            LOGGER.atWarning().log("[OpenPhone] No held item — cannot open phone page");
            return;
        }

        BsonDocument metadata = heldItem.getMetadata();
        final String phoneNumber;

        if (metadata != null && metadata.containsKey(PHONE_NUMBER_KEY)) {
            phoneNumber = metadata.getString(PHONE_NUMBER_KEY).getValue();
        } else {
            // Generate a new number and write it into the inventory slot
            phoneNumber = generatePhoneNumber();
            BsonDocument newMeta = metadata != null ? metadata.clone() : new BsonDocument();
            newMeta.put(PHONE_NUMBER_KEY, new BsonString(phoneNumber));
            ItemStack stamped = new ItemStack(heldItem.getItemId(), heldItem.getQuantity(), newMeta);
            context.getHeldItemContainer().setItemStackForSlot(context.getHeldItemSlot(), stamped);
            context.setHeldItem(stamped);
            LOGGER.atInfo().log("[OpenPhone] Assigned new phone number %s to item", phoneNumber);
        }

        // Register the phone number in the registry so messages can be delivered
        // and the disconnect listener can clean it up. Safe on the tick thread (ConcurrentHashMap).
        // The PhonePage is created here so its reference can be stored in the registry
        // for live push updates before the page is opened on the world thread.
        var store = commandBuffer.getStore();
        PhonePage phonePage = PhoneService.get().createPhonePage(playerRef, phoneNumber);
        World world = player.getWorld();
        PhoneRegistry.register(phoneNumber, ref, playerRef, store, world, phonePage);

        final boolean needsComponent = store.getComponent(ref, PhoneOwnerComponent.getComponentType()) == null;

        world.execute(() -> {
            if (needsComponent) {
                store.putComponent(ref, PhoneOwnerComponent.getComponentType(), new PhoneOwnerComponent());
            }
            player.getPageManager().openCustomPage(
                    playerRef.getReference(),
                    store,
                    phonePage);
        });
    }

    /** Generates a number in the format {@code XXX-XXXX}. */
    private static String generatePhoneNumber() {
        String phoneNumber;
        do {
            int prefix = 100 + RANDOM.nextInt(900);
            int suffix = RANDOM.nextInt(10_000);
            phoneNumber = String.format("%03d-%04d", prefix, suffix);
        } while (PhoneRegistry.getOnlineEntry(phoneNumber) != null);
        return phoneNumber;
    }
}
