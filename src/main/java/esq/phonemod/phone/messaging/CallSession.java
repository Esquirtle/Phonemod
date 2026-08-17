package esq.phonemod.phone.messaging;

import esq.phonemod.device.assets.CallAudioPolicy;

import javax.annotation.Nonnull;
import java.util.UUID;

/** One atomically reserved two-endpoint call. */
public final class CallSession {
    public enum State { RINGING, ACTIVE, ENDED }
    public enum EndReason { HANGUP, DECLINED, CANCELLED, TIMEOUT, DISCONNECT, SHUTDOWN }

    private final UUID id = UUID.randomUUID();
    private final String caller;
    private final String callee;
    private final String calleeName;
    private final long createdAtMillis;
    private final CallAudioPolicy audioPolicy;
    private State state = State.RINGING;
    private boolean speakerEnabled;
    private EndReason endReason;

    public CallSession(@Nonnull String caller, @Nonnull String callee,
                       @Nonnull String calleeName, long createdAtMillis,
                       @Nonnull CallAudioPolicy audioPolicy) {
        if (caller.equals(callee)) throw new IllegalArgumentException("Call endpoints must differ");
        this.caller = caller;
        this.callee = callee;
        this.calleeName = calleeName;
        this.createdAtMillis = createdAtMillis;
        this.audioPolicy = audioPolicy;
        this.speakerEnabled = audioPolicy == CallAudioPolicy.SPEAKER;
    }

    public synchronized boolean answer(@Nonnull String endpoint) {
        if (state != State.RINGING || !callee.equals(endpoint)) return false;
        state = State.ACTIVE;
        return true;
    }

    public synchronized boolean end(@Nonnull EndReason reason) {
        if (state == State.ENDED) return false;
        state = State.ENDED;
        endReason = reason;
        speakerEnabled = false;
        return true;
    }

    public synchronized boolean setSpeaker(@Nonnull String endpoint, boolean enabled) {
        if (state != State.ACTIVE || !contains(endpoint)
                || audioPolicy != CallAudioPolicy.TOGGLEABLE) return false;
        speakerEnabled = enabled;
        return true;
    }

    public boolean contains(@Nonnull String endpoint) {
        return caller.equals(endpoint) || callee.equals(endpoint);
    }

    public String partnerOf(@Nonnull String endpoint) {
        if (caller.equals(endpoint)) return callee;
        if (callee.equals(endpoint)) return caller;
        return null;
    }

    public UUID id() { return id; }
    public String caller() { return caller; }
    public String callee() { return callee; }
    public String calleeName() { return calleeName; }
    public long createdAtMillis() { return createdAtMillis; }
    public CallAudioPolicy audioPolicy() { return audioPolicy; }
    public synchronized State state() { return state; }
    public synchronized boolean speakerEnabled() { return speakerEnabled; }
    public synchronized EndReason endReason() { return endReason; }
}
