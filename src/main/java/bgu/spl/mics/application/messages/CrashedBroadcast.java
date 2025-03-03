package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;

public class CrashedBroadcast implements Broadcast {
    private final String source; // The name, ID of the crashed component
    private final String error;

    public CrashedBroadcast(String source, String error) {
        this.source = source;
        this.error = error;
    }    
    public String getSource() {
        return source;
    }
    public String getError() {
        return error;
    }

}
