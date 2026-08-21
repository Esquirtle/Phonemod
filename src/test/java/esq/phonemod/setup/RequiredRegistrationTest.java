package esq.phonemod.setup;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fail-fast ordering contract of {@link RequiredRegistration}: all-passing runs every step;
 * the first throwing step wraps+rethrows naming that step and skips the rest; only
 * successfully-applied step names are recorded.
 */
final class RequiredRegistrationTest {

    @Test
    void allPassingStepsRunInOrderAndAreAllRecorded() {
        List<String> order = new ArrayList<>();
        RequiredRegistration sequence = new RequiredRegistration()
                .step("a", () -> order.add("a"))
                .step("b", () -> order.add("b"))
                .step("c", () -> order.add("c"));

        sequence.run();

        assertEquals(List.of("a", "b", "c"), order);
        assertEquals(List.of("a", "b", "c"), sequence.appliedSteps());
    }

    @Test
    void aThrowingStepIsWrappedByNameAndAbortsLaterSteps() {
        List<String> order = new ArrayList<>();
        RequiredRegistration sequence = new RequiredRegistration()
                .step("first", () -> order.add("first"))
                .step("boom", () -> {
                    throw new RuntimeException("nope");
                })
                .step("never", () -> order.add("never"));

        IllegalStateException error = assertThrows(IllegalStateException.class, sequence::run);

        assertEquals("Required boom registration failed", error.getMessage());
        assertNotNull(error.getCause());
        assertEquals("nope", error.getCause().getMessage());
        assertEquals(List.of("first"), order, "the step after the failure must not run");
        assertEquals(List.of("first"), sequence.appliedSteps(),
                "appliedSteps records only steps that succeeded");
    }

    @Test
    void theFirstFailureWinsAndNoStepIsRecorded() {
        RequiredRegistration sequence = new RequiredRegistration()
                .step("one", () -> {
                    throw new IllegalArgumentException("1");
                })
                .step("two", () -> {
                    throw new IllegalStateException("2");
                });

        IllegalStateException error = assertThrows(IllegalStateException.class, sequence::run);

        assertEquals("Required one registration failed", error.getMessage());
        assertEquals("1", error.getCause().getMessage());
        assertTrue(sequence.appliedSteps().isEmpty());
    }
}
