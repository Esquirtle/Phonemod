package esq.phonemod.phone.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic event payload for phone UI interactions.
 */
public final class PhoneEvent {

    private final String action;
    private final String appId;
    private final Map<String, String> params;

    public PhoneEvent(@Nonnull String action,
                      @Nonnull String appId,
                      Map<String, String> params) {
        this.action = action;
        this.appId = appId;
        this.params = Collections.unmodifiableMap(new HashMap<>(params));
    }

    @Nonnull
    public String getAction() {
        return action;
    }

    @Nonnull
    public String getAppId() {
        return appId;
    }

    public Map<String, String> getParams() {
        return params;
    }

    @Nullable
    public String getParam(String key) {
        return params.get(key);
    }

    public String getParam(String key, String defaultValue) {
        return params.getOrDefault(key, defaultValue);
    }
}
