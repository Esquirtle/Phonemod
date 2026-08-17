package esq.phonemod.phone.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic event payload for phone UI interactions.
 */
@Deprecated(since = "2.0", forRemoval = false)
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

    /**
     * The clicked slot index for an ItemGrid slot-click event, or {@code -1} if the event
     * carries no {@code SlotIndex} (i.e. it was not a slot click). The client appends the
     * index automatically to a slot-click binding — bind one with {@link PhoneUi#bindSlot}.
     */
    public int slotIndex() {
        String raw = params.get("SlotIndex");
        if (raw == null) return -1;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
