package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.action.ContentAction;
import com.awabi2048.ccsystem.api.action.ContentActionPriority;
import com.awabi2048.ccsystem.api.action.ContentActionPublishResult;
import com.awabi2048.ccsystem.api.action.ContentActionType;
import com.awabi2048.ccsystem.core.action.ContentActionDispatcherImpl;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContentActionDispatcherTest {
    @Test
    void ordersSubscribersAndSuppressesDuplicateActionIds() {
        var dispatcher = new ContentActionDispatcherImpl();
        var calls = new ArrayList<String>();
        dispatcher.subscribe("late", ContentActionPriority.LATE, action -> calls.add("late"));
        dispatcher.subscribe("early", ContentActionPriority.EARLY, action -> calls.add("early"));
        var action = action(UUID.randomUUID());

        assertEquals(ContentActionPublishResult.PUBLISHED, dispatcher.publish(action));
        assertEquals(ContentActionPublishResult.DUPLICATE, dispatcher.publish(action));
        assertEquals(java.util.List.of("early", "late"), calls);
        assertEquals(1, dispatcher.recentActionCount());
    }

    @Test
    void isolatesSubscriberFailureAndSupportsOwnerRemoval() {
        var failures = new ArrayList<String>();
        var calls = new ArrayList<String>();
        var dispatcher = new ContentActionDispatcherImpl(10, (owner, failure) -> {
            failures.add(owner);
            return kotlin.Unit.INSTANCE;
        });
        dispatcher.subscribe("broken", ContentActionPriority.EARLY, action -> { throw new IllegalStateException("boom"); });
        dispatcher.subscribe("healthy", ContentActionPriority.NORMAL, action -> calls.add("healthy"));

        dispatcher.publish(action(UUID.randomUUID()));
        dispatcher.unsubscribeOwner("healthy");
        dispatcher.publish(action(UUID.randomUUID()));

        assertEquals(java.util.List.of("broken", "broken"), failures);
        assertEquals(java.util.List.of("healthy"), calls);
    }

    private static ContentAction action(UUID id) {
        return new ContentAction(
            id,
            1,
            Instant.parse("2026-07-19T00:00:00Z"),
            UUID.randomUUID(),
            ContentActionType.FISH_CAUGHT,
            1,
            null,
            Map.of()
        );
    }
}
