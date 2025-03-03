package bgu.spl.mics.application.services;
import java.util.ArrayList;
import java.util.stream.Collectors;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.LiDarWorkerTracker;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StatisticalFolder;

/**
 * LiDarService is responsible for processing data from the LiDAR sensor and
 * sending TrackedObjectsEvents to the FusionSLAM service.
 * 
 * This service interacts with the LiDarTracker object to retrieve and process
 * cloud point data and updates the system's StatisticalFolder upon sending its
 * observations.
 */
public class LiDarService extends MicroService { 
    private final LiDarWorkerTracker liDar;
    
    /**
     * Constructor for LiDarService.
     *
     * @param liDarTracker The LiDAR tracker object that this service will use to process data.
     */
    public LiDarService(LiDarWorkerTracker liDarTracker, StatisticalFolder stats) {
        super("LiDar - " + liDarTracker.getID());
        this.liDar = liDarTracker;
    }

    /**
     * Initializes the LiDarService.
     * Registers the service to handle DetectObjectsEvents and TickBroadcasts,
     * and sets up the necessary callbacks for processing data.
     */
    @Override
    protected void initialize() {    
        System.out.println(getName() + "Starting up...");

        //Subscribe to time updates
        subscribeBroadcast(TickBroadcast.class, tick -> {
            handleTick(tick);
        });
        
        // Subscribe to termination messages
        subscribeBroadcast(TerminatedBroadcast.class, (broadcast) -> {
            System.out.println(getName() + " received TerminatedBroadcast. Terminating...");
            terminate();
        });
      
        // Subscribe to crash notifications
       subscribeBroadcast(CrashedBroadcast.class, (broadcast) -> {
            System.out.println(getName() + " received CrashedBroadcast. Terminating...");
            terminate();
        });
        
        subscribeEvent(DetectObjectsEvent.class, event -> {
            handleDetection(event);
        });
    }


    /**
    * Handles incoming time ticks by processing any pending tracked objects
    * that are ready according to the LiDAR's frequency.
    */
    private void handleTick(TickBroadcast tick) {
         // Check if LiDAR is in an error state
        if (liDar.getStatus() == STATUS.ERROR) {
            System.out.println(getName() + " detected an error in LiDAR. Sending CrashedBroadcast...");
            sendBroadcast(new CrashedBroadcast(liDar.getIDString(), "lidar status is ERROR"));
            terminate();
            return;
        }
        
        if(!liDar.isLeft()){ //meanning lidar status is DOWN
            System.out.println(getName() + " has no more objects to track. Setting Lidar -"+ liDar.getID() + " status to DOWN");
            //if all the sensors are down terminate
            if (liDar.areAllSensorsDown()){
                System.out.println(getName() + " detected all sensors are DOWN. Initiating termination...");
                sendBroadcast(new TerminatedBroadcast());
                terminate();
            }
        }

        //send event and update statistics
        ArrayList<TrackedObjectsEvent> events = liDar.processTimeStep(tick.getCurrentTime());

        if (events != null) {
            for (TrackedObjectsEvent event : events) {
                sendEvent(event);
            }
        }
    }


    /**
     * Handles incoming camera detections by queuing them for processing
     * according to the LiDAR's frequency.
     */   
    private void handleDetection(DetectObjectsEvent event) {
        // System.out.println(getName() + " received detection event");
        System.out.println(getName() + " received detection event with objects: " + 
                      event.getDetectedObjects().stream()
                          .map(DetectedObject::getId)
                          .collect(Collectors.joining(", "))); // Debug
        try {
            TrackedObjectsEvent result = liDar.processDetection(event);
            if (liDar.getStatus() == STATUS.ERROR) {  // Add this check
                sendBroadcast(new CrashedBroadcast(liDar.getIDString(), "Error detected in LiDAR data"));
                terminate();
                return;
            }
            complete(event, result != null); //Signal successful back to camera
        } catch (Exception e) {
            System.err.println(getName() + " failed to process detection: " + e.getMessage());
            complete(event, false); 
        }
    }
    

}