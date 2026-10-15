package esq.phonemod.api.v3;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * F2 contract tests for {@link ViewModel}/{@link Prop}/{@link ListProp}
 * (R-API-4, R-KIT-5). All ROJO: every one asserts the real target behaviour and
 * fails on the F2 skeleton's uncaught {@code UnsupportedOperationException("F4")}
 * — starting with {@code new Model()} itself, since {@code ViewModel()} throws.
 */
@Tag("contract")
class ViewModelContractTest {

    private static final class Model extends ViewModel {
        final Prop<String> name = prop("Name", "a");
        final ListProp<String, String> items = list("Items", i -> i);
    }

    @Test
    void setWithSameValueDoesNotDirty() {
        Model m = new Model();
        m.name.markClean();
        m.name.set("a");
        assertFalse(m.name.dirty());
    }

    @Test
    void setWithDifferentValueDirtiesExactlyThatField() {
        Model m = new Model();
        m.name.markClean();
        m.items.markClean();
        m.name.set("b");
        assertTrue(m.name.dirty());
        assertFalse(m.items.dirty());
    }

    @Test
    void fieldsAreListedInDeclarationOrder() {
        Model m = new Model();
        List<ModelField> fields = m.fields();
        assertEquals(List.of("Name", "Items"), fields.stream().map(ModelField::name).toList());
    }

    @Test
    void listPutWithExistingKeyReplacesNotDuplicates() {
        Model m = new Model();
        m.items.put("x");
        m.items.put("x");
        assertEquals(1, m.items.size());
    }

    @Test
    void listIdentityIsTheKeyNotTheIndex() {
        Model m = new Model();
        m.items.put("x");
        assertTrue(m.items.find("x").isPresent());
    }

    @Test
    void listSetAllRejectsDuplicateKeys() {
        Model m = new Model();
        assertThrows(IllegalArgumentException.class, () -> m.items.setAll(List.of("x", "x")));
    }

    @Test
    void markCleanClearsDirty() {
        Model m = new Model();
        m.name.set("b");
        m.name.markClean();
        assertFalse(m.name.dirty());
    }

    @Test
    void attachedGuardRunsOnEveryReadAndWrite() {
        Model m = new Model();
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        m.attach(calls::incrementAndGet);
        m.name.get();
        m.name.set("c");
        assertTrue(calls.get() >= 2);
    }

    @Test
    void guardCanOnlyBeAttachedOnce() {
        Model m = new Model();
        m.attach(() -> {});
        assertThrows(IllegalStateException.class, () -> m.attach(() -> {}));
    }
}
