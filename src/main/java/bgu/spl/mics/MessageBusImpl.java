package bgu.spl.mics;

import java.util.*;
import java.util.concurrent.*;

/**
 * The {@link MessageBusImpl class is the implementation of the MessageBus interface.
 * Write your implementation here!
 * Only private fields and methods can be added to this class.
 */
public class MessageBusImpl implements MessageBus {

	private final Map<MicroService, BlockingQueue<Message>> MSqueues = new ConcurrentHashMap<>(); //Stores the ms's queues.
	private final Map<Class<? extends Event<?>>, ConcurrentLinkedQueue<MicroService>> eventsub = new ConcurrentHashMap<>();
	private final Map<Class<? extends Broadcast>, ConcurrentLinkedQueue<MicroService>> broadsub = new ConcurrentHashMap<>();
	private final Map<Event<?>, Future<?>> eFutures = new ConcurrentHashMap<>(); // links the Events and the future results.
	
	private static class SingletonHolder{
		private static MessageBusImpl instance = new MessageBusImpl();
		
	}

	public static MessageBusImpl getInstance(){
		return SingletonHolder.instance;
	}

	@Override
	public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
		if (!MSqueues.containsKey(m)) {
			throw new IllegalStateException("MicroService must be registered before subscribing");
		}
		eventsub.computeIfAbsent(type, k -> new ConcurrentLinkedQueue<>()).add(m); // add m to type's queue if type is in the map, it not create and add.

	}

	@Override
	public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) {
		broadsub.computeIfAbsent(type, k -> new ConcurrentLinkedQueue<>()).add(m);

	}

	@Override
	public <T> void complete(Event<T> e, T result) {
		Future<T> future = (Future<T>) eFutures.remove(e);
		if(future != null){
			future.resolve(result);
		}

	}

	@Override
	public void sendBroadcast(Broadcast b) {
		ConcurrentLinkedQueue<MicroService> subs = broadsub.get(b.getClass());
		if (subs != null){
			for(MicroService m : subs){
				BlockingQueue<Message> queue = MSqueues.get(m);
				if (queue != null){
					try{
						queue.offer(b);
					}
					catch(IllegalStateException e){

					}
				}
			}
		}

    }

	
	@Override
	public <T> Future<T> sendEvent(Event<T> e) {
		ConcurrentLinkedQueue<MicroService> subscribers = eventsub.get(e.getClass());
		if (subscribers == null || subscribers.isEmpty()) {
			return null; // No MicroService is subscribed to this event type
		}

		Future<T> future = new Future<>(); // Create a Future for the event result
		eFutures.put(e, future); // Associate the event with the future

		MicroService target = subscribers.poll(); // Get the next MicroService
		synchronized (subscribers) { // Ensure round-robin selection is thread-safe
			if (target != null) {
				subscribers.offer(target); // Add it back to the end of the queue
				BlockingQueue<Message> queue = MSqueues.get(target); // Get its message queue
				if (queue != null) {
					queue.offer(e); // Add the event to the queue
				}
			}
		}
		return future; // Return the Future to the caller

	}

	@Override
	public void register(MicroService m) {
		MSqueues.putIfAbsent(m, new LinkedBlockingQueue<>());

	}

	@Override
	public void unregister(MicroService m) {
		// Remove message queue
		BlockingQueue<Message> queue = MSqueues.remove(m);
		if (queue != null) {
			queue.clear(); // Clean up any pending messages
		}
	
		// Remove from event subscribers
		for (ConcurrentLinkedQueue<MicroService> subscribers : eventsub.values()) {
			subscribers.remove(m);
		}
	
		// Remove from broadcast subscribers
		for (ConcurrentLinkedQueue<MicroService> subscribers : broadsub.values()) {
			subscribers.remove(m);
		}
	
		// Clean up any pending futures
		eFutures.entrySet().removeIf(entry -> {
			Message msg = entry.getKey();
			BlockingQueue<Message> msgQueue = MSqueues.get(m);
			return msgQueue != null && msgQueue.contains(msg);
		});

	}

	@Override
	public Message awaitMessage(MicroService m) throws InterruptedException {
		BlockingQueue<Message> queue = MSqueues.get(m);

		if (queue == null) {
			// If the MicroService is not registered, throw an exception
			throw new IllegalStateException("MicroService is not registered with the MessageBus.");
		}

		// Retrieve and return the next message from the queue, blocking if necessary
		return queue.take();

	}

}
