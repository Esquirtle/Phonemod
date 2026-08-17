package esq.phonemod.security;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UiEventValidatorTest {
    private final UiSafetyLimits config = new UiSafetyLimits() {
        public int maxUiEventBytes() { return 8192; }
        public int maxUiParameters() { return 32; }
        public int maxUiKeyCharacters() { return 64; }
        public int maxUiValueCharacters() { return 1024; }
        public int maxMessageCharacters() { return 512; }
        public int maxContactNameCharacters() { return 64; }
    };

    @Test
    void acceptsValidOpenAndMessageActions() {
        assertTrue(UiEventValidator.validateFields(
                Map.of("Action", "open_app", "App", "phonemod:whatgram"), config).allowed());
        assertTrue(UiEventValidator.validateFields(
                Map.of("Action", "send_message", "MessageValue", "hello"), config).allowed());
    }

    @Test
    void rejectsOversizedAndMalformedEnvelopes() {
        assertFalse(UiEventValidator.validateRaw("x".repeat(8193), config).allowed());
        assertFalse(UiEventValidator.validateFields(Map.of("Action", "../../bad"), config).allowed());
        assertFalse(UiEventValidator.validateFields(Map.of("Action", "open_app"), config).allowed());
        assertFalse(UiEventValidator.validateFields(
                Map.of("Action", "send_message", "MessageValue", "x".repeat(513)), config).allowed());

        Map<String, String> tooMany = new HashMap<>();
        tooMany.put("Action", "home");
        for (int i = 0; i < 32; i++) tooMany.put("p" + i, "v");
        assertFalse(UiEventValidator.validateFields(tooMany, config).allowed());
    }
}
