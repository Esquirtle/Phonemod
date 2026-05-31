package esq.phonemod.phone.apps;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import esq.phonemod.device.core.DeviceService;
import esq.phonemod.device.services.DeviceCallService;
import esq.phonemod.device.services.DeviceContactService;
import esq.phonemod.phone.api.PhoneAppContext;
import esq.phonemod.phone.api.PhoneAssetPaths;
import esq.phonemod.phone.api.PhoneEvent;
import esq.phonemod.phone.api.PhoneEventActions;
import esq.phonemod.phone.api.PhoneUi;
import esq.phonemod.phone.api.StatefulPhoneApp;
import esq.phonemod.phone.apps.helpers.Contacts;

import java.util.Map;

/**
 * Contacts — phone book app.
 *
 * <p>Demonstrates the preferred A4 pattern:
 * <ul>
 *   <li>All contact mutations go through {@link DeviceContactService} via
 *       {@link DeviceService#get()} — no direct {@code PhoneOwnerComponent}
 *       access.</li>
 *   <li>Call initiation goes through {@link DeviceCallService} — no direct
 *       {@code CallRegistry} calls.</li>
 *   <li>All UI bindings use {@link PhoneUi} helpers with
 *       {@link PhoneEventActions} constants.</li>
 * </ul>
 */
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
        return PhoneAssetPaths.CONTACTS_BUTTON_UI;
    }

    @Override
    public String getUIPath() {
        return PhoneAssetPaths.DUST_CONTACTS_UI;
    }

    @Override
    public String getIconPath() {
        return "Pages/Phone/Contacts.png";
    }

    @Override
    public Map<String, String> getThemeableSelectors() {
        return Map.of(
                "appHeader", Contacts.SEL_HEADER,
                "appPanel", Contacts.SEL_PANEL,
                "appContent", Contacts.SEL_CONTACTS_LIST);
    }

    @Override
    public void build(PhoneAppContext ctx, UICommandBuilder cmd, UIEventBuilder evb) {
        appendMainUI(ctx, cmd);
        if (getState(ctx) == State.ADD_CONTACT) {
            renderAddContactState(ctx, cmd, evb);
        } else {
            setState(ctx, State.LIST);
            renderContactsState(ctx, cmd, evb);
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private void renderContactsState(PhoneAppContext ctx, UICommandBuilder cmd, UIEventBuilder evb) {
        // Show the list view, hide the add form — both live in the single root, so
        // the themed selectors (#ContactsHeader/#ContactsPanel) stay alive and we
        // never replace the shell content area.
        PhoneUi.setVisible(cmd, Contacts.SEL_LIST_VIEW, true);
        PhoneUi.setVisible(cmd, Contacts.SEL_ADD_VIEW, false);
        PhoneUi.setVisible(cmd, Contacts.SEL_ADD_CONTACT_BUTTON, true);

        PhoneUi.safeClear(cmd, Contacts.SEL_CONTACTS_LIST);

        // The add-contact button is defined in the DustContacts root; bind it.
        PhoneUi.bindAction(evb, Contacts.SEL_ADD_CONTACT_BUTTON, PhoneEventActions.OPEN_ADD_CONTACT);

        DeviceContactService contactService = DeviceService.get().getContactService();
        Map<String, String> contacts = contactService.getContacts(
                ctx.getStore(), ctx.getRef(), ctx.getPhoneNumber());

        int i = 0;
        for (Map.Entry<String, String> entry : contacts.entrySet()) {
            String number      = entry.getKey();
            String displayName = entry.getValue();
            String row = PhoneUi.appendListItem(
                    cmd, Contacts.SEL_CONTACTS_LIST, Contacts.CONTACTS_ENTRY_UI, i);
            PhoneUi.setText(cmd, PhoneUi.child(row, Contacts.SEL_ENTRY_CONTACT_NAME_TEXT),
                    displayName + " (" + number + ")");
            PhoneUi.bindAction(evb, PhoneUi.child(row, Contacts.SEL_ENTRY_CHAT_BUTTON),
                    PhoneEventActions.OPEN_CHAT, PhoneUi.params("Contact", number));
            PhoneUi.bindAction(evb, PhoneUi.child(row, Contacts.SEL_ENTRY_REMOVE_BUTTON),
                    PhoneEventActions.REMOVE_CONTACT, PhoneUi.params("Contact", number));
            PhoneUi.bindAction(evb, PhoneUi.child(row, Contacts.SEL_ENTRY_CALL_BUTTON),
                    PhoneEventActions.START_CALL, PhoneUi.params("Contact", number));
            i++;
        }
    }

    private void renderAddContactState(PhoneAppContext ctx, UICommandBuilder cmd, UIEventBuilder evb) {
        // Toggle to the add form (a hidden sibling in the same root). The shell
        // content area is never replaced, so #ContactsHeader/#ContactsPanel stay
        // alive for theme re-application.
        PhoneUi.setVisible(cmd, Contacts.SEL_LIST_VIEW, false);
        PhoneUi.setVisible(cmd, Contacts.SEL_ADD_VIEW, true);
        PhoneUi.setVisible(cmd, Contacts.SEL_ADD_CONTACT_BUTTON, false);

        // Capture form values from both input fields in a single binding each.
        PhoneUi.bindAction(evb, Contacts.SEL_SAVE_BUTTON,
                PhoneEventActions.SAVE_CONTACT,
                PhoneUi.params("@ContactFormNumber", Contacts.SEL_NUMBER_INPUT_VALUE)
                        .append("@ContactFormName", Contacts.SEL_NAME_INPUT_VALUE));
        PhoneUi.bindAction(evb, Contacts.SEL_CANCEL_BUTTON, PhoneEventActions.CONTACTS);
    }

    // ── Event handling ─────────────────────────────────────────────────────────

    @Override
    public boolean handleEvent(PhoneAppContext ctx, PhoneEvent event, UICommandBuilder cmd, UIEventBuilder evb) {
        String action = event.getAction();

        switch (action) {
            case PhoneEventActions.OPEN_ADD_CONTACT -> {
                setState(ctx, State.ADD_CONTACT);
                build(ctx, cmd, evb);
                return true;
            }
            case PhoneEventActions.CONTACTS -> {
                setState(ctx, State.LIST);
                build(ctx, cmd, evb);
                return true;
            }
            case PhoneEventActions.SAVE_CONTACT -> {
                String number = event.getParam("ContactFormNumber");
                String name   = event.getParam("ContactFormName");
                if (number != null && !number.isBlank() && name != null && !name.isBlank()) {
                    DeviceService.get().getContactService().addContact(
                            ctx.getStore(), ctx.getRef(),
                            ctx.getPhoneNumber(), number.trim(), name.trim());
                }
                setState(ctx, State.LIST);
                build(ctx, cmd, evb);
                return true;
            }
            case PhoneEventActions.REMOVE_CONTACT -> {
                String contact = event.getParam("Contact");
                if (contact != null && !contact.isBlank()) {
                    DeviceService.get().getContactService().removeContact(
                            ctx.getStore(), ctx.getRef(),
                            ctx.getPhoneNumber(), contact.trim());
                }
                setState(ctx, State.LIST);
                build(ctx, cmd, evb);
                return true;
            }
            case PhoneEventActions.START_CALL -> {
                String contact = event.getParam("Contact");
                if (contact != null && !contact.isBlank()) {
                    DeviceService.get().getCallService()
                            .initiateCall(ctx.getPhoneNumber(), contact.trim(), contact.trim());
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
