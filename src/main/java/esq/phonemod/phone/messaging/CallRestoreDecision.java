package esq.phonemod.phone.messaging;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Pure, side-effect-free resolution of which call state a reopened device page should
 * restore. Mirrors the branch ladder previously inlined in
 * {@code DevicePage.buildContent} (precedence: active &gt; outgoing-pending &gt;
 * incoming-pending &gt; none). Safe to call from any thread.
 */
public final class CallRestoreDecision {
    public enum Kind { ACTIVE, OUTGOING, INCOMING, NONE }

    /** {@code value} is the partner endpoint for ACTIVE/OUTGOING/INCOMING, null for NONE. */
    public record Restore(@Nonnull Kind kind, @Nullable String value) {}

    private static final Restore NONE = new Restore(Kind.NONE, null);

    private CallRestoreDecision() {}

    @Nonnull
    public static Restore resolve(@Nullable String activePartner,
                                  @Nullable String pendingCallee,
                                  @Nullable String pendingCaller) {
        if (activePartner != null) return new Restore(Kind.ACTIVE, activePartner);
        if (pendingCallee != null) return new Restore(Kind.OUTGOING, pendingCallee);
        if (pendingCaller != null) return new Restore(Kind.INCOMING, pendingCaller);
        return NONE;
    }
}
