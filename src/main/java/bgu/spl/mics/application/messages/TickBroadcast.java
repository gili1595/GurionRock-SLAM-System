package bgu.spl.mics.application.messages;
import bgu.spl.mics.Broadcast;

public class TickBroadcast implements Broadcast {
/**
 * Sent by: TimeService
 * Used for: Timing message publications and processing.
 */
    //fields
    private final int tick;

    public TickBroadcast(int tick) {
        this.tick = tick;
    }
     public int getCurrentTime() {
        return tick;
    }
}
