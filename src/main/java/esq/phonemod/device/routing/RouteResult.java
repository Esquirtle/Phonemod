package esq.phonemod.device.routing;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Outcome of a {@link RouteHandler}: the decision about what UI flush the router
 * should apply via {@link RouterHost#sendUpdate}.
 *
 * <p>Two shapes mirror exactly what the legacy {@code DevicePage} did:
 * <ul>
 *   <li>{@link #update(UICommandBuilder, UIEventBuilder)} &rarr;
 *       {@code sendUpdate(cmd, evb, false)} — push the built UI.</li>
 *   <li>{@link #empty()} &rarr; {@code sendUpdate(null, null, false)} — the
 *       acknowledgement-only flush used after call actions and unhandled events.</li>
 * </ul>
 */
public final class RouteResult {

    private static final RouteResult EMPTY = new RouteResult(false, null, null);

    private final boolean update;
    @Nullable
    private final UICommandBuilder cmd;
    @Nullable
    private final UIEventBuilder evb;

    private RouteResult(boolean update, @Nullable UICommandBuilder cmd, @Nullable UIEventBuilder evb) {
        this.update = update;
        this.cmd = cmd;
        this.evb = evb;
    }

    /** Push the supplied builders to the client. */
    @Nonnull
    public static RouteResult update(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evb) {
        return new RouteResult(true, cmd, evb);
    }

    /** Acknowledgement-only flush (no UI changes). */
    @Nonnull
    public static RouteResult empty() {
        return EMPTY;
    }

    public boolean isUpdate() {
        return update;
    }

    @Nullable
    public UICommandBuilder cmd() {
        return cmd;
    }

    @Nullable
    public UIEventBuilder evb() {
        return evb;
    }
}
