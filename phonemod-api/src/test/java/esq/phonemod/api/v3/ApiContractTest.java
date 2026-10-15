package esq.phonemod.api.v3;

import esq.phonemod.api.AppRegistration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * F2 contract tests for the v3 API surface (plan sec.3). Per the plan: 4 VERDE
 * (signature pins, which do not invoke behaviour) and the rest ROJO (they
 * exercise real behaviour and hit the F2 skeleton's
 * {@code UnsupportedOperationException("F4")}).
 */
@Tag("contract")
class ApiContractTest {

    // ---- ROJO: R-API-2 — v2 removal only happens at the F4 promotion step ----
    // These assert the REAL target state (types gone); in F2 they fail on
    // purpose, because the promotion step (plan sec.1) has not run yet.

    @Test
    void removedV2TypesAreAbsent() {
        for (String fqcn : new String[] {
                "esq.phonemod.phone.api.PhoneApp",
                "esq.phonemod.phone.api.StatefulPhoneApp",
                "esq.phonemod.phone.api.PhoneUi",
                "esq.phonemod.phone.api.PhoneAppContext"}) {
            try {
                Class.forName(fqcn);
                fail("F4: v2 type " + fqcn + " is only deleted by the F4 promotion step (plan sec.1); still present in F2");
            } catch (ClassNotFoundException expected) {
                // would be the real F4 target state; not reached in F2
            }
        }
    }

    @Test
    void v2DeviceAppAndLegacyRegistrationAreGone() {
        try {
            Class.forName("esq.phonemod.phone.sync.LegacyRooms");
            fail("F4: LegacyRooms is only deleted by the F4 promotion step; still present in F2");
        } catch (ClassNotFoundException expected) {
            // real F4 target state; not reached in F2
        }
        boolean hasRegisterLegacyApp = Set.of(esq.phonemod.api.AppService.class.getMethods())
                .stream().anyMatch(m -> m.getName().equals("registerLegacyApp"));
        if (hasRegisterLegacyApp) {
            fail("F4: AppService.registerLegacyApp is only removed by the F4 promotion step; still present in F2");
        }
    }

    // ---- VERDE: signature pins (no behaviour invoked) ----

    @Test
    void deviceAppSignatureIsPinned() throws NoSuchMethodException {
        assertTrue(DeviceApp.class.isInterface());
        assertEquals(AppDescriptor.class, DeviceApp.class.getMethod("descriptor").getReturnType());
        Method createModel = DeviceApp.class.getMethod("createModel", DeviceAppContext.class);
        assertEquals(ViewModel.class, createModel.getReturnType());
        assertNotNull(DeviceApp.class.getMethod("onOpen", DeviceAppContext.class, ViewModel.class));
        assertNotNull(DeviceApp.class.getMethod("onAction", DeviceAppContext.class, ViewModel.class, DeviceAction.class));
        Method onClose = DeviceApp.class.getMethod("onClose", DeviceAppContext.class, ViewModel.class);
        assertTrue(onClose.isDefault(), "onClose must have a default no-op body");
    }

    @Test
    void noApiMethodExposesUiTypes() {
        for (Class<?> type : new Class<?>[] {DeviceApp.class, DeviceAppContext.class, AppDescriptor.class,
                AppService.class, ViewModel.class, Prop.class, ListProp.class}) {
            for (Method m : type.getMethods()) {
                assertNoUiType(type, m.getReturnType());
                for (Class<?> p : m.getParameterTypes()) {
                    assertNoUiType(type, p);
                }
            }
        }
    }

    private static void assertNoUiType(Class<?> owner, Class<?> candidate) {
        String n = candidate.getName();
        assertFalse(n.startsWith("com.hypixel.hytale.server.core.ui.")
                        || n.startsWith("com.hypixel.hytale.server.core.modules.ui.")
                        || n.contains("InteractiveCustomUIPage") || n.contains("PageManager"),
                owner.getName() + " exposes a UI type: " + n);
    }

    @Test
    void contextSignatureIsPinned() throws NoSuchMethodException {
        assertNotNull(DeviceAppContext.class.getMethod("player"));
        assertNotNull(DeviceAppContext.class.getMethod("identity"));
        assertNotNull(DeviceAppContext.class.getMethod("sessionId"));
        assertNotNull(DeviceAppContext.class.getMethod("deviceType"));
        assertEquals(String.class, DeviceAppContext.class.getMethod("address").getReturnType());
        assertNotNull(DeviceAppContext.class.getMethod("refresh"));
        assertNotNull(DeviceAppContext.class.getMethod("onWorld", java.util.function.Consumer.class));
        assertNotNull(DeviceAppContext.class.getMethod("sync", esq.phonemod.phone.sync.SyncChannelType.class));
        assertNotNull(DeviceAppContext.class.getMethod("notify", String.class, String.class));
        assertNotNull(DeviceAppContext.class.getMethod("state"));
    }

    @Test
    void appServiceSignatureIsPinned() throws NoSuchMethodException {
        Method register = AppService.class.getMethod("registerApp", DeviceApp.class);
        assertEquals(AppRegistration.class, register.getReturnType(),
                "AppService.registerApp must return esq.phonemod.api.AppRegistration (D-28)");
        Method refreshApp = AppService.class.getMethod("refreshApp", DeviceSessionId.class);
        assertEquals(void.class, refreshApp.getReturnType());
    }

    // ---- ROJO: R-API-5 ----

    @Test
    void actionAcceptsOnlyPrimitiveWrappers() {
        assertEquals(Set.of(String.class, Integer.class, Long.class, Double.class, Boolean.class),
                DeviceAction.ALLOWED_TYPES);
        // F4: DeviceAction.of is unimplemented; a non-wrapper value should be
        // rejected for real once it is, instead of propagating F4 here.
        DeviceAction action = DeviceAction.of("some.command", Map.of("flag", Character.valueOf('x')));
        assertTrue(action.params().isEmpty());
    }

    @Test
    void actionIsImmutableAndValidated() {
        // F4: should build a validated, defensively-copied DeviceAction.
        DeviceAction action = DeviceAction.of("some.command", Map.of("key", "value"));
        assertEquals("some.command", action.command());
        assertEquals("value", action.params().get("key"));
    }

    @Test
    void actionTypedAccessorsAreStrict() {
        // F4: string()/integer()/... should return Optional, strictly typed.
        DeviceAction action = DeviceAction.of("some.command", Map.of("count", 3));
        assertEquals(java.util.Optional.of(3), action.integer("count"));
        assertEquals(java.util.Optional.empty(), action.string("count"));
    }

    @Test
    void sampleAppCompilesAgainstApiOnly() {
        // The fixture compiles and links against phonemod-api alone (R-API-3);
        // building its descriptor hits the F2 AppDescriptor.Builder skeleton
        // (an uncaught UnsupportedOperationException("F4") fails this test).
        HelloContractApp app = new HelloContractApp();
        assertEquals("esq.hello:hello", app.descriptor().id());
    }
}
