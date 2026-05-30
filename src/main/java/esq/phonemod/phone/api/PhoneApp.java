package esq.phonemod.phone.api;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

import java.util.Collections;
import java.util.Map;

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

    /**
     * Optional icon texture path for list/store rendering (e.g. the App Store),
     * as an absolute UI path from {@code Common/UI/Custom/} — e.g.
     * {@code "Pages/Phone/Whatgram.png"} (the {@code @2x} suffix is added by the
     * engine; reference without it). Returns {@code null} when the app has no
     * dedicated icon, in which case consumers fall back to a placeholder tile.
     *
     * <p>Distinct from {@link #getAppButtonUI()} (the full home-grid button
     * document); this is just the image, for compact rows.
     */
    default String getIconPath() {
        return null;
    }

    /**
     * Themeable selectors this app exposes, as a map of <em>role &rarr; selector</em>.
     * Roles correspond to keys in a theme palette's {@code Colors} map (see
     * {@code DeviceThemeAsset}); the framework sets the {@code Background} of each
     * selector to the palette color when a theme is applied.
     *
     * <p>Default is empty — apps opt in by overriding. Selectors should match
     * named elements in the app's own {@link #getUIPath()} document.
     */
    default Map<String, String> getThemeableSelectors() {
        return Collections.emptyMap();
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
