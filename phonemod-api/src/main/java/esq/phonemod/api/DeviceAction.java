package esq.phonemod.api;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Immutable validated event envelope delivered to a device app. */
public record DeviceAction(@Nonnull String action, @Nonnull String appId,
                           @Nonnull Map<String, String> parameters, long sequence) {
    private static final Pattern ID = Pattern.compile("[a-z0-9_.:-]{1,64}");

    public DeviceAction {
        if (action == null || !ID.matcher(action).matches())
            throw new IllegalArgumentException("Invalid action");
        if (appId == null || !ID.matcher(appId).matches())
            throw new IllegalArgumentException("Invalid app id");
        if (parameters == null || parameters.size() > 32)
            throw new IllegalArgumentException("Too many parameters");
        LinkedHashMap<String, String> copy = new LinkedHashMap<>();
        parameters.forEach((key, value) -> {
            if (key == null || key.isBlank() || key.length() > 64
                    || value == null || value.length() > 1024)
                throw new IllegalArgumentException("Invalid action parameter");
            copy.put(key, value);
        });
        parameters = Map.copyOf(copy);
        if (sequence < 0) throw new IllegalArgumentException("sequence must be non-negative");
    }
}
