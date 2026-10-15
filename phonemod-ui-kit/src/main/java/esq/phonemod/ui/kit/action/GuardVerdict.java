package esq.phonemod.ui.kit.action;

import javax.annotation.Nonnull;

/** Shape mirrors {@code UiEventValidator.ValidationResult} ({@code :84}). */
public record GuardVerdict(boolean allowed, @Nonnull String reason) {

    public static final GuardVerdict ALLOWED = new GuardVerdict(true, "allowed");

    // DEVIATION (same reasoning as DeviceType): this record owns a self-typed
    // static constant (ALLOWED), so its compact constructor is a plain, real
    // pass-through rather than an F4 throw — throwing here would fail this
    // class's <clinit> on first touch and turn every later reference into
    // NoClassDefFoundError, which the task's hard gate forbids. There is no
    // real validation logic to stub out here anyway; it is a pure data carrier.
    public GuardVerdict {
    }

    @Nonnull
    public static GuardVerdict reject(@Nonnull String reason) {
        return new GuardVerdict(false, reason);
    }
}
