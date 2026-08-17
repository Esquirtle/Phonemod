package esq.phonemod.security;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UiEventDecoderTest {
    @Test
    void decodesClientScalarValuesAndNormalizesLiveKeys() {
        Map<String, String> decoded = UiEventDecoder.decode("""
                {"Action":"home","State":0,"@Enabled":true,"Ratio":1.25,"Ignored":null}
                """);

        assertEquals("home", decoded.get("Action"));
        assertEquals("0", decoded.get("State"));
        assertEquals("true", decoded.get("Enabled"));
        assertEquals("1.25", decoded.get("Ratio"));
        assertFalse(decoded.containsKey("Ignored"));
    }

    @Test
    void rejectsCompoundValues() {
        assertThrows(IllegalArgumentException.class,
                () -> UiEventDecoder.decode("{\"Action\":\"home\",\"Nested\":{}}"));
        assertThrows(IllegalArgumentException.class,
                () -> UiEventDecoder.decode("{\"Action\":\"home\",\"List\":[]}"));
    }
}
