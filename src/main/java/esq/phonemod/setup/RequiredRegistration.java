package esq.phonemod.setup;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Fail-fast sequencer for registrations that must all succeed for the plugin to start.
 *
 * <p>Runs an ordered list of named steps. On the first step that throws, the cause is
 * wrapped and rethrown as {@link IllegalStateException} — {@code "Required <name>
 * registration failed"} — and no later step is invoked. The names of steps that were
 * applied successfully are recorded (excluding the failing one) for assertion.
 *
 * <p>Same spirit as {@code VoiceRelayDecision}: a small, side-effect-free-to-construct
 * helper that concentrates a policy (here: first-failure-wins ordering) so it can be
 * exercised deterministically. Per the persistence-slice decision, only interaction and
 * codec registration is routed through this; component/asset registration keeps its
 * existing implicit fail-fast (an uncaught exception aborts {@code SetupManager.start()}).
 */
public final class RequiredRegistration {

    private final List<Step> steps = new ArrayList<>();
    private final List<String> appliedSteps = new ArrayList<>();

    /** Enqueues a named step. Steps run in the order added when {@link #run()} is called. */
    @Nonnull
    public RequiredRegistration step(@Nonnull String name, @Nonnull Runnable action) {
        steps.add(new Step(name, action));
        return this;
    }

    /**
     * Runs each enqueued step in order. The first step to throw stops the sequence: its
     * cause is wrapped in an {@link IllegalStateException} naming the step and rethrown,
     * and no subsequent step executes.
     */
    public void run() {
        for (Step step : steps) {
            try {
                step.action.run();
            } catch (RuntimeException | Error cause) {
                throw new IllegalStateException(
                        "Required " + step.name + " registration failed", cause);
            }
            appliedSteps.add(step.name);
        }
    }

    /** Names of the steps that ran successfully, in order (excludes any failing step). */
    @Nonnull
    public List<String> appliedSteps() {
        return List.copyOf(appliedSteps);
    }

    private record Step(@Nonnull String name, @Nonnull Runnable action) {}
}
