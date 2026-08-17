package esq.phonemod.security;

import esq.phonemod.phone.api.PhoneEventActions;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Validates the complete untrusted custom-UI event envelope. */
public final class UiEventValidator {
    private static final Pattern IDENTIFIER = Pattern.compile("[a-z0-9_.:-]{1,64}");
    private static final Pattern ADDRESS = Pattern.compile("[A-Za-z0-9+_.:@-]{1,64}");
    private static final Set<String> CONTACT_ACTIONS = Set.of(
            PhoneEventActions.OPEN_CHAT, PhoneEventActions.START_CALL,
            PhoneEventActions.REMOVE_CONTACT);

    private UiEventValidator() {}

    @Nonnull
    public static ValidationResult validateRaw(@Nonnull String raw, @Nonnull UiSafetyLimits config) {
        if (raw.getBytes(StandardCharsets.UTF_8).length > config.maxUiEventBytes()) {
            return ValidationResult.reject("payload_too_large");
        }
        return ValidationResult.ALLOWED;
    }

    @Nonnull
    public static ValidationResult validateFields(@Nonnull Map<String, String> fields,
                                                   @Nonnull UiSafetyLimits config) {
        if (fields.size() > config.maxUiParameters()) {
            return ValidationResult.reject("too_many_parameters");
        }
        for (Map.Entry<String, String> field : fields.entrySet()) {
            if (field.getKey().isBlank() || field.getKey().length() > config.maxUiKeyCharacters()) {
                return ValidationResult.reject("invalid_parameter_key");
            }
            if (field.getValue() == null || field.getValue().length() > config.maxUiValueCharacters()) {
                return ValidationResult.reject("invalid_parameter_value");
            }
        }
        String action = fields.get("Action");
        if (action == null || !IDENTIFIER.matcher(action).matches()) {
            return ValidationResult.reject("invalid_action");
        }
        String app = fields.get("App");
        if (app != null && !app.isBlank() && !IDENTIFIER.matcher(app).matches()) {
            return ValidationResult.reject("invalid_app_id");
        }
        if (PhoneEventActions.OPEN_APP.equals(action) && (app == null || app.isBlank())) {
            return ValidationResult.reject("missing_app_id");
        }
        if (CONTACT_ACTIONS.contains(action)) {
            String address = firstNonBlank(fields.get("Contact"), fields.get("DialNumber"));
            if (address == null || !ADDRESS.matcher(address).matches()) {
                return ValidationResult.reject("invalid_address");
            }
        }
        if (PhoneEventActions.SEND_MESSAGE.equals(action)) {
            String message = fields.get("MessageValue");
            if (message == null || message.isBlank() || message.length() > config.maxMessageCharacters()) {
                return ValidationResult.reject("invalid_message");
            }
        }
        if (PhoneEventActions.SAVE_CONTACT.equals(action)) {
            String address = fields.get("ContactFormNumber");
            String name = fields.get("ContactFormName");
            if (address == null || !ADDRESS.matcher(address).matches()) {
                return ValidationResult.reject("invalid_contact_address");
            }
            if (name == null || name.isBlank() || name.length() > config.maxContactNameCharacters()) {
                return ValidationResult.reject("invalid_contact_name");
            }
        }
        return ValidationResult.ALLOWED;
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first;
        return second != null && !second.isBlank() ? second : null;
    }

    public record ValidationResult(boolean allowed, @Nonnull String reason) {
        private static final ValidationResult ALLOWED = new ValidationResult(true, "allowed");
        public static ValidationResult reject(String reason) {
            return new ValidationResult(false, reason);
        }
    }
}
