package com.awabi2048.ccsystem.core.action

import com.awabi2048.ccsystem.api.action.ContentAction
import com.awabi2048.ccsystem.api.action.ContentActionDispatcher
import com.awabi2048.ccsystem.api.action.ContentActionPriority
import com.awabi2048.ccsystem.api.action.ContentActionPublishResult
import com.awabi2048.ccsystem.api.action.ContentActionSubscriber
import com.awabi2048.ccsystem.api.action.ContentActionSubscription
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class ContentActionDispatcherImpl @JvmOverloads constructor(
    private val maximumRememberedActions: Int = 10_000,
    private val subscriberFailureHandler: (String, Throwable) -> Unit = { _, _ -> }
) : ContentActionDispatcher {
    private data class RegisteredSubscriber(
        val id: Long,
        val owner: String,
        val priority: ContentActionPriority,
        val subscriber: ContentActionSubscriber
    )

    private val lock = Any()
    private val subscriptionSequence = AtomicLong()
    private val subscribers = mutableListOf<RegisteredSubscriber>()
    private val processedActionIds = object : LinkedHashMap<UUID, Unit>(16, 0.75f, false) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<UUID, Unit>?): Boolean =
            size > maximumRememberedActions
    }

    init {
        require(maximumRememberedActions > 0) { "maximumRememberedActions must be positive" }
    }

    override fun publish(action: ContentAction): ContentActionPublishResult {
        val snapshot = synchronized(lock) {
            if (processedActionIds.putIfAbsent(action.actionId, Unit) != null) {
                return ContentActionPublishResult.DUPLICATE
            }
            subscribers.sortedWith(compareBy<RegisteredSubscriber> { it.priority.ordinal }.thenBy { it.id })
        }

        snapshot.forEach { registration ->
            runCatching { registration.subscriber.onAction(action) }
                .onFailure { subscriberFailureHandler(registration.owner, it) }
        }
        return ContentActionPublishResult.PUBLISHED
    }

    override fun subscribe(
        owner: String,
        priority: ContentActionPriority,
        subscriber: ContentActionSubscriber
    ): ContentActionSubscription {
        require(owner.isNotBlank()) { "owner must not be blank" }
        val id = subscriptionSequence.incrementAndGet()
        synchronized(lock) {
            subscribers += RegisteredSubscriber(id, owner, priority, subscriber)
        }
        return ContentActionSubscription {
            synchronized(lock) { subscribers.removeIf { it.id == id } }
        }
    }

    override fun unsubscribeOwner(owner: String) {
        synchronized(lock) { subscribers.removeIf { it.owner == owner } }
    }

    override fun recentActionCount(): Int = synchronized(lock) { processedActionIds.size }
}
