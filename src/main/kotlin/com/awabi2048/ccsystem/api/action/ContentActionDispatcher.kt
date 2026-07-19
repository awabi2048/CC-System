package com.awabi2048.ccsystem.api.action

enum class ContentActionPriority {
    EARLY,
    NORMAL,
    LATE
}

enum class ContentActionPublishResult {
    PUBLISHED,
    DUPLICATE
}

fun interface ContentActionSubscriber {
    fun onAction(action: ContentAction)
}

fun interface ContentActionSubscription : AutoCloseable {
    override fun close()
}

interface ContentActionDispatcher {
    fun publish(action: ContentAction): ContentActionPublishResult

    fun subscribe(
        owner: String,
        priority: ContentActionPriority = ContentActionPriority.NORMAL,
        subscriber: ContentActionSubscriber
    ): ContentActionSubscription

    fun unsubscribeOwner(owner: String)

    fun recentActionCount(): Int
}
