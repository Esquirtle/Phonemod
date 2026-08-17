package esq.phonemod.device.events;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Lightweight framework event bus for device and phone lifecycle events.
 *
 * <p>Usage:
 * <pre>{@code
 * // Subscribe (typically in plugin setup):
 * PhoneEvents.subscribe(DeviceAppOpenEvent.class, event -> {
 *     // react to app open
 * });
 *
 * // Post from framework code or third-party plugins:
 * PhoneEvents.post(new DeviceAppOpenEvent(session, appId));
 * }</pre>
 *
 * <p>Threading: {@link #subscribe} and {@link #post} are safe to call from
 * any thread. Listeners are invoked on whatever thread {@link #post} is called
 * from. Listeners that mutate game state must dispatch back to the correct
 * world thread themselves.
 */
@Deprecated(since = "2.0", forRemoval = false)
public final class PhoneEvents {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @SuppressWarnings("rawtypes")
    private static final Map<Class, List<Consumer>> LISTENERS = new ConcurrentHashMap<>();

    private PhoneEvents() {
    }

    /**
     * Subscribes {@code listener} to events of type {@code eventType}.
     *
     * @param eventType the concrete event class to listen for
     * @param listener  the callback to invoke when an event of that type is posted
     */
    @SuppressWarnings("unchecked")
    public static <T extends DeviceEvent> void subscribe(
            @Nonnull Class<T> eventType,
            @Nonnull Consumer<T> listener) {
        LISTENERS.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(listener);
    }

    /**
     * Removes one listener previously registered for {@code eventType}.
     *
     * @return {@code true} if the listener was registered and removed
     */
    public static <T extends DeviceEvent> boolean unsubscribe(
            @Nonnull Class<T> eventType,
            @Nonnull Consumer<T> listener) {
        List<Consumer> listeners = LISTENERS.get(eventType);
        if (listeners == null) {
            return false;
        }
        boolean removed = listeners.remove(listener);
        if (listeners.isEmpty()) {
            LISTENERS.remove(eventType, listeners);
        }
        return removed;
    }

    /**
     * Removes all subscriptions for {@code eventType}.
     * Framework owners may use this during full shutdown. Integrations should
     * prefer {@link #unsubscribe} so they do not remove unrelated listeners.
     */
    public static void unsubscribeAll(@Nonnull Class<? extends DeviceEvent> eventType) {
        LISTENERS.remove(eventType);
    }

    /**
     * Posts {@code event} to all subscribed listeners for its runtime type.
     * Listeners are invoked synchronously on the calling thread.
     *
     * @param event the event to dispatch
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends DeviceEvent> void post(@Nonnull T event) {
        List<Consumer> listeners = LISTENERS.get(event.getClass());
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        for (Consumer listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log(
                        "[PhoneEvents] Listener threw on event %s", event.getClass().getSimpleName());
            }
        }
    }
}
