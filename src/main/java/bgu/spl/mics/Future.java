package bgu.spl.mics;

import java.util.concurrent.TimeUnit;

/**
 * A Future object represents a promised result - an object that will
 * eventually be resolved to hold a result of some operation. The class allows
 * Retrieving the result once it is available.
 * 
 * Only private methods may be added to this class.
 * No public constructor is allowed except for the empty constructor.
 */
public class Future<T> {	
	private T result;
	
	/**
	 * This should be the the only public constructor in this class.
	 */

	/**
	 * Empty Constructor.
	 */
	public Future() {
		this.result = null;
		
	}
	
	/**
     * retrieves the result the Future object holds if it has been resolved.
     * This is a blocking method! It waits for the computation in case it has
     * not been completed.
     * <p>
     * @return return the result of type T if it is available, if not wait until it is available.
     * 	       
     */

	/**
	 * We used 'synchronized' to lock this.
	 * Wait so the thread will wait until data is available.
	 * Try and Catch to prevent interruptions.
	 */
	public synchronized T get() {
		try{
			while(result == null)
				this.wait();
			return result;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("thread interrupted", e);
		}
	}
	
	/**
     * Resolves the result of this Future object.
     */
	public synchronized void resolve (T result) {
		if(this.result == null) { //only execute if result isnt already resolved
			this.result = result;
			this.notifyAll(); //notify all waiting threads
		}
	}
	
	/**
     * @return true if this object has been resolved, false otherwise
     */
	public synchronized boolean isDone() {
		return result != null;
	}
	
	/**
     * retrieves the result the Future object holds if it has been resolved,
     * This method is non-blocking, it has a limited amount of time determined
     * by {@code timeout}
     * <p>
     * @param timeout 	the maximal amount of time units to wait for the result.
     * @param unit		the {@link TimeUnit} time units to wait.
     * @return return the result of type T if it is available, if not, 
     * 	       wait for {@code timeout} TimeUnits {@code unit}. If time has
     *         elapsed, return null.
     */
	public synchronized T get(long timeout, TimeUnit unit) {
		if (result == null) {                        // Only wait if unresolved
			long milliSec = unit.toMillis(timeout);  //convert timeout to milliseconds
			long deadline = System.currentTimeMillis() + milliSec; // Calculate deadline
			long remaining = milliSec;                // Initialize remaining wait time

			try {
				while (result == null && remaining > 0) { // Wait for result or timeout
					this.wait(remaining); // Wait for the remaining time
					remaining = deadline - System.currentTimeMillis(); // Recalculate remaining time
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt(); // Restore interrupted status
				throw new RuntimeException("Thread interrupted while waiting for result", e);
			}
		}
		return result; // Return the result if resolved, or null if unresolved
	}
}