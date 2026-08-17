package esq.phonemod.platform;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import esq.phonemod.api.AppDescriptor;
import esq.phonemod.api.DeviceAction;
import esq.phonemod.api.DeviceActionResult;
import esq.phonemod.api.DeviceApp;
import esq.phonemod.phone.api.PhoneApp;
import esq.phonemod.phone.api.PhoneAppContext;
import esq.phonemod.phone.api.PhoneEvent;

import java.util.Map;

/** Deprecated v1 runtime bridge for a v2 DeviceApp. */
final class DeviceAppAdapter implements PhoneApp<DeviceAppAdapter.State> {
    enum State { DEFAULT }
    private final DeviceApp delegate;
    private final AppDescriptor descriptor;
    DeviceAppAdapter(DeviceApp delegate) { this.delegate = delegate; this.descriptor = delegate.descriptor(); }

    public String getId() { return descriptor.id(); }
    public String getDisplayName() { return descriptor.localizationKey(); }
    public String getAppButtonUI() { return descriptor.appButtonUiPath(); }
    public String getUIPath() { return descriptor.uiPath(); }
    public String getIconPath() { return descriptor.iconPath(); }
    public int getSortOrder() { return descriptor.sortOrder(); }
    public String getRequiredPermission() { return descriptor.permissions().stream().findFirst().orElse(null); }
    public String[] getSupportedDeviceTypes() { return descriptor.supportedDeviceTypes().toArray(String[]::new); }
    public boolean isSystemApp() { return descriptor.system(); }
    public void onOpen(PhoneAppContext context, UICommandBuilder cmd, UIEventBuilder evb) {
        delegate.onOpen(new RuntimeDeviceAppContext(context));
    }
    public void build(PhoneAppContext context, UICommandBuilder cmd, UIEventBuilder evb) {
        cmd.clear(context.getContentSelector());
        cmd.append(context.getContentSelector(), descriptor.uiPath());
        delegate.build(new RuntimeDeviceAppContext(context), cmd, evb);
    }
    public boolean handleEvent(PhoneAppContext context, PhoneEvent event,
                               UICommandBuilder cmd, UIEventBuilder evb) {
        DeviceActionResult result = delegate.handle(new RuntimeDeviceAppContext(context),
                new DeviceAction(event.getAction(), descriptor.id(), event.getParams(), 0));
        if (result == DeviceActionResult.RERENDER) build(context, cmd, evb);
        return result != DeviceActionResult.REJECTED;
    }
    public void onClose(PhoneAppContext context) { delegate.onClose(new RuntimeDeviceAppContext(context)); }
}
