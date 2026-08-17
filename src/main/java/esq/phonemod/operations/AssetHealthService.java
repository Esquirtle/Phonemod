package esq.phonemod.operations;

import esq.phonemod.device.assets.DeviceAsset;
import esq.phonemod.device.assets.DeviceThemeAsset;
import esq.phonemod.device.core.DeviceShell;
import esq.phonemod.phone.core.PhoneService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Strict validation for device definitions that would otherwise fail only when opened. */
public final class AssetHealthService {
    private static final Pattern SELECTOR = Pattern.compile("#[A-Za-z_][A-Za-z0-9_-]*");
    private volatile Report report = new Report(false, 0, List.of("not validated"));

    public Report validate() {
        List<String> errors = new ArrayList<>();
        var assets = DeviceAsset.getAssetMap().getAssetMap();
        if (assets.isEmpty()) errors.add("no device assets loaded");
        assets.forEach((id, asset) -> validateDevice(id, asset, errors));
        report = new Report(errors.isEmpty(), assets.size(), List.copyOf(errors));
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Device asset validation failed: " + String.join("; ", errors));
        }
        return report;
    }

    private static void validateDevice(String id, DeviceAsset asset, List<String> errors) {
        DeviceShell shell;
        try {
            shell = new DeviceShell(asset);
        } catch (IllegalArgumentException e) {
            errors.add(id + ": " + e.getMessage());
            return;
        }
        requireSelector(id, "ContentSelector", shell.getContentSelector(), errors);
        requireSelector(id, "HomeHolderSelector", shell.getHomeHolderSelector(), errors);
        if (shell.getHomeButtonSelector() != null)
            requireSelector(id, "HomeButtonSelector", shell.getHomeButtonSelector(), errors);
        shell.getSelectors().forEach((role, selector) -> {
            if (role == null || role.isBlank()) errors.add(id + ": blank selector role");
            requireSelector(id, "Selectors." + role, selector, errors);
        });
        if (!resourceExists("Common/UI/Custom/" + shell.getShellUiPath()))
            errors.add(id + ": missing shell UI " + shell.getShellUiPath());
        if (shell.getDefaultThemeId() == null
                || DeviceThemeAsset.getAssetMap().getAsset(shell.getDefaultThemeId()) == null)
            errors.add(id + ": unknown DefaultThemeId " + shell.getDefaultThemeId());
        Set<String> uniqueApps = new HashSet<>();
        for (String appId : shell.getDefaultApps()) {
            if (appId == null || appId.isBlank() || !uniqueApps.add(appId)) {
                errors.add(id + ": invalid or duplicate default app " + appId);
                continue;
            }
            var app = PhoneService.get().getApp(appId);
            if (app == null) errors.add(id + ": unknown default app " + appId);
            else if (!supports(app.getSupportedDeviceTypes(), shell.getDeviceType()))
                errors.add(id + ": app " + appId + " does not support " + shell.getDeviceType());
        }
        for (String capability : shell.getCapabilities()) {
            if (capability.isBlank()) errors.add(id + ": blank capability");
        }
    }

    private static boolean supports(String[] supported, String deviceType) {
        return supported.length == 0 || java.util.Arrays.asList(supported).contains(deviceType);
    }

    private static void requireSelector(String id, String field, String selector, List<String> errors) {
        if (selector == null || !SELECTOR.matcher(selector).matches())
            errors.add(id + ": invalid " + field + " " + selector);
    }

    private static boolean resourceExists(String path) {
        return AssetHealthService.class.getClassLoader().getResource(path) != null;
    }

    public Report report() { return report; }
    public record Report(boolean healthy, int deviceCount, List<String> errors) {}
}
