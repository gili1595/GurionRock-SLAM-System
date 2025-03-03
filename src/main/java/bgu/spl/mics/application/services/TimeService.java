package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.*;


/**
 * TimeService acts as the global timer for the system, broadcasting TickBroadcast messages
 * at regular intervals and controlling the simulation's duration.
 */
public class TimeService extends MicroService {
    private final int tickTime;
    private final int duration;
    private volatile boolean shouldRun = true;

    /**
     * Constructor for TimeService.
     *
     * @param TickTime  The duration of each tick in milliseconds.
     * @param Duration  The total number of ticks before the service terminates.
     */
    public TimeService(int tickTime, int duration) {
        super("TimeService");
        this.tickTime = tickTime;
        this.duration = duration;
    }

    /**
     * Initializes the TimeService.
     * Starts broadcasting TickBroadcast messages and terminates after the specified duration.
     */
    @Override
    protected void initialize() {
        System.out.println(getName() + " starting up...");    
        subscribeBroadcast(CrashedBroadcast.class, b -> {
            System.out.println(getName() + " received CrashedBroadcast. Terminating...");
            shouldRun = false;
            terminate();
        }); 
        subscribeBroadcast(TerminatedBroadcast.class, b -> {
            System.out.println(getName() + " received TerminatedBroadcast. Terminating...");
            shouldRun = false;
            terminate();
        });
        Thread timerThread = new Thread(() -> {
            try {
                // Run from 1 to duration inclusive
                for (int currentTick = 1; currentTick <= duration && shouldRun; currentTick++) {
                    // Send tick broadcast with current time
                    TickBroadcast tick = new TickBroadcast(currentTick);
                    sendBroadcast(tick);
                    System.out.println("TimeService: Sent tick " + currentTick);
                    
                    // Wait for next tick
                    Thread.sleep(tickTime);
                }      
                // After all ticks are done, send termination
                if(shouldRun){
                    sendBroadcast(new TerminatedBroadcast());
                    System.out.println(getName() + " completed " + duration + " ticks. Terminating...");
                }
            } catch (InterruptedException e) {
                System.err.println(getName() + " was interrupted. Shutting down...");
                Thread.currentThread().interrupt();
            }
            terminate();
        });      
        timerThread.start();
    }
}
