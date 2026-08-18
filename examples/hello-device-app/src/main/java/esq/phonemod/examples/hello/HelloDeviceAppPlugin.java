package esq.phonemod.examples.hello;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import esq.phonemod.api.AppRegistration;
import esq.phonemod.api.DevicePlatform;
import esq.phonemod.api.event.AppOpenEvent;
import esq.phonemod.api.event.DeviceOpenEvent;

import javax.annotation.Nonnull;

/**
 * Entry point for the Hello Device App example.
 *
 * <p>Shows the two things every Phonemod consumer does: register a
 * {@link esq.phonemod.api.DeviceApp} against the platform, and (optionally)
 * subscribe to the published device events.
 */
public final class HelloDeviceAppPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public HelloDeviceAppPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        // require() throws if the Phonemod runtime is absent. Because the manifest
        // declares a hard dependency on esq.phonemod:Phonemod, it is always present
        // by the time our setup() runs.
        AppRegistration registration =
                DevicePlatform.require().apps().registerApp(this, new HelloDeviceApp());
        LOGGER.atInfo().log("[HelloDeviceApp] registered id=%s active=%s",
                registration.appId(), registration.active());

        // The published events go through the shared Hytale EventBus, so they are
        // registered exactly like any other IEvent. Registering through the plugin's
        // own registry means they are unsubscribed automatically on disable.
        //
        // NOTE the fully-qualified intent: esq.phonemod.api.event.DeviceOpenEvent is
        // the PUBLIC event. The runtime has an unrelated internal class of the same
        // simple name (esq.phonemod.device.events.DeviceOpenEvent) that consumers
        // never see.
        getEventRegistry().registerGlobal(DeviceOpenEvent.Post.class, event ->
                LOGGER.atInfo().log("[HelloDeviceApp] device opened by %s address=%s",
                        event.playerId(), event.device().optionalAddress().orElse("-")));

        // "Pre" events are cancellable — this is the authorization hook. Setting
        // cancelled stops the open before it happens.
        getEventRegistry().registerGlobal(AppOpenEvent.Pre.class, event -> {
            if ("example:blocked".equals(event.appId())) {
                event.setCancelled(true);
            }
        });
    }

    @Override
    protected void shutdown() {
        // No explicit AppRegistration.close() needed: the registration is bound to
        // this plugin's task registry and is closed for us.
        LOGGER.atInfo().log("[HelloDeviceApp] shut down");
    }
}
