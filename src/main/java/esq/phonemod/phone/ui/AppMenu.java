package esq.phonemod.phone.ui;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

import javax.annotation.Nonnull;

public class AppMenu {

    // Selectors — target the TextButton inside each @AppButton instance
    private static final String BTN_WHATGRAM  = "#WHATGRAM #APPBUTTON";
    private static final String BTN_CONTACTS  = "#CONTACTS #APPBUTTON";
    private static final String BTN_GANG      = "#GANG #APPBUTTON";
    private static final String BTN_SETTINGS  = "#SETTINGS #APPBUTTON";
    private static final String BTN_AMASON    = "#AMASON #APPBUTTON";
    private static final String BTN_CALLS     = "#CALLS #APPBUTTON";

    public static void buildEventBindings(@Nonnull UIEventBuilder evb) {
        evb.addEventBinding(CustomUIEventBindingType.Activating, BTN_WHATGRAM,
                EventData.of("Action", "open_app").append("App", "whatgram"), false);
        evb.addEventBinding(CustomUIEventBindingType.Activating, BTN_CONTACTS,
                EventData.of("Action", "open_app").append("App", "contacts"), false);
        evb.addEventBinding(CustomUIEventBindingType.Activating, BTN_GANG,
                EventData.of("Action", "open_app").append("App", "gang"), false);
        evb.addEventBinding(CustomUIEventBindingType.Activating, BTN_SETTINGS,
                EventData.of("Action", "open_app").append("App", "settings"), false);
        evb.addEventBinding(CustomUIEventBindingType.Activating, BTN_AMASON,
                EventData.of("Action", "open_app").append("App", "amason"), false);
        evb.addEventBinding(CustomUIEventBindingType.Activating, BTN_CALLS,
                EventData.of("Action", "open_app").append("App", "calls"), false);
    }
}
