package esq.phonemod.examples.hello;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import esq.phonemod.api.AppDescriptor;
import esq.phonemod.api.DeviceAction;
import esq.phonemod.api.DeviceActionResult;
import esq.phonemod.api.DeviceApp;
import esq.phonemod.api.DeviceAppContext;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * A complete, minimal v2 {@link DeviceApp}.
 *
 * <p><b>This app renders synchronously and only synchronously.</b> Everything
 * {@code build} needs is already in hand: {@link DeviceAppContext#identity()},
 * {@link DeviceAppContext#address()} and {@link DeviceAppContext#state(String)} are all
 * plain in-memory reads. There is deliberately no async fetch here, because a v2
 * {@code DeviceApp} has no way to request a repaint after one:
 * {@code AppService.refreshApp(appId, phoneNumber)} is keyed on a phone number, and
 * {@code DeviceAppContext} exposes no phone number — only
 * {@code address() -> Optional<String>}. See {@code docs/V2_API.md} for the full note.
 *
 * <p>The app object is a singleton shared by every player, so it holds no per-player
 * fields. Per-player values live in {@link DeviceAppContext#state}, which the framework
 * scopes to this device + app.
 */
public final class HelloDeviceApp implements DeviceApp {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Action names must match {@code [a-z0-9_.:-]{1,64}} (DeviceAction.java:11). */
    private static final String ACTION_GREET = "hello_greet";
    private static final String ACTION_SET_NAME = "hello_set_name";
    private static final String ACTION_BACK = "hello_back";

    /** Captured client input arrives under this parameter key. */
    private static final String PARAM_NAME = "NameText";

    private static final String STATE_VIEW = "view";
    private static final String STATE_NAME = "name";
    private static final String VIEW_DETAIL = "detail";
    private static final String VIEW_MAIN = "main";

    /**
     * The canonical id must be namespaced — {@code [a-z0-9_.-]+:[a-z0-9_.-]+}
     * (AppDescriptor.java:22). Field order is
     * (id, legacyAliases, localizationKey, iconPath, uiPath, appButtonUiPath,
     * capabilities, permissions, supportedDeviceTypes, system, sortOrder).
     */
    private static final AppDescriptor DESCRIPTOR = new AppDescriptor(
            "example:hello",
            Set.of("hello"),
            "hellodevice.name",
            null,
            "Pages/Phone/HelloDeviceApp/Main.ui",
            "Pages/Phone/HelloDeviceApp/AppButton.ui",
            Set.of("apps"),
            Set.of(),
            Set.of("phone"),
            false,
            50);

    @Override
    public @Nonnull AppDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void build(@Nonnull DeviceAppContext context,
                      @Nonnull UICommandBuilder commands,
                      @Nonnull UIEventBuilder events) {
        boolean detail = VIEW_DETAIL.equals(context.state(STATE_VIEW).orElse(VIEW_MAIN));

        commands.set("#MainView.Visible", !detail);
        commands.set("#DetailView.Visible", detail);
        commands.set("#TitleLabel.Text", Message.translation("hellodevice.title"));

        // address() is Optional — a shared device may have no assigned address yet.
        commands.set("#AddressLabel.Text", context.address().orElse("(no address)"));
        commands.set("#GreetingLabel.Text", context.state(STATE_NAME).orElse("(nobody yet)"));

        bind(events, "#GreetButton", EventData.of("Action", ACTION_GREET));
        // "@" prefixes a param whose value the client reads from a UI element.
        bind(events, "#SaveNameButton", EventData.of("Action", ACTION_SET_NAME)
                .append("@" + PARAM_NAME, "#NameField.Value"));
        bind(events, "#BackButton", EventData.of("Action", ACTION_BACK));
    }

    @Override
    public @Nonnull DeviceActionResult handle(@Nonnull DeviceAppContext context,
                                              @Nonnull DeviceAction action) {
        LOGGER.atInfo().log("[HelloDeviceApp] app=%s device=%s action=%s",
                DESCRIPTOR.id(), context.identity().id(), action.action());

        return switch (action.action()) {
            case ACTION_GREET -> {
                context.notify("Hello", "Hello, " + context.state(STATE_NAME).orElse("stranger") + "!");
                yield DeviceActionResult.HANDLED;
            }
            case ACTION_SET_NAME -> {
                // Always re-validate client input, however small.
                String name = action.parameters().getOrDefault(PARAM_NAME, "").trim();
                if (name.isEmpty() || name.length() > 32) {
                    yield DeviceActionResult.REJECTED;
                }
                context.state(STATE_NAME, name);
                context.state(STATE_VIEW, VIEW_DETAIL);
                yield DeviceActionResult.RERENDER;
            }
            case ACTION_BACK -> {
                context.state(STATE_VIEW, VIEW_MAIN);
                yield DeviceActionResult.RERENDER;
            }
            default -> DeviceActionResult.REJECTED;
        };
    }

    @Override
    public void onOpen(@Nonnull DeviceAppContext context) {
        // Touching the ECS Store is only legal on the owning world thread; the
        // context schedules the callback there and hands you the Ref + Store.
        context.executeOnWorld(world ->
                LOGGER.atFine().log("[HelloDeviceApp] opened, ref valid=%s", world.ref().isValid()));
    }

    private static void bind(UIEventBuilder events, String selector, EventData data) {
        events.addEventBinding(CustomUIEventBindingType.Activating, selector, data, true);
    }
}
