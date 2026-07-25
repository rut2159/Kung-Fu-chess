package com.chessgame.bus;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleEventBusTest {

    /** Minimal test-only event, kept private to this test - not part of the real event catalog. */
    private record TestEvent(String payload) implements GameEvent {
    }

    private record OtherTestEvent(String payload) implements GameEvent {
    }

    @Test
    void subscriberReceivesPublishedEvent() {
        EventBus bus = new SimpleEventBus();
        List<String> received = new ArrayList<>();

        bus.subscribe(TestEvent.class, event -> received.add(event.payload()));
        bus.publish(new TestEvent("hello"));

        assertEquals(List.of("hello"), received);
    }

    @Test
    void multipleSubscribersOfSameTypeAllReceiveTheEvent() {
        EventBus bus = new SimpleEventBus();
        List<String> firstReceived = new ArrayList<>();
        List<String> secondReceived = new ArrayList<>();

        bus.subscribe(TestEvent.class, event -> firstReceived.add(event.payload()));
        bus.subscribe(TestEvent.class, event -> secondReceived.add(event.payload()));
        bus.publish(new TestEvent("hello"));

        assertEquals(List.of("hello"), firstReceived);
        assertEquals(List.of("hello"), secondReceived);
    }

    @Test
    void subscriberOnlyReceivesEventsOfItsOwnType() {
        EventBus bus = new SimpleEventBus();
        List<String> received = new ArrayList<>();

        bus.subscribe(TestEvent.class, event -> received.add(event.payload()));
        bus.publish(new OtherTestEvent("should not arrive"));

        assertTrue(received.isEmpty());
    }

    @Test
    void publishingWithNoSubscribers_doesNothingAndDoesNotThrow() {
        EventBus bus = new SimpleEventBus();

        bus.publish(new TestEvent("nobody is listening"));
        // No assertion needed beyond "this didn't throw" - the point is silent no-op.
    }

    @Test
    void unsubscribedListener_stopsReceivingEvents() {
        EventBus bus = new SimpleEventBus();
        List<String> received = new ArrayList<>();
        java.util.function.Consumer<TestEvent> listener = event -> received.add(event.payload());

        bus.subscribe(TestEvent.class, listener);
        bus.unsubscribe(TestEvent.class, listener);
        bus.publish(new TestEvent("hello"));

        assertTrue(received.isEmpty());
    }

    @Test
    void aSubscriberThatThrows_doesNotPreventOtherSubscribersFromRunning() {
        EventBus bus = new SimpleEventBus();
        List<String> received = new ArrayList<>();

        bus.subscribe(TestEvent.class, event -> {
            throw new RuntimeException("boom");
        });
        bus.subscribe(TestEvent.class, event -> received.add(event.payload()));

        bus.publish(new TestEvent("hello"));

        assertEquals(List.of("hello"), received);
    }
}
