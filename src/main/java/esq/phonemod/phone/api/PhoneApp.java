package esq.phonemod.phone.api;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

/**
 * Generic phone application contract.
 *
 * @param <S> application state enum type
 */
public interface PhoneApp<S extends Enum<S>> {

    String getId();

    String getDisplayName();

    /**
     * Path to the .ui file that renders this app's button in the AppMenu grid.
     *
     * <p>
     * The file must define a root element named {@code #APPBUTTONENTRY} containing
     * a {@code TextButton #APPBUTTON} (with the icon hardcoded in its Default
     * style)
     * and a {@code Label #APPNAME}. AppMenu sets the label text dynamically after
     * appending the file, so {@code Text} in the file is just a placeholder.
     *
     * <p>
     * Example path: {@code "Pages/Phone/Components/WhatgramButton.ui"}.
     * Third-party plugins should use their own namespace, e.g.
     * {@code "Pages/Playground/AppIcon.ui"}.
     */
    String getAppButtonUI();

    String getUIPath();

    default int getSortOrder() {
        return 0;
    }

    default void onOpen(PhoneAppContext ctx,
            UICommandBuilder cmd,
            UIEventBuilder evb) {
    }

    void build(PhoneAppContext ctx,
            UICommandBuilder cmd,
            UIEventBuilder evb);

    boolean handleEvent(PhoneAppContext ctx,
            PhoneEvent event,
            UICommandBuilder cmd,
            UIEventBuilder evb);

    default void onClose(PhoneAppContext ctx) {
    }

    default void onIncomingMessage(PhoneAppContext ctx,
            String fromNumber) {
    }

    default boolean onIncomingMessage(PhoneAppContext ctx,
            String fromNumber,
            UICommandBuilder cmd,
            UIEventBuilder evb) {
        onIncomingMessage(ctx, fromNumber);
        return false;
    }

    default void onIncomingCall(PhoneAppContext ctx,
            String callerNumber,
            String callerName) {
    }
}
