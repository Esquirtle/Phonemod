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
     * Removes all subscriptions for {@code eventType}.
     * Useful when a plugin shuts down and wants to avoid leaking listeners.
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
