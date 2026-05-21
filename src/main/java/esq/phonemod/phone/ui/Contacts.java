package esq.phonemod.phone.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.phone.components.PhoneOwnerComponent;

import javax.annotation.Nonnull;
import java.util.Map;

/** Static helpers that build the Contacts app UI state. */
public final class Contacts {

    static final String CONTACTS_UI       = "Pages/Phone/Contacts.ui";
    static final String CONTACTS_ENTRY_UI = "Pages/Phone/Components/ContactsEntry.ui";
    static final String CONTACTS_ADD_UI   = "Pages/Phone/Components/ContactsAdd.ui";

    private static final String CONTENT = "#AppContent";

    private Contacts() {}

    /**
     * Clears {@code #AppContent} and renders the contacts list for the given phone number.
     * Each row has a Chat button ({@code open_chat}) and a Remove button ({@code remove_contact}).
     * An Add Contact button at the top fires {@code open_add_contact}.
     */
    public static void loadContactsState(@Nonnull String phoneNumber,
                                          @Nonnull Store<EntityStore> store,
                                          @Nonnull Ref<EntityStore> ref,
                                          @Nonnull UICommandBuilder cmd,
                                          @Nonnull UIEventBuilder evb) {
        cmd.clear(CONTENT);
        cmd.append(CONTENT, CONTACTS_UI);

        evb.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#AddContactButton",
                EventData.of("Action", "open_add_contact"),
                false);

        PhoneOwnerComponent owner =
                store.ensureAndGetComponent(ref, PhoneOwnerComponent.getComponentType());
        Map<String, String> contacts = owner.getContacts(phoneNumber);
        int i = 0;
        for (Map.Entry<String, String> entry : contacts.entrySet()) {
            String number      = entry.getKey();
            String displayName = entry.getValue();
            cmd.append("#ContactsList", CONTACTS_ENTRY_UI);
            cmd.set("#ContactsList[" + i + "] #ContactName.Text", displayName + " (" + number + ")");
            evb.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ContactsList[" + i + "] #ChatButton",
                    EventData.of("Action", "open_chat").append("Contact", number),
                    false);
            evb.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ContactsList[" + i + "] #RemoveButton",
                    EventData.of("Action", "remove_contact").append("Contact", number),
                    false);
            evb.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ContactsList[" + i + "] #CallButton",
                    EventData.of("Action", "start_call").append("Contact", number),
                    false);
            i++;
        }
    }

    /**
     * Clears {@code #AppContent} and renders the add-contact inline form.
     * Save fires {@code save_contact} with form values; Cancel fires {@code contacts}.
     */
    public static void loadAddContactState(@Nonnull UICommandBuilder cmd,
                                            @Nonnull UIEventBuilder evb) {
        cmd.clear(CONTENT);
        cmd.append(CONTENT, CONTACTS_ADD_UI);

        evb.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SaveButton",
                EventData.of("Action", "save_contact")
                        .append("@ContactFormNumber", "#NumberInput.Value")
                        .append("@ContactFormName", "#NameInput.Value"),
                false);
        evb.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CancelButton",
                EventData.of("Action", "contacts"),
                false);
    }
}
