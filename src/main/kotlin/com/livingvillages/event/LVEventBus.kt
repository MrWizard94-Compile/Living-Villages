package com.livingvillages.event

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Lightweight internal event bus for cross-module communication.
 *
 * Modules subscribe to specific event types they care about.
 * This keeps modules decoupled — no direct dependencies between gameplay modules.
 */
object LVEventBus {
    private val logger = LoggerFactory.getLogger("LivingVillages/EventBus")
    private val listeners = ConcurrentHashMap<KClass<out LVEvent>, MutableList<EventListener<*>>>()

    private data class EventListener<T : LVEvent>(
        val owner: String,
        val handler: (T) -> Unit,
    )

    /**
     * Subscribe to an event type. [owner] is used for logging/debugging.
     */
    inline fun <reified T : LVEvent> subscribe(owner: String, noinline handler: (T) -> Unit) {
        subscribe(T::class, owner, handler)
    }

    fun <T : LVEvent> subscribe(eventClass: KClass<T>, owner: String, handler: (T) -> Unit) {
        listeners.getOrPut(eventClass) { mutableListOf() }
            .add(EventListener(owner, handler))
        logger.debug("Module '{}' subscribed to {}", owner, eventClass.simpleName)
    }

    /**
     * Post an event to all registered listeners.
     */
    fun post(event: LVEvent) {
        val eventClass = event::class
        val handlers = listeners[eventClass] ?: return

        logger.trace("Posting {} to {} listeners", eventClass.simpleName, handlers.size)
        for (listener in handlers) {
            try {
                @Suppress("UNCHECKED_CAST")
                (listener as EventListener<LVEvent>).handler(event)
            } catch (e: Exception) {
                logger.error(
                    "Error in event handler '{}' for {}: {}",
                    listener.owner, eventClass.simpleName, e.message, e
                )
            }
        }
    }

    /**
     * Remove all listeners registered by a given owner. Useful for cleanup/testing.
     */
    fun unsubscribeAll(owner: String) {
        listeners.values.forEach { list ->
            list.removeAll { it.owner == owner }
        }
        logger.debug("Unsubscribed all listeners for '{}'", owner)
    }

    /**
     * Clear all listeners. Used in testing.
     */
    fun clear() {
        listeners.clear()
        logger.debug("Event bus cleared")
    }
}
