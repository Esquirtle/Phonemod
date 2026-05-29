package esq.phonemod.phone.apps;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.phone.api.PhoneAppContext;
import esq.phonemod.phone.api.PhoneEvent;
import esq.phonemod.phone.api.StatefulPhoneApp;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import esq.phonemod.phone.components.PhoneOwnerComponent;
import esq.phonemod.phone.messaging.CallRegistry;

import java.util.Map;


public final class ContactsApp extends StatefulPhoneApp<ContactsApp.State> {

    public enum State {
        LIST,
        ADD_CONTACT
    }

    public ContactsApp() {
        super(State.LIST);
    }

    @Override
    public String getId() {
        return "contacts";
    }

    @Override
    public String getDisplayName() {
        return "Contacts";
    }

    @Override
    public String getAppButtonUI() {
        return "Pages/Phone/Components/ContactsButton.ui";
    }

    @Override
    public String getUIPath() {
        return "Pages/Phone/Contacts.ui";
    }

    @Override
    public void build(PhoneAppContext ctx, UICommandBuilder cmd, UIEventBuilder evb) {
        appendMainUI(cmd);
        if (getState(ctx) == State.ADD_CONTACT) {
            renderAddContactState(cmd, evb);
        } else {
            setState(ctx, State.LIST);
            renderContactsState(ctx, cmd, evb);
        }
    }

    private void renderContactsState(PhoneAppContext ctx, UICommandBuilder cmd, UIEventBuilder evb) {
        cmd.clear("#ContactsList");
        cmd.set("#AddContactButton.Text", "Add");
        evb.addEventBinding(CustomUIEventBindingType.Activating,
                "#AddContactButton",
                EventData.of("Action", "open_add_contact"),
                false);

        PhoneOwnerComponent owner = ctx.getStore().ensureAndGetComponent(ctx.getRef(), PhoneOwnerComponent.getComponentType());
        Map<String, String> contacts = owner.getContacts(ctx.getPhoneNumber());
        int i = 0;
        for (Map.Entry<String, String> entry : contacts.entrySet()) {
            String number = entry.getKey();
            String displayName = entry.getValue();
            cmd.append("#ContactsList", "Pages/Phone/Components/ContactsEntry.ui");
            cmd.set("#ContactsList[" + i + "] #ContactName.Text", displayName + " (" + number + ")");
            evb.addEventBinding(CustomUIEventBindingType.Activating,
                    "#ContactsList[" + i + "] #ChatButton",
                    EventData.of("Action", "open_chat").append("Contact", number),
                    false);
            evb.addEventBinding(CustomUIEventBindingType.Activating,
                    "#ContactsList[" + i + "] #RemoveButton",
                    EventData.of("Action", "remove_contact").append("Contact", number),
                    false);
            evb.addEventBinding(CustomUIEventBindingType.Activating,
                    "#ContactsList[" + i + "] #CallButton",
                    EventData.of("Action", "start_call").append("Contact", number),
                    false);
            i++;
        }
    }

    private void renderAddContactState(UICommandBuilder cmd, UIEventBuilder evb) {
        cmd.clear("#AppContent");
        cmd.append("#AppContent", "Pages/Phone/Components/ContactsAdd.ui");
        evb.addEventBinding(CustomUIEventBindingType.Activating,
                "#SaveButton",
                EventData.of("Action", "save_contact")
                        .append("@ContactFormNumber", "#NumberInput.Value")
                        .append("@ContactFormName", "#NameInput.Value"),
                false);
        evb.addEventBinding(CustomUIEventBindingType.Activating,
                "#CancelButton",
                EventData.of("Action", "contacts"),
                false);
    }

    @Override
    public boolean handleEvent(PhoneAppContext ctx, PhoneEvent event, UICommandBuilder cmd, UIEventBuilder evb) {
        String action = event.getAction();
        switch (action) {
            case "open_add_contact" -> {
                setState(ctx, State.ADD_CONTACT);
                build(ctx, cmd, evb);
                return true;
            }
            case "contacts" -> {
                setState(ctx, State.LIST);
                build(ctx, cmd, evb);
                return true;
            }
            case "save_contact" -> {
                String number = event.getParams().get("ContactFormNumber");
                String name = event.getParams().get("ContactFormName");
                if (number != null && !number.isBlank() && name != null && !name.isBlank()) {
                    Ref<EntityStore> ref = ctx.getRef();
                    Store<EntityStore> store = ctx.getStore();
                    PhoneOwnerComponent owner = store.ensureAndGetComponent(ref, PhoneOwnerComponent.getComponentType());
                    owner.addContact(ctx.getPhoneNumber(), number.trim(), name.trim());
                    store.putComponent(ref, PhoneOwnerComponent.getComponentType(), owner);
                }
                setState(ctx, State.LIST);
                build(ctx, cmd, evb);
                return true;
            }
            case "remove_contact" -> {
                String contact = event.getParams().get("Contact");
                if (contact != null) {
                    Ref<EntityStore> ref = ctx.getRef();
                    Store<EntityStore> store = ctx.getStore();
                    PhoneOwnerComponent owner = store.ensureAndGetComponent(ref, PhoneOwnerComponent.getComponentType());
                    owner.removeContact(ctx.getPhoneNumber(), contact);
                    store.putComponent(ref, PhoneOwnerComponent.getComponentType(), owner);
                }
                setState(ctx, State.LIST);
                build(ctx, cmd, evb);
                return true;
            }
            case "start_call" -> {
                String contact = event.getParams().get("Contact");
                if (contact != null && !contact.isBlank()) {
                    CallRegistry.initiateCall(ctx.getPhoneNumber(), contact.trim(), contact.trim());
                }
                build(ctx, cmd, evb);
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
