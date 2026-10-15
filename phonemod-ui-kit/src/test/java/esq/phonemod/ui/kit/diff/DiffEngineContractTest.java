package esq.phonemod.ui.kit.diff;

import esq.phonemod.api.v3.ListProp;
import esq.phonemod.api.v3.Prop;
import esq.phonemod.api.v3.ViewModel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * F2 contract tests for {@link DiffEngine} (R-KIT-1). All ROJO: each asserts
 * real target behaviour and fails on the F2 skeleton's uncaught
 * {@code UnsupportedOperationException("F4")} — starting with {@code new
 * Model()} itself, since {@code ViewModel()} throws.
 */
@Tag("contract")
class DiffEngineContractTest {

    private static final class Model extends ViewModel {
        final Prop<String> name = prop("Name", "a");
        final ListProp<String, String> items = list("Items", i -> i);
    }

    @Test
    void firstDiffEmitsFullStateInDeclarationOrder() {
        Model m = new Model();
        List<DiffOp> ops = DiffEngine.diff(m);
        assertFalse(ops.isEmpty());
        assertEquals("Name", ops.get(0).field());
    }

    @Test
    void secondDiffWithoutChangesIsEmpty() {
        Model m = new Model();
        DiffEngine.diff(m);
        assertTrue(DiffEngine.diff(m).isEmpty());
    }

    @Test
    void singlePropChangeEmitsOneSetProp() {
        Model m = new Model();
        DiffEngine.diff(m);
        m.name.set("b");
        List<DiffOp> ops = DiffEngine.diff(m);
        assertEquals(1, ops.size());
        assertInstanceOf(DiffOp.SetProp.class, ops.get(0));
    }

    @Test
    void samePropValueEmitsNothing() {
        Model m = new Model();
        DiffEngine.diff(m);
        m.name.set("a");
        assertTrue(DiffEngine.diff(m).isEmpty());
    }

    @Test
    void appendedItemEmitsInsertItemAtIndex() {
        Model m = new Model();
        DiffEngine.diff(m);
        m.items.put("x");
        List<DiffOp> ops = DiffEngine.diff(m);
        assertInstanceOf(DiffOp.InsertItem.class, ops.get(0));
    }

    @Test
    void removedItemEmitsRemoveItemByKey() {
        Model m = new Model();
        m.items.put("x");
        DiffEngine.diff(m);
        m.items.remove("x");
        List<DiffOp> ops = DiffEngine.diff(m);
        assertInstanceOf(DiffOp.RemoveItem.class, ops.get(0));
    }

    @Test
    void changedItemEmitsUpdateItemNotRemoveInsert() {
        Model m = new Model();
        m.items.put("x");
        DiffEngine.diff(m);
        m.items.put("x");
        List<DiffOp> ops = DiffEngine.diff(m);
        assertInstanceOf(DiffOp.UpdateItem.class, ops.get(0));
    }

    @Test
    void reorderEmitsRemoveThenInsert() {
        Model m = new Model();
        m.items.put("x");
        m.items.put("y");
        DiffEngine.diff(m);
        m.items.setAll(List.of("y", "x"));
        List<DiffOp> ops = DiffEngine.diff(m);
        assertInstanceOf(DiffOp.RemoveItem.class, ops.get(0));
        assertInstanceOf(DiffOp.InsertItem.class, ops.get(1));
    }

    @Test
    void mixedChangesEmitCanonicalOrder() {
        Model m = new Model();
        m.items.put("x");
        DiffEngine.diff(m);
        m.name.set("b");
        m.items.put("y");
        List<DiffOp> ops = DiffEngine.diff(m);
        assertInstanceOf(DiffOp.SetProp.class, ops.get(0));
        assertInstanceOf(DiffOp.InsertItem.class, ops.get(1));
    }

    @Test
    void diffMarksFieldsClean() {
        Model m = new Model();
        DiffEngine.diff(m);
        assertFalse(m.name.dirty());
    }
}
