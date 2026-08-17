package esq.phonemod.phone.messaging;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.packets.stream.StreamType;
import com.hypixel.hytale.protocol.packets.voice.RelayedVoiceData;
import com.hypixel.hytale.protocol.packets.voice.VoiceData;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.modules.voice.VoiceModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.PhoneMod;
import esq.phonemod.device.assets.CallAudioPolicy;
import esq.phonemod.device.events.PhoneCallAnsweredEvent;
import esq.phonemod.device.events.PhoneCallEndedEvent;
import esq.phonemod.device.events.PhoneCallMissedEvent;
import esq.phonemod.device.events.PhoneCallStartedEvent;
import esq.phonemod.device.events.PhoneEvents;
import esq.phonemod.phone.components.CallHistoryComponent;
import esq.phonemod.phone.components.CallRecord;
import esq.phonemod.security.TokenBucket;
import esq.phonemod.operations.OperationalMetrics;
import esq.phonemod.api.event.CallTransitionEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Atomic call state and immutable Netty-thread voice routes. */
public final class CallRegistry {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Object MUTEX = new Object();
    /** Both endpoint keys point at the same session while reserved. */
    private static final Map<String, CallSession> BY_ENDPOINT = new HashMap<>();
    private static final ConcurrentHashMap<String, TokenBucket> ATTEMPT_LIMITERS =
            new ConcurrentHashMap<>();
    private static final AtomicReference<Map<UUID, VoiceRoute>> VOICE_ROUTES =
            new AtomicReference<>(Map.of());
    private static final long TIMEOUT_SCAN_SECONDS = 2L;
    @Nullable private static volatile ScheduledFuture<?> timeoutWatcher;

    private CallRegistry() {}

    public static synchronized void startTimeoutWatcher() {
        if (timeoutWatcher != null) return;
        timeoutWatcher = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                CallRegistry::checkTimeouts,
                TIMEOUT_SCAN_SECONDS, TIMEOUT_SCAN_SECONDS, TimeUnit.SECONDS);
    }

    public static synchronized void stopTimeoutWatcher() {
        ScheduledFuture<?> watcher = timeoutWatcher;
        timeoutWatcher = null;
        if (watcher != null) watcher.cancel(false);
        Set<CallSession> sessions;
        synchronized (MUTEX) {
            sessions = new HashSet<>(BY_ENDPOINT.values());
        }
        for (CallSession session : sessions) {
            endSession(session, CallSession.EndReason.SHUTDOWN, session.caller());
        }
        ATTEMPT_LIMITERS.clear();
        VOICE_ROUTES.set(Map.of());
    }

    private static void checkTimeouts() {
        long now = System.currentTimeMillis();
        Set<CallSession> sessions;
        synchronized (MUTEX) {
            sessions = new HashSet<>(BY_ENDPOINT.values());
        }
        for (CallSession session : sessions) {
            if (session.state() == CallSession.State.RINGING
                    && now - session.createdAtMillis()
                    >= TimeUnit.SECONDS.toMillis(PhoneMod.config().callRingTimeoutSeconds())) {
                endSession(session, CallSession.EndReason.TIMEOUT, session.caller());
            }
        }
    }

    public static final PlayerPacketFilter VOICE_FILTER = (speakerRef, packet) -> {
        if (!(packet instanceof VoiceData voiceData)) return false;
        VoiceRoute route = VOICE_ROUTES.get().get(speakerRef.getUuid());
        if (route == null) return false;

        VoiceModule voice = VoiceModule.get();
        if (!voice.isVoiceEnabled() || voice.isPlayerMuted(speakerRef.getUuid())
                || voiceData.opusData == null || voiceData.opusData.length > voice.getMaxPacketSize()) {
            return true;
        }
        VoiceModule.PositionSnapshot speakerPosition = voice.getCachedPosition(speakerRef.getUuid());
        VoiceModule.PositionSnapshot receiverPosition =
                voice.getCachedPosition(route.receiver().getUuid());

        RelayedVoiceData relay = relay(voiceData, speakerRef, speakerPosition);
        var receiverChannel = route.receiver().getPacketHandler().getChannel(StreamType.Voice);
        if (receiverChannel != null && receiverChannel.isActive()) receiverChannel.writeAndFlush(relay);

        if (route.speakerEnabled() && receiverPosition != null) {
            RelayedVoiceData speakerRelay = relay(voiceData, speakerRef, receiverPosition);
            double speakerRadius = PhoneMod.config().speakerRadiusBlocks();
            double radiusSq = speakerRadius * speakerRadius;
            for (PlayerRef bystander : Universe.get().getPlayers()) {
                if (bystander.getUuid().equals(speakerRef.getUuid())
                        || bystander.getUuid().equals(route.receiver().getUuid())) continue;
                VoiceModule.PositionSnapshot position = voice.getCachedPosition(bystander.getUuid());
                if (position == null || position.worldId() != receiverPosition.worldId()) continue;
                double dx = position.position().x() - receiverPosition.position().x();
                double dy = position.position().y() - receiverPosition.position().y();
                double dz = position.position().z() - receiverPosition.position().z();
                if (dx * dx + dy * dy + dz * dz > radiusSq) continue;
                var channel = bystander.getPacketHandler().getChannel(StreamType.Voice);
                if (channel != null && channel.isActive()) channel.writeAndFlush(speakerRelay);
            }
        }
        return true;
    };

    private static RelayedVoiceData relay(VoiceData source, PlayerRef speaker,
                                          @Nullable VoiceModule.PositionSnapshot position) {
        RelayedVoiceData relay = new RelayedVoiceData();
        relay.speakerId = speaker.getUuid();
        relay.sequenceNumber = source.sequenceNumber;
        relay.timestamp = source.timestamp;
        relay.opusData = source.opusData;
        if (position != null) {
            relay.entityId = position.networkId();
            relay.speakerPosition = new Position(position.position().x(), position.position().y(),
                    position.position().z());
            relay.speakerIsUnderwater = position.isUnderwater();
        }
        return relay;
    }

    public static boolean initiateCall(@Nonnull String caller, @Nonnull String callee,
                                       @Nonnull String calleeName) {
        if (!PhoneMod.config().callsEnabled()) return false;
        PhoneRegistry.PresenceEntry callerEntry = PhoneRegistry.getPresenceEntry(caller);
        PhoneRegistry.PresenceEntry calleeEntry = PhoneRegistry.getPresenceEntry(callee);
        if (caller.equals(callee) || callerEntry == null || calleeEntry == null
                || callerEntry.playerRef().getUuid().equals(calleeEntry.playerRef().getUuid())) {
            recordFailedCall(callerEntry, caller, callee, calleeName);
            return false;
        }
        if (PhoneMod.config().requireOpenDeviceForIncomingCalls()
                && PhoneRegistry.getOnlineEntry(callee) == null) {
            recordFailedCall(callerEntry, caller, callee, calleeName);
            return false;
        }
        if (!PhoneRegistry.getPreferences(callee).incomingCallsEnabled()) {
            recordFailedCall(callerEntry, caller, callee, calleeName);
            return false;
        }
        if (!PhoneMod.config().allowCrossWorldCalls()
                && callerEntry.world() != calleeEntry.world()) {
            recordFailedCall(callerEntry, caller, callee, calleeName);
            return false;
        }
        TokenBucket limiter = ATTEMPT_LIMITERS.computeIfAbsent(caller, ignored ->
                new TokenBucket(1.0 / PhoneMod.config().callAttemptIntervalSeconds(), 1));
        if (!limiter.tryAcquire()) {
            OperationalMetrics.get().rateLimited();
            return false;
        }

        CallTransitionEvent.Pre pre = HytaleServer.get().getEventBus()
                .dispatchFor(CallTransitionEvent.Pre.class)
                .dispatch(new CallTransitionEvent.Pre(caller, callee,
                        CallTransitionEvent.Transition.RINGING));
        if (pre.isCancelled()) return false;

        CallAudioPolicy policy = resolvePolicy(caller);
        CallSession session = new CallSession(caller, callee, calleeName,
                System.currentTimeMillis(), policy);
        synchronized (MUTEX) {
            if (BY_ENDPOINT.containsKey(caller) || BY_ENDPOINT.containsKey(callee)) return false;
            BY_ENDPOINT.put(caller, session);
            BY_ENDPOINT.put(callee, session);
        }
        PhoneEvents.post(new PhoneCallStartedEvent(caller, callee));
        HytaleServer.get().getEventBus().dispatchFor(CallTransitionEvent.Post.class)
                .dispatch(new CallTransitionEvent.Post(caller, callee,
                        CallTransitionEvent.Transition.RINGING));
        notifyPage(callee, page -> page.onIncomingCall(caller, caller));
        notifyPage(caller, page -> page.onOutgoingCall(callee, calleeName));
        return true;
    }

    private static CallAudioPolicy resolvePolicy(String endpoint) {
        CallAudioPolicy assetPolicy = CallAudioPolicy.PRIVATE;
        PhoneRegistry.OnlineEntry open = PhoneRegistry.getOnlineEntry(endpoint);
        if (open != null && open.page() instanceof esq.phonemod.device.ui.DevicePage page) {
            assetPolicy = page.getSession().getShell().getCallAudioPolicy();
        }
        return PhoneMod.config().callAudioMode().resolve(assetPolicy);
    }

    public static boolean answerCall(@Nonnull String callee) {
        CallSession session;
        synchronized (MUTEX) {
            session = BY_ENDPOINT.get(callee);
            if (session == null || !session.answer(callee)) return false;
            rebuildVoiceRoutesLocked();
        }
        PhoneEvents.post(new PhoneCallAnsweredEvent(session.caller(), session.callee()));
        PhoneEvents.post(new PhoneCallAnsweredEvent(session.callee(), session.caller()));
        HytaleServer.get().getEventBus().dispatchFor(CallTransitionEvent.Post.class)
                .dispatch(new CallTransitionEvent.Post(session.caller(), session.callee(),
                        CallTransitionEvent.Transition.ANSWERED));
        notifyPage(session.caller(), page -> page.onCallAnswered(session.callee(), session.callee()));
        notifyPage(session.callee(), page -> page.onCallAnswered(session.caller(), session.caller()));
        return true;
    }

    public static boolean setSpeaker(@Nonnull String endpoint, boolean enabled) {
        CallSession session;
        synchronized (MUTEX) {
            session = BY_ENDPOINT.get(endpoint);
            if (session == null || !session.setSpeaker(endpoint, enabled)) return false;
            rebuildVoiceRoutesLocked();
        }
        notifyPage(session.caller(), page -> page.onCallAnswered(
                session.callee(), session.callee()));
        notifyPage(session.callee(), page -> page.onCallAnswered(
                session.caller(), session.caller()));
        return true;
    }

    public static void hangUp(@Nonnull String endpoint) {
        CallSession session;
        synchronized (MUTEX) {
            session = BY_ENDPOINT.get(endpoint);
        }
        if (session == null) return;
        CallSession.EndReason reason = session.state() == CallSession.State.ACTIVE
                ? CallSession.EndReason.HANGUP
                : (session.callee().equals(endpoint)
                    ? CallSession.EndReason.DECLINED : CallSession.EndReason.CANCELLED);
        endSession(session, reason, endpoint);
    }

    private static void endSession(CallSession session, CallSession.EndReason reason, String actor) {
        CallSession.State prior;
        synchronized (MUTEX) {
            if (BY_ENDPOINT.get(session.caller()) != session
                    || BY_ENDPOINT.get(session.callee()) != session) return;
            prior = session.state();
            if (!session.end(reason)) return;
            BY_ENDPOINT.remove(session.caller(), session);
            BY_ENDPOINT.remove(session.callee(), session);
            rebuildVoiceRoutesLocked();
        }
        long timestamp = System.currentTimeMillis();
        boolean missed = prior == CallSession.State.RINGING;
        persistAndClose(session.caller(), session.callee(), session.calleeName(), timestamp, true, missed);
        persistAndClose(session.callee(), session.caller(), session.caller(), timestamp, false, missed);
        if (missed) PhoneEvents.post(new PhoneCallMissedEvent(session.callee(), session.caller()));
        else {
            PhoneEvents.post(new PhoneCallEndedEvent(session.caller(), session.callee()));
            PhoneEvents.post(new PhoneCallEndedEvent(session.callee(), session.caller()));
        }
        HytaleServer.get().getEventBus().dispatchFor(CallTransitionEvent.Post.class)
                .dispatch(new CallTransitionEvent.Post(session.caller(), session.callee(),
                        missed ? CallTransitionEvent.Transition.MISSED
                                : CallTransitionEvent.Transition.ENDED));
    }

    private static void recordFailedCall(@Nullable PhoneRegistry.PresenceEntry entry,
                                         String caller, String callee, String calleeName) {
        if (entry != null) {
            entry.world().execute(() -> persistRecord(entry.store(), entry.ref(), caller,
                    new CallRecord(callee, calleeName, System.currentTimeMillis(), true, true)));
        }
        PhoneEvents.post(new PhoneCallMissedEvent(callee, caller));
        notifyPage(caller, esq.phonemod.device.api.DevicePageHandle::onCallEnded);
    }

    private static void persistAndClose(String own, String partner, String partnerName,
                                        long timestamp, boolean outgoing, boolean missed) {
        PhoneRegistry.PresenceEntry entry = PhoneRegistry.getPresenceEntry(own);
        if (entry != null) {
            entry.world().execute(() -> persistRecord(entry.store(), entry.ref(), own,
                    new CallRecord(partner, partnerName, timestamp, outgoing, missed)));
        }
        notifyPage(own, esq.phonemod.device.api.DevicePageHandle::onCallEnded);
    }

    private static void notifyPage(String endpoint,
            java.util.function.Consumer<esq.phonemod.device.api.DevicePageHandle> notification) {
        PhoneRegistry.OnlineEntry entry = PhoneRegistry.getOnlineEntry(endpoint);
        if (entry != null) entry.world().execute(() -> notification.accept(entry.page()));
    }

    private static void rebuildVoiceRoutesLocked() {
        Map<UUID, VoiceRoute> routes = new HashMap<>();
        Set<CallSession> sessions = new HashSet<>(BY_ENDPOINT.values());
        for (CallSession session : sessions) {
            if (session.state() != CallSession.State.ACTIVE) continue;
            PhoneRegistry.PresenceEntry caller = PhoneRegistry.getPresenceEntry(session.caller());
            PhoneRegistry.PresenceEntry callee = PhoneRegistry.getPresenceEntry(session.callee());
            if (caller == null || callee == null) continue;
            routes.put(caller.playerRef().getUuid(),
                    new VoiceRoute(callee.playerRef(), session.speakerEnabled()));
            routes.put(callee.playerRef().getUuid(),
                    new VoiceRoute(caller.playerRef(), session.speakerEnabled()));
        }
        VOICE_ROUTES.set(Map.copyOf(routes));
    }

    public static void cleanupOnDisconnect(@Nonnull String endpoint) {
        CallSession session;
        synchronized (MUTEX) { session = BY_ENDPOINT.get(endpoint); }
        if (session != null) endSession(session, CallSession.EndReason.DISCONNECT, endpoint);
    }

    public static boolean isInCall(@Nonnull String endpoint) {
        synchronized (MUTEX) {
            CallSession session = BY_ENDPOINT.get(endpoint);
            return session != null && session.state() == CallSession.State.ACTIVE;
        }
    }

    @Nullable public static CallAudioPolicy getAudioPolicy(@Nonnull String endpoint) {
        synchronized (MUTEX) {
            CallSession session = BY_ENDPOINT.get(endpoint);
            return session != null ? session.audioPolicy() : null;
        }
    }

    public static boolean isSpeakerEnabled(@Nonnull String endpoint) {
        synchronized (MUTEX) {
            CallSession session = BY_ENDPOINT.get(endpoint);
            return session != null && session.speakerEnabled();
        }
    }

    public static int activeSessionCount() {
        synchronized (MUTEX) { return new HashSet<>(BY_ENDPOINT.values()).size(); }
    }

    @Nullable public static String getActivePartner(@Nonnull String endpoint) {
        synchronized (MUTEX) {
            CallSession session = BY_ENDPOINT.get(endpoint);
            return session != null && session.state() == CallSession.State.ACTIVE
                    ? session.partnerOf(endpoint) : null;
        }
    }

    @Nullable public static String getPendingCallee(@Nonnull String endpoint) {
        synchronized (MUTEX) {
            CallSession session = BY_ENDPOINT.get(endpoint);
            return session != null && session.state() == CallSession.State.RINGING
                    && session.caller().equals(endpoint) ? session.callee() : null;
        }
    }

    @Nullable public static String getPendingCaller(@Nonnull String endpoint) {
        synchronized (MUTEX) {
            CallSession session = BY_ENDPOINT.get(endpoint);
            return session != null && session.state() == CallSession.State.RINGING
                    && session.callee().equals(endpoint) ? session.caller() : null;
        }
    }

    private static void persistRecord(Store<EntityStore> store, Ref<EntityStore> ref,
                                      String own, CallRecord record) {
        CallHistoryComponent component = store.ensureAndGetComponent(
                ref, CallHistoryComponent.getComponentType());
        component.addRecord(own, record);
        store.putComponent(ref, CallHistoryComponent.getComponentType(), component);
    }

    private record VoiceRoute(@Nonnull PlayerRef receiver, boolean speakerEnabled) {}
}
