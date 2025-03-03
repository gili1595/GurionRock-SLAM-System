package bgu.spl.mics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;

public class MessageBusTest {
    private MessageBusImpl messageBus;
    private MicroService testService1;
    private MicroService testService2;
    
    class TestEvent implements Event<String> {}
    class TestBroadcast implements Broadcast {}
    
    class TestMicroService extends MicroService {
        public TestMicroService(String name) {
            super(name);
        }
        @Override
        protected void initialize() {}
    }

    @BeforeEach
    public void setUp() {
        System.out.println("Setting up test environment for msgBus");
        messageBus = MessageBusImpl.getInstance();
        testService1 = new TestMicroService("service1");
        testService2 = new TestMicroService("service2");
        
        //clear any previous registrations
        messageBus.unregister(testService1);
        messageBus.unregister(testService2);
        System.out.println("Unregistered any existing services.");
    
        messageBus.register(testService1);
        messageBus.register(testService2);
        System.out.println("Registered test services: service1 and service2.");
    }

    @Test //testing that unregister- removes it from event and broadcast subscribers, makes its queue inaccessible
    public void testUnregister() {
        System.out.println("Starting testUnregister..");

        //Subscribe service to events and broadcasts
        messageBus.subscribeEvent(TestEvent.class, testService1);
        messageBus.subscribeBroadcast(TestBroadcast.class, testService1);

        // Unregister the service
        messageBus.unregister(testService1);

        //verify service can't receive events anymore
        Future<String> future = messageBus.sendEvent(new TestEvent());
        assertNull(future, "Event sent to unregistered service should return null");
    }

    @Test //verify multiple registrations of the same microservices don't cause issues
    public void testRegister() {
        System.out.println("Starting testRegister..");

        // Already registered in setUp
        messageBus.register(testService1);  
        TestEvent event = new TestEvent();

        // Subscribe and send event
        messageBus.subscribeEvent(TestEvent.class, testService1);
        Future<String> future = messageBus.sendEvent(event);

        // Should still work despite duplicate registration
        assertNotNull(future, "Should work");
    }

    @Test //testing if trying to await messages for an unregistered microservice throws an exeption
    public void testAwaitMessageUnregistered() {
        System.out.println("Starting testAwaitMessageUnregistered..");
        // Create new MicroService without registering it
        MicroService unregistered = new TestMicroService("unregistered");
        // Assert that calling awaitMessage throws IllegalStateException
        assertThrows(IllegalStateException.class, () -> messageBus.awaitMessage(unregistered));
    }

    @Test //test when sending an event with no subscribed microservices
    public void testSendEventNoSubscribers() {
        System.out.println("Starting testSendEventNoSubscribers..");
        // Send event without any subscribers registered for TestEvent type
        Future<String> future = messageBus.sendEvent(new TestEvent());
        //should return null 
        assertNull(future, "Should return null when no subscribers exist");
    }

    @Test
    public void testSubscribeAndSendEvent() throws InterruptedException {
        System.out.println("Starting testSubscribeAndSendEvent...");

        //subscribe service to event 
        messageBus.subscribeEvent(TestEvent.class, testService1);

        //send event and get future
        TestEvent event = new TestEvent();
        Future<String> future = messageBus.sendEvent(event);

        //verify not null
        assertNotNull(future, "Future should not be null");

        //verify service received the event
        assertTrue(messageBus.awaitMessage(testService1) instanceof TestEvent);
    }

    @Test
    public void testBroadcastMessage() throws InterruptedException {
        System.out.println("Starting testBroadcastMessage...");

        //subscribe services to broadcast 
        messageBus.subscribeBroadcast(TestBroadcast.class, testService1);
        messageBus.subscribeBroadcast(TestBroadcast.class, testService2);

        //send event
        TestBroadcast broadcast = new TestBroadcast();
        messageBus.sendBroadcast(broadcast);

        //verify services received the broadcast
        assertTrue(messageBus.awaitMessage(testService1) instanceof TestBroadcast);
        assertTrue(messageBus.awaitMessage(testService2) instanceof TestBroadcast);
    }

    @Test
    public void testEventRoundRobin() throws InterruptedException {
        System.out.println("Starting testEventRoundRobin...");
        
        //bubscribe both services
        messageBus.subscribeEvent(TestEvent.class, testService1);
        messageBus.subscribeEvent(TestEvent.class, testService2);

        //send two events
        TestEvent event1 = new TestEvent();
        TestEvent event2 = new TestEvent();
        Future<String> future1 = messageBus.sendEvent(event1);
        Future<String> future2 = messageBus.sendEvent(event2);

        //both futures should exist
        assertNotNull(future1);
        assertNotNull(future2);

        //each service should get one event
        Message received1 = messageBus.awaitMessage(testService1);
        Message received2 = messageBus.awaitMessage(testService2);

        //verify services received the events
        assertTrue(received1 instanceof TestEvent);
        assertTrue(received2 instanceof TestEvent);
        assertNotSame(received1, received2);
    }

    @Test
    public void testCompleteEvent() throws InterruptedException {
        System.out.println("Starting testCompleteEvent...");
        messageBus.subscribeEvent(TestEvent.class, testService1);
        
        TestEvent event = new TestEvent();
        Future<String> future = messageBus.sendEvent(event);
        assertNotNull(future);

        // Service receives event
        Message receivedMessage = messageBus.awaitMessage(testService1);
        assertTrue(receivedMessage instanceof TestEvent);
        
        //complete the event
        messageBus.complete((Event<String>)receivedMessage, "task completed");
        
        //verify future is resolved with correct result
        assertTrue(future.isDone());
        assertEquals("task completed", future.get());
    }

    @AfterEach
    public void tearDown() {
        System.out.println("Tearing down test environment of msgBus test");
        messageBus.unregister(testService1);
        messageBus.unregister(testService2);
    }
}
