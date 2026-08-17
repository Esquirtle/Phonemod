package esq.phonemod.identity;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

final class IdentityRegistryTest {
    @Test void duplicateLegacyClaimPreservesFirstOwnerAndRemapsSecond() {
        IdentityRegistry registry = new IdentityRegistry(false);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        var original = registry.claimLegacy(null, "555-0100", first);
        var duplicate = registry.claimLegacy(null, "555-0100", second);
        assertEquals("555-0100", original.identity().address());
        assertNotEquals(original.identity().id(), duplicate.identity().id());
        assertNotEquals("555-0100", duplicate.identity().address());
        assertTrue(duplicate.conflict());
        assertEquals(1, registry.conflictCount());
    }

    @Test void repeatedClaimIsIdempotentAndTransferKeepsIdentity() {
        IdentityRegistry registry = new IdentityRegistry(false);
        UUID owner = UUID.randomUUID();
        var first = registry.claimLegacy(null, "555-0200", owner);
        var again = registry.claimLegacy(null, "555-0200", owner);
        assertEquals(first.identity(), again.identity());
        UUID recipient = UUID.randomUUID();
        assertTrue(registry.transfer(first.identity().id(), owner, recipient));
        assertEquals(first.identity().id(), registry.findAddress("555-0200").orElseThrow().id());
        assertFalse(registry.transfer(first.identity().id(), owner, UUID.randomUUID()));
    }

    @Test void sharedStableKeyIsAddressOptionalAndDeterministic() {
        IdentityRegistry registry = new IdentityRegistry(false);
        var first = registry.ensureShared("world:terminal:1");
        var second = registry.ensureShared("world:terminal:1");
        assertEquals(first, second);
        assertTrue(first.shared());
        assertNull(first.address());
    }

    @Test void playerBindingReusesOnePrimaryIdentityAcrossDevices() {
        IdentityRegistry registry = new IdentityRegistry(false);
        UUID owner = UUID.randomUUID();
        var first = registry.claimPlayer(owner);
        var second = registry.claimPlayer(owner);
        assertEquals(first.identity(), second.identity());
        assertNotNull(first.identity().address());
        assertEquals(1, registry.snapshot().size());
    }

    @Test void playerBindingDoesNotDestroyExistingDeviceIdentities() {
        IdentityRegistry registry = new IdentityRegistry(false);
        UUID owner = UUID.randomUUID();
        var firstDevice = registry.claimLegacy(null, "555-0300", owner);
        var secondDevice = registry.claimLegacy(null, "555-0301", owner);

        var primary = registry.claimPlayer(owner);

        assertTrue(primary.identity().equals(firstDevice.identity())
                || primary.identity().equals(secondDevice.identity()));
        assertEquals(secondDevice.identity(), registry.findAddress("555-0301").orElseThrow());
        assertEquals(2, registry.snapshot().size());
    }
}
