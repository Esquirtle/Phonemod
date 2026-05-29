package esq.phonemod.phone.messaging;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.packets.stream.StreamType;
import com.hypixel.hytale.protocol.packets.voice.RelayedVoiceData;
import com.hypixel.hytale.protocol.packets.voice.VoiceData;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.modules.voice.VoiceModule;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.phone.components.CallHistoryComponent;
import esq.phonemod.phone.components.CallRecord;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages phone call state and performs private voice routing between call parties.
 *
 * <h3>Call flow</h3>
 * <ol>
 *   <li>{@link #initiateCall} — caller presses Call; if callee is online their UI shows incoming call.</li>
 *   <li>{@link #answerCall} — callee taps Answer; both UIs switch to active-call view.</li>
 *   <li>{@link #hangUp} — either party ends the call; history is persisted on both sides.</li>
 * </ol>
 *
 * <h3>Voice routing</h3>
 * {@link #VOICE_FILTER} is registered globally via {@code PacketAdapters.registerInbound}.
 * When a player speaking is in an active call, their {@link VoiceData} packet is intercepted,
 * relayed as {@link RelayedVoiceData} directly to the call partner's voice channel, and the
 * original packet is cancelled so normal proximity routing does NOT fire.
 */
public final class CallRegistry {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** phoneNumber → phoneNumber (both directions when a call is active). */
    private static final ConcurrentHashMap<String, String> activeCalls = new ConcurrentHashMap<>();

    /** callerNumber → calleeNumber during the ringing phase. */
    private static final ConcurrentHashMap<String, String> pendingCalls = new ConcurrentHashMap<>();

    /** Radius (in blocks) within which bystanders near the call recipient can hear the phone speaker. */
    private static final double PHONE_SPEAKER_RADIUS = 8.0;

    private CallRegistry() {}

    // ── Voice intercept ───────────────────────────────────────────────────────

    /**
     * Global {@link PlayerPacketFilter} that intercepts {@link VoiceData} packets.
     * When the speaking player is in an active phone call:
     * <ol>
     *   <li>Audio is relayed privately to the call partner (speakerPosition = caller's real
     *       position, so the partner hears the caller directionally).</li>
     *   <li>Audio is also broadcast spatially to any player within {@link #PHONE_SPEAKER_RADIUS}
     *       blocks of the <em>recipient</em>, with speakerPosition set to the recipient's
     *       position — simulating the phone playing out loud.</li>
     * </ol>
     * The original packet is cancelled so the caller's proximity voice is suppressed.
     */
    public static final PlayerPacketFilter VOICE_FILTER = (playerRef, packet) -> {
        if (!(packet instanceof VoiceData voiceData)) return false;

        // Find which of this player's phone numbers is in an active call
        String callerNumber = null;
        for (String number : PhoneRegistry.getPhoneNumbersByUuid(playerRef.getUuid())) {
            if (activeCalls.containsKey(number)) {
                callerNumber = number;
                break;
            }
        }
        if (callerNumber == null) return false;

        String partnerNumber = activeCalls.get(callerNumber);
        if (partnerNumber == null) return false;

        PhoneRegistry.OnlineEntry partnerEntry = PhoneRegistry.getOnlineEntry(partnerNumber);
        if (partnerEntry == null) return true; // partner offline — block but don't route

        VoiceModule voiceMod = VoiceModule.get();
        if (!voiceMod.isVoiceEnabled()) return true;
        if (voiceMod.isPlayerMuted(playerRef.getUuid())) return true;
        if (voiceData.opusData == null || voiceData.opusData.length > voiceMod.getMaxPacketSize()) return true;

        VoiceModule.PositionSnapshot callerPos  = voiceMod.getCachedPosition(playerRef.getUuid());
        VoiceModule.PositionSnapshot partnerPos = voiceMod.getCachedPosition(partnerEntry.playerRef().getUuid());

        // ── 1. Private relay to call partner ──────────────────────────────────
        // speakerPosition = caller's real position → partner hears caller directionally.
        RelayedVoiceData privateRelay = new RelayedVoiceData();
        privateRelay.speakerId     = playerRef.getUuid();
        privateRelay.sequenceNumber = voiceData.sequenceNumber;
        privateRelay.timestamp     = voiceData.timestamp;
        privateRelay.opusData      = voiceData.opusData;
        if (callerPos != null) {
            privateRelay.entityId            = callerPos.networkId();
            privateRelay.speakerPosition     = new Position(callerPos.x(), callerPos.y(), callerPos.z());
            privateRelay.speakerIsUnderwater = callerPos.isUnderwater();
        }
        var partnerVoiceChannel = partnerEntry.playerRef().getPacketHandler().getChannel(StreamType.Voice);
        if (partnerVoiceChannel != null && partnerVoiceChannel.isActive()) {
            partnerVoiceChannel.writeAndFlush(privateRelay);
        }

        // ── 2. Spatial bystander relay (phone speaker simulation) ─────────────
        // speakerPosition = recipient's position → bystanders near the recipient
        // hear the audio as if it's emitting from the phone in their hand.
        if (partnerPos != null) {
            RelayedVoiceData speakerRelay = new RelayedVoiceData();
            speakerRelay.speakerId       = playerRef.getUuid();
            speakerRelay.sequenceNumber  = voiceData.sequenceNumber;
            speakerRelay.timestamp       = voiceData.timestamp;
            speakerRelay.opusData        = voiceData.opusData;
            speakerRelay.entityId        = partnerPos.networkId();
            speakerRelay.speakerPosition = new Position(partnerPos.x(), partnerPos.y(), partnerPos.z());
            speakerRelay.speakerIsUnderwater = partnerPos.isUnderwater();

            double radiusSq = PHONE_SPEAKER_RADIUS * PHONE_SPEAKER_RADIUS;
            for (var bystander : Universe.get().getPlayers()) {
                // Skip the two call participants — partner already received privateRelay
                if (bystander.getUuid().equals(playerRef.getUuid())) continue;
                if (bystander.getUuid().equals(partnerEntry.playerRef().getUuid())) continue;

                VoiceModule.PositionSnapshot bystanderPos = voiceMod.getCachedPosition(bystander.getUuid());
                if (bystanderPos == null) continue;
                if (bystanderPos.worldId() != partnerPos.worldId()) continue;

                double dx = bystanderPos.x() - partnerPos.x();
                double dy = bystanderPos.y() - partnerPos.y();
                double dz = bystanderPos.z() - partnerPos.z();
                if (dx * dx + dy * dy + dz * dz > radiusSq) continue;

                var bystanderChannel = bystander.getPacketHandler().getChannel(StreamType.Voice);
                if (bystanderChannel != null && bystanderChannel.isActive()) {
                    bystanderChannel.writeAndFlush(speakerRelay);
                }
            }
        }

        return true; // cancel normal proximity routing for the caller
    };

    // ── Call management ───────────────────────────────────────────────────────

    /**
     * Starts a call from {@code callerNumber} to {@code calleeNumber}.
     * If the callee has an open phone, their incoming-call UI is shown.
     * If the callee is offline, a missed-call record is immediately written to the caller.
     */
    public static void initiateCall(@Nonnull String callerNumber,
                                     @Nonnull String calleeNumber,
                                     @Nonnull String calleeName) {
        if (activeCalls.containsKey(callerNumber) || pendingCalls.containsKey(callerNumber)) {
            LOGGER.atWarning().log("[CallRegistry] %s tried to call while already in/pending a call", callerNumber);
            return;
        }

        PhoneRegistry.OnlineEntry callerEntry = PhoneRegistry.getOnlineEntry(callerNumber);
        PhoneRegistry.OnlineEntry calleeEntry = PhoneRegistry.getOnlineEntry(calleeNumber);

        if (calleeEntry == null) {
            // Callee offline — log missed on callee side when possible, failed on caller side
            LOGGER.atInfo().log("[CallRegistry] Callee %s offline; call from %s failed", calleeNumber, callerNumber);
            if (callerEntry != null) {
                callerEntry.world().execute(() -> {
                    persistRecord(callerEntry.store(), callerEntry.ref(), callerNumber,
                            new CallRecord(calleeNumber, calleeName, System.currentTimeMillis(), true, true));
                    callerEntry.page().onCallEnded();
                });
            }
            return;
        }

        pendingCalls.put(callerNumber, calleeNumber);
        LOGGER.atInfo().log("[CallRegistry] Call pending: %s → %s", callerNumber, calleeNumber);

        calleeEntry.world().execute(() ->
                calleeEntry.page().onIncomingCall(callerNumber, callerNumber));
    }

    /**
     * Called when the callee taps Answer.
     * Finds who is calling {@code calleeNumber}, promotes to active, and notifies both pages.
     */
    public static void answerCall(@Nonnull String calleeNumber) {
        String callerNumber = getCallerFor(calleeNumber);
        if (callerNumber == null) {
            LOGGER.atWarning().log("[CallRegistry] answerCall: no pending caller for %s", calleeNumber);
            return;
        }
        pendingCalls.remove(callerNumber);
        activeCalls.put(callerNumber, calleeNumber);
        activeCalls.put(calleeNumber, callerNumber);
        LOGGER.atInfo().log("[CallRegistry] Call connected: %s ↔ %s", callerNumber, calleeNumber);

        PhoneRegistry.OnlineEntry callerEntry = PhoneRegistry.getOnlineEntry(callerNumber);
        PhoneRegistry.OnlineEntry calleeEntry = PhoneRegistry.getOnlineEntry(calleeNumber);
        if (callerEntry != null) {
            callerEntry.world().execute(() -> callerEntry.page().onCallAnswered(calleeNumber, calleeNumber));
        }
        if (calleeEntry != null) {
            calleeEntry.world().execute(() -> calleeEntry.page().onCallAnswered(callerNumber, callerNumber));
        }
    }

    /**
     * Ends any active or pending call involving {@code number}. Persists call records and
     * notifies the other party. Safe to call from any thread.
     */
    public static void hangUp(@Nonnull String number) {
        // End active call
        String partnerNumber = activeCalls.remove(number);
        if (partnerNumber != null) {
            activeCalls.remove(partnerNumber);
            LOGGER.atInfo().log("[CallRegistry] Call ended: %s ↔ %s", number, partnerNumber);
            long ts = System.currentTimeMillis();

            PhoneRegistry.OnlineEntry myEntry      = PhoneRegistry.getOnlineEntry(number);
            PhoneRegistry.OnlineEntry partnerEntry = PhoneRegistry.getOnlineEntry(partnerNumber);

            if (myEntry != null) {
                final String pNum = partnerNumber;
                myEntry.world().execute(() -> {
                    persistRecord(myEntry.store(), myEntry.ref(), number,
                            new CallRecord(pNum, pNum, ts, true, false));
                    myEntry.page().onCallEnded();
                });
            }
            if (partnerEntry != null) {
                final String n = number;
                partnerEntry.world().execute(() -> {
                    persistRecord(partnerEntry.store(), partnerEntry.ref(), partnerNumber,
                            new CallRecord(n, n, ts, false, false));
                    partnerEntry.page().onCallEnded();
                });
            }
            return;
        }

        // Decline pending call (callee declining)
        String callerNumber = getCallerFor(number);
        if (callerNumber != null) {
            pendingCalls.remove(callerNumber);
            LOGGER.atInfo().log("[CallRegistry] Call declined (%s → %s)", callerNumber, number);
            long ts = System.currentTimeMillis();

            PhoneRegistry.OnlineEntry callerEntry = PhoneRegistry.getOnlineEntry(callerNumber);
            PhoneRegistry.OnlineEntry calleeEntry = PhoneRegistry.getOnlineEntry(number);
            if (callerEntry != null) {
                final String n = number;
                callerEntry.world().execute(() -> {
                    persistRecord(callerEntry.store(), callerEntry.ref(), callerNumber,
                            new CallRecord(n, n, ts, true, true));
                    callerEntry.page().onCallEnded();
                });
            }
            if (calleeEntry != null) {
                calleeEntry.world().execute(() -> calleeEntry.page().onCallEnded());
            }
            return;
        }

        // Cancel pending call (caller hanging up before answer)
        String calleeNumber = pendingCalls.remove(number);
        if (calleeNumber != null) {
            LOGGER.atInfo().log("[CallRegistry] Call cancelled by caller (%s → %s)", number, calleeNumber);
            PhoneRegistry.OnlineEntry calleeEntry = PhoneRegistry.getOnlineEntry(calleeNumber);
            if (calleeEntry != null) {
                calleeEntry.world().execute(() -> calleeEntry.page().onCallEnded());
            }
        }
    }

    /** Cleans up any calls for a disconnecting player (by phone number). */
    public static void cleanupOnDisconnect(@Nonnull String phoneNumber) {
        if (activeCalls.containsKey(phoneNumber) || pendingCalls.containsKey(phoneNumber)
                || getCallerFor(phoneNumber) != null) {
            hangUp(phoneNumber);
        }
    }

    public static boolean isInCall(@Nonnull String phoneNumber) {
        return activeCalls.containsKey(phoneNumber);
    }

    /** Returns the active call partner's number, or {@code null} if not in an active call. */
    @Nullable
    public static String getActivePartner(@Nonnull String phoneNumber) {
        return activeCalls.get(phoneNumber);
    }

    /** Returns the callee number if this player is the pending caller (ringing), or {@code null}. */
    @Nullable
    public static String getPendingCallee(@Nonnull String phoneNumber) {
        return pendingCalls.get(phoneNumber);
    }

    /** Returns the caller number if this player is the pending callee (incoming), or {@code null}. */
    @Nullable
    public static String getPendingCaller(@Nonnull String phoneNumber) {
        return getCallerFor(phoneNumber);
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private static void persistRecord(@Nonnull Store<EntityStore> store,
                                       @Nonnull Ref<EntityStore> ref,
                                       @Nonnull String ownNumber,
                                       @Nonnull CallRecord record) {
        CallHistoryComponent component =
                store.ensureAndGetComponent(ref, CallHistoryComponent.getComponentType());
        component.addRecord(ownNumber, record);
        store.putComponent(ref, CallHistoryComponent.getComponentType(), component);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @Nullable
    private static String getCallerFor(@Nonnull String calleeNumber) {
        for (Map.Entry<String, String> entry : pendingCalls.entrySet()) {
            if (entry.getValue().equals(calleeNumber)) return entry.getKey();
        }
        return null;
    }
}
