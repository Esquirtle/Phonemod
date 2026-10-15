package esq.phonemod.api.v3;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * F2 contract tests for {@link DeviceType} (R-API-8) and {@link DeviceSessionId}
 * (R-API-6). Per plan sec.3 "todos ROJO", except where noted — see
 * "Desviaciones" in the implementer's report: {@code DeviceType}'s compact
 * constructor is real, not F4 (it owns the self-typed static constants PHONE/PC,
 * see {@link DeviceType}'s javadoc), so {@code invalidIdRejected} legitimately
 * passes rather than failing with F4. Every other test asserts the real target
 * behaviour and lets the F2 skeleton's uncaught
 * {@code UnsupportedOperationException("F4")} fail it.
 */
@Tag("contract")
class DeviceTypeContractTest {

    @Test
    void builtinConstantsMatchAssetIds() {
        assertEquals("phone", DeviceType.PHONE.id());
        assertEquals("pc", DeviceType.PC.id());
        // F4: threading a builtin constant through the descriptor builder.
        AppDescriptor descriptor = AppDescriptor.builder("esq.x:y")
                .deviceTypes(Set.of(DeviceType.PHONE)).build();
        assertTrue(descriptor.supports(DeviceType.PHONE));
    }

    @Test
    void assetDefinedTypeIsValueEqual() {
        DeviceType a = DeviceType.of("custom_shell");
        DeviceType b = new DeviceType("custom_shell");
        assertEquals(a, b);
        // F4: threading an asset-defined type through the descriptor builder.
        AppDescriptor descriptor = AppDescriptor.builder("esq.x:y").deviceTypes(Set.of(a)).build();
        assertTrue(descriptor.supports(b));
    }

    @Test
    void invalidIdRejected() {
        // DEVIATION: DeviceType validates for real in F2 (see class javadoc); this
        // assertion legitimately passes rather than failing with F4.
        assertThrows(IllegalArgumentException.class, () -> DeviceType.of("Not Valid!"));
    }

    @Test
    void sessionIdRoundTripsThroughString() {
        // F4: DeviceSessionId can't even be constructed yet.
        UUID raw = UUID.randomUUID();
        DeviceSessionId id = new DeviceSessionId(raw);
        assertEquals(id, DeviceSessionId.parse(id.toString()));
    }

    @Test
    void sessionIdIsStableForSameDeviceAndPlayer() {
        // F4: DeviceSessionId.of must derive the same id for the same inputs (D-26).
        esq.phonemod.api.DeviceIdentity device =
                new esq.phonemod.api.DeviceIdentity(UUID.randomUUID(), null, false);
        DeviceSessionId first = DeviceSessionId.of(device, null);
        DeviceSessionId second = DeviceSessionId.of(device, null);
        assertEquals(first, second);
    }
}
