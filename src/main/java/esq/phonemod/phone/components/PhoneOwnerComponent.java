package esq.phonemod.phone.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Persistent component attached to any entity that owns a phone.
 *
 * <p>Phone numbers are stored on the phone item's metadata (BsonDocument key
 * {@code "PhoneNumber"}) so each physical item has its own unique number.
 * This component holds only the contacts list.
 * Contacts are stored as a map of {@code phoneNumber -> displayName}.
 */
public class PhoneOwnerComponent implements Component<EntityStore> {

    // ── Fields ───────────────────────────────────────────────────────────────

    /**
     * Contact list: own phone number → contact phone number → display name.
     * e.g. {@code "555-0001" → { "555-1234" → "Big Tony" }}
     */
    private Map<String, Map<String, String>> contacts;

    // ── Codec ───────────────────────────────────────────────────────────────

    public static final BuilderCodec<PhoneOwnerComponent> CODEC =
            BuilderCodec.builder(PhoneOwnerComponent.class, PhoneOwnerComponent::new)
                    .append(
                            new KeyedCodec<>("Contacts",
                                    new MapCodec<>(
                                            new MapCodec<>(Codec.STRING, HashMap::new, false),
                                            HashMap::new, false)),
                            (data, value) -> data.contacts = value,
                            data -> data.contacts)
                    .add()
                    .build();

    // ── Constructors ───────────────────────────────────────────────────────────

    /** Default constructor. */
    public PhoneOwnerComponent() {
        this.contacts = new HashMap<>();
    }

    /** Copy constructor used by {@link #clone()}. */
    public PhoneOwnerComponent(@Nonnull PhoneOwnerComponent source) {
        this.contacts = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> outer : source.contacts.entrySet()) {
            this.contacts.put(outer.getKey(), new HashMap<>(outer.getValue()));
        }
    }

    // ── Component impl ─────────────────────────────────────────────────────────

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new PhoneOwnerComponent(this);
    }

    // ── ComponentType ─────────────────────────────────────────────────────────

    private static ComponentType<EntityStore, PhoneOwnerComponent> COMPONENT_TYPE;

    public static ComponentType<EntityStore, PhoneOwnerComponent> getComponentType() {
        return COMPONENT_TYPE;
    }

    public static void setComponentType(ComponentType<EntityStore, PhoneOwnerComponent> type) {
        COMPONENT_TYPE = type;
    }

    // ── Getters / Setters ──────────────────────────────────────────────────

    /**
     * Returns the contacts map for the given own phone number (contactNumber → displayName).
     * Returns an empty (mutable) map if none exist yet — caller must persist the component after mutating.
     */
    @Nonnull
    public Map<String, String> getContacts(@Nonnull String ownNumber) {
        return contacts.computeIfAbsent(ownNumber, k -> new HashMap<>());
    }

    public void addContact(@Nonnull String ownNumber, @Nonnull String contactNumber, @Nonnull String displayName) {
        contacts.computeIfAbsent(ownNumber, k -> new HashMap<>()).put(contactNumber, displayName);
    }

    public void removeContact(@Nonnull String ownNumber, @Nonnull String contactNumber) {
        Map<String, String> inner = contacts.get(ownNumber);
        if (inner != null) inner.remove(contactNumber);
    }

}

