package esq.phonemod.phone.api;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

import javax.annotation.Nonnull;

/**
 * Base class for apps that use an enum-based state machine.
 *
 * <p>
 * State is stored per-player in {@link PhoneAppContext} — never on the app
 * instance — so a single app object can safely serve all players concurrently.
 *
 * <p>
 * Read the current state with {@link #getState(PhoneAppContext)}, which
 * returns {@code initialState} when no state has been written yet. Write it
 * with {@link #setState(PhoneAppContext, Enum)}.
 *
 * @param <S> state enum type
 */
public abstract class StatefulPhoneApp<S extends Enum<S>> implements PhoneApp<S> {

    protected static final String CONTENT_SELECTOR = "#AppContent";
    private static final String STATE_KEY = "__state__";

    private final S initialState;

    protected StatefulPhoneApp(@Nonnull S initialState) {
        this.initialState = initialState;
    }

    /**
     * Returns the current state for the player identified by {@code ctx}.
     * Falls back to {@code initialState} when no state has been set yet or
     * the stored name is no longer a valid enum constant.
     */
    @SuppressWarnings("unchecked")
    protected S getState(@Nonnull PhoneAppContext ctx) {
        String raw = ctx.getState(STATE_KEY);
        if (raw == null) {
            return initialState;
        }
        try {
            return (S) Enum.valueOf((Class<Enum>) initialState.getClass(), raw);
        } catch (IllegalArgumentException e) {
            return initialState;
        }
    }

    /**
     * Stores {@code state} for the player identified by {@code ctx}.
     */
    protected void setState(@Nonnull PhoneAppContext ctx, @Nonnull S state) {
        ctx.setState(STATE_KEY, state.name());
    }

    /**
     * Clears {@link #CONTENT_SELECTOR} and appends this app's main UI file.
     */
    protected void appendMainUI(@Nonnull UICommandBuilder cmd) {
        cmd.clear(CONTENT_SELECTOR);
        cmd.append(CONTENT_SELECTOR, getUIPath());
    }
}
