package esq.phonemod.phone.api;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Thin helper utilities wrapping common Hytale UI builder patterns used in
 * phone/device apps.
 *
 * <p>These helpers reduce repeated selector-string and {@link EventData}
 * boilerplate. They do not hide the underlying builders — callers can mix
 * direct calls with helper calls freely.
 *
 * <p>All helpers are pure static; there is no instance state.
 *
 * <h2>set / append / clear ownership contract</h2>
 * Every render must respect a strict ownership boundary so theme re-application
 * and partial updates never target a selector that has been torn down:
 * <ol>
 *   <li><b>The shell content area ({@code ctx.getContentSelector()}, e.g.
 *       {@code #AppContent}) is owned by the framework.</b> An app clears and
 *       re-appends it in exactly one place — {@code build()} via
 *       {@link StatefulPhoneApp#appendMainUI} (or {@link #appendMain}). Render
 *       helpers must never clear or append the content area again.</li>
 *   <li><b>Sub-views are switched <i>inside</i> a single app root, not by
 *       swapping roots.</b> Prefer one root document that contains every view's
 *       elements; toggle between views with {@link #setVisible} and by refilling
 *       the app's own list container. This keeps every
 *       {@code getThemeableSelectors()} target alive in all views.</li>
 *   <li><b>An app only clears containers it owns</b> — its own list/group
 *       selectors (via {@link #safeClear}), never the shell content area
 *       mid-render. Appending rows: {@link #safeClear} the list, then
 *       {@link #appendListItem} in a loop.</li>
 *   <li><b>A full-screen takeover</b> (e.g. an incoming-call overlay driven by
 *       the device, not the app) is the only case that replaces the content
 *       area outright, via {@link #replace}. Apps themselves should avoid it.</li>
 * </ol>
 */
public final class PhoneUi {

    private PhoneUi() {
    }

    // ── App entry point ────────────────────────────────────────────────────────

    /**
     * Clears the device shell's content area (from {@link PhoneAppContext#getContentSelector()})
     * and appends the app's main UI file into it. Equivalent to the canonical first
     * two lines of every {@code build()} implementation.
     *
     * @param ctx the app context (provides the shell content selector)
     * @param cmd the command builder to write to
     * @param app the app whose {@link PhoneApp#getUIPath()} will be appended
     */
    public static void appendMain(@Nonnull PhoneAppContext ctx,
            @Nonnull UICommandBuilder cmd,
            @Nonnull PhoneApp<?> app) {
        String content = ctx.getContentSelector();
        cmd.clear(content);
        cmd.append(content, app.getUIPath());
    }

    // ── Action binding ─────────────────────────────────────────────────────────

    /**
     * Binds an {@code Activating} event to {@code selector} with a single
     * {@code Action} key.
     */
    public static void bindAction(@Nonnull UIEventBuilder evb,
            @Nonnull String selector,
            @Nonnull String action) {
        evb.addEventBinding(CustomUIEventBindingType.Activating,
                selector,
                EventData.of("Action", action),
                false);
    }

    /**
     * Binds an {@code Activating} event to {@code selector} with an {@link EventData}
     * payload. Use {@link #params} to build the payload.
     */
    public static void bindAction(@Nonnull UIEventBuilder evb,
            @Nonnull String selector,
            @Nonnull String action,
            @Nonnull EventData extraParams) {
        EventData data = EventData.of("Action", action);
        for (Map.Entry<String, String> entry : extraParams.events().entrySet()) {
            data.append(entry.getKey(), entry.getValue());
        }
        evb.addEventBinding(CustomUIEventBindingType.Activating, selector, data, false);
    }

    /**
     * Binds a button to open an app by ID.
     * Equivalent to {@code EventData.of("Action", "open_app").append("App", appId)}.
     */
    public static void bindOpenApp(@Nonnull UIEventBuilder evb,
            @Nonnull String selector,
            @Nonnull String appId) {
        evb.addEventBinding(CustomUIEventBindingType.Activating,
                selector,
                EventData.of("Action", PhoneEventActions.OPEN_APP).append("App", appId),
                false);
    }

    /**
     * Binds a button to navigate back (fires the {@code back} action).
     */
    public static void bindBack(@Nonnull UIEventBuilder evb, @Nonnull String selector) {
        bindAction(evb, selector, PhoneEventActions.BACK);
    }

    /**
     * Binds a button to navigate home (fires the {@code home} action).
     */
    public static void bindHome(@Nonnull UIEventBuilder evb, @Nonnull String selector) {
        bindAction(evb, selector, PhoneEventActions.HOME);
    }

    /**
     * Binds a button to fire {@code action} and capture the value of a client-side
     * text input field as a string parameter.
     *
     * <p>The input value is captured via the {@code @paramName} convention in
     * {@link EventData}. The UI element at {@code inputSelector} must expose a
     * {@code .Value} property.
     *
     * @param action        the action name to send
     * @param paramName     the param key under which the captured text is sent
     *                      (prefixed with {@code @} internally)
     * @param inputSelector selector of the input element, e.g. {@code "#MessageInput.Value"}
     */
    public static void bindCapturedText(@Nonnull UIEventBuilder evb,
            @Nonnull String selector,
            @Nonnull String action,
            @Nonnull String paramName,
            @Nonnull String inputSelector) {
        evb.addEventBinding(CustomUIEventBindingType.Activating,
                selector,
                EventData.of("Action", action).append("@" + paramName, inputSelector),
                false);
    }

    /**
     * Binds a button to fire {@code action} and capture a number input field value.
     * Functionally identical to {@link #bindCapturedText}; provided for semantic clarity.
     */
    public static void bindCapturedNumber(@Nonnull UIEventBuilder evb,
            @Nonnull String selector,
            @Nonnull String action,
            @Nonnull String paramName,
            @Nonnull String inputSelector) {
        bindCapturedText(evb, selector, action, paramName, inputSelector);
    }

    // ── List and selector composition ─────────────────────────────────────────

    /**
     * Appends a UI file into a list container and returns the index selector for
     * the appended entry.
     *
     * @param cmd          the command builder to write to
     * @param listSelector the parent list/group selector
     * @param uiPath       the UI file to append
     * @param index        0-based position to use when generating the row selector
     * @return the indexed row selector, e.g. {@code "#MyList[2]"}
     */
    @Nonnull
    public static String appendListItem(@Nonnull UICommandBuilder cmd,
            @Nonnull String listSelector,
            @Nonnull String uiPath,
            int index) {
        cmd.append(listSelector, uiPath);
        return rowSelector(listSelector, index);
    }

    /**
     * Returns the indexed child selector for a list row.
     * Example: {@code rowSelector("#ContactList", 3)} returns {@code "#ContactList[3]"}.
     */
    @Nonnull
    public static String rowSelector(@Nonnull String listSelector, int index) {
        return listSelector + "[" + index + "]";
    }

    /**
     * Returns a child selector composed from a parent and child selector.
     * Example: {@code child("#ContactList[0]", "#ContactName")} returns
     * {@code "#ContactList[0] #ContactName"}.
     */
    @Nonnull
    public static String child(@Nonnull String parentSelector, @Nonnull String childSelector) {
        return parentSelector + " " + childSelector;
    }

    /**
     * Returns an indexed child selector under a parent.
     * Example: {@code indexed("#APPHolder", 2)} returns {@code "#APPHolder[2]"}.
     */
    @Nonnull
    public static String indexed(@Nonnull String parentSelector, int index) {
        return parentSelector + "[" + index + "]";
    }

    // ── Text and visibility ────────────────────────────────────────────────────

    /**
     * Sets the text of {@code selector} to a localized {@link Message}.
     */
    public static void setText(@Nonnull UICommandBuilder cmd,
            @Nonnull String selector,
            @Nonnull Message message) {
        cmd.set(selector, message);
    }

    /**
     * Sets the text of {@code selector} to a raw (unlocalized) string.
     * Prefer {@link #setLocalizedText} for user-visible strings.
     */
    public static void setText(@Nonnull UICommandBuilder cmd,
            @Nonnull String selector,
            @Nonnull String rawText) {
        cmd.set(selector, rawText);
    }

    /**
     * Sets the text of {@code selector} to a localized translation key.
     */
    public static void setLocalizedText(@Nonnull UICommandBuilder cmd,
            @Nonnull String selector,
            @Nonnull String translationKey) {
        cmd.set(selector, Message.translation(translationKey));
    }

    /**
     * Sets the visibility of {@code selector}.
     * Maps to {@code selector + ".Visible"} and sends {@code true} or {@code false}.
     */
    public static void setVisible(@Nonnull UICommandBuilder cmd,
            @Nonnull String selector,
            boolean visible) {
        cmd.set(selector + ".Visible", visible);
    }

    // ── Content replacement and clearing ─────────────────────────────────────

    /**
     * Clears {@code selector} and appends a replacement UI file into it.
     */
    public static void replace(@Nonnull UICommandBuilder cmd,
            @Nonnull String selector,
            @Nonnull String uiPath) {
        cmd.clear(selector);
        cmd.append(selector, uiPath);
    }

    /**
     * Clears {@code selector}. Safe to call even when the element may already be
     * empty or absent — the server ignores a clear on a non-existent selector.
     */
    public static void safeClear(@Nonnull UICommandBuilder cmd, @Nonnull String selector) {
        cmd.clear(selector);
    }

    // ── EventData factory helpers ──────────────────────────────────────────────

    /**
     * Creates an {@link EventData} with a single key/value pair.
     * Convenience alias for {@link EventData#of(String, String)}.
     */
    @Nonnull
    public static EventData params(@Nonnull String key, @Nonnull String value) {
        return EventData.of(key, value);
    }

    /**
     * Creates an {@link EventData} from an existing map of key/value pairs.
     */
    @Nonnull
    public static EventData params(@Nonnull Map<String, String> values) {
        EventData data = new EventData();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            data.append(entry.getKey(), entry.getValue());
        }
        return data;
    }

    // ── Selector validation ────────────────────────────────────────────────────

    /**
     * Logs a warning for any required selector key whose value is null or blank.
     * Does not throw — the page will still build, but missing selectors will
     * silently no-op in the UI.
     *
     * @param uiPath    the UI file being validated (used only in the warning message)
     * @param selectors a map of logical name to selector string, e.g.
     *                  {@code Map.of("submit button", "#SubmitBtn")}
     */
    public static void ensureRequiredSelectors(@Nonnull String uiPath,
            @Nonnull Map<String, String> selectors) {
        for (Map.Entry<String, String> entry : selectors.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass()
                        .atWarning()
                        .log("[PhoneUi] Required selector '%s' is missing for UI path '%s'",
                                entry.getKey(), uiPath);
            }
        }
    }
}
