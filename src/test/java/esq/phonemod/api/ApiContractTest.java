package esq.phonemod.api;

import org.junit.jupiter.api.Test;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

final class ApiContractTest {
    @Test void descriptorRequiresCanonicalIdAndDefensivelyCopies() {
        Set<String> aliases = new java.util.HashSet<>(Set.of("old"));
        AppDescriptor descriptor = new AppDescriptor("example:app", aliases, "example.app",
                null, "Pages/App.ui", null, Set.of("apps"), Set.of(), Set.of("phone"), false, 4);
        aliases.add("later");
        assertEquals(Set.of("old"), descriptor.legacyAliases());
        assertTrue(descriptor.supports("phone"));
        assertFalse(descriptor.supports("pc"));
        assertThrows(IllegalArgumentException.class, () -> new AppDescriptor("simple", Set.of(),
                "x", null, "x.ui", null, Set.of(), Set.of(), Set.of(), false, 0));
    }

    @Test void actionValidatesAndFreezesEnvelope() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("Value", "safe");
        DeviceAction action = new DeviceAction("example_action", "example:app", params, 7);
        params.put("Injected", "later");
        assertEquals(Map.of("Value", "safe"), action.parameters());
        assertThrows(UnsupportedOperationException.class,
                () -> action.parameters().put("x", "y"));
        assertThrows(IllegalArgumentException.class,
                () -> new DeviceAction("BAD ACTION", "example:app", Map.of(), 0));
    }
}
