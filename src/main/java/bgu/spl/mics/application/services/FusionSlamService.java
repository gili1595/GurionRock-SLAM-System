package bgu.spl.mics.application.services;


import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;
import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.TrackedObject;
import bgu.spl.mics.application.messages.PoseEvent;
import java.io.*;

/**
 * FusionSlamService integrates data from multiple sensors to build and update
 * the robot's global map.
 * 
 * This service receives TrackedObjectsEvents from LiDAR workers and PoseEvents from the PoseService,
 * transforming and updating the map with new landmarks.
 */
public class FusionSlamService extends MicroService {

    private final FusionSlam fusionSlam; 

    /**
     * Constructor for FusionSlamService.
     *
     * @param fusionSlam The FusionSLAM object responsible for managing the global map.
     */
    public FusionSlamService(FusionSlam fusionSlam) {
        super("FusionSlam Service");
        this.fusionSlam = FusionSlam.getInstance();
    }

    /**
     * Initializes the FusionSlamService.
     * Registers the service to handle TrackedObjectsEvents, PoseEvents, and TickBroadcasts,
     * and sets up callbacks for updating the global map.
     */
    @Override
    protected void initialize() {
        System.out.println(getName() + "statring up...");

        //Subscribe to TickBroadcast
        subscribeBroadcast(TickBroadcast.class, (broadcast)->{
            fusionSlam.updateTime();
        });

        //Subscribe to TerminatedBroadcast
        subscribeBroadcast(TerminatedBroadcast.class, (broadcast)->{
            System.out.println(getName() + " received TerminatedBroadcast. Terminating...");
            
            // fusionSlam.handleTermination();

            try {
                fusionSlam.writeOutputFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            terminate();
        });

        // Subscribe to CrashedBroadcast
        subscribeBroadcast(CrashedBroadcast.class, (broadcast)->{
            System.out.println(getName() + " received CrashedBroadcast. Terminating...");
            try {
                fusionSlam.writeErrorOutputFile(broadcast.getSource(), broadcast.getError());
            } catch (IOException e) {
                e.printStackTrace();
            }
            terminate();
        });

        //Subscribe to PoseEvent
        subscribeEvent(PoseEvent.class, event -> {
            try {
                fusionSlam.updatePose(event.getPose());
            } catch (Exception e) {
                System.err.println("Error updating pose: " + e.getMessage());
            }
        });
        

        //Subscribe to TrackedObjectsEvent
        subscribeEvent(TrackedObjectsEvent.class, event -> {
            try {
                System.out.println(getName() + " received TrackedObjectsEvent");
                for (TrackedObject trackedObject : event.getTrackedObjects()) {
                    // Get pose from the same time as the detection
                    Pose poseAtDetection = fusionSlam.getPose(trackedObject.getTime());
                    if (poseAtDetection != null) {
                        fusionSlam.processTrackedObject(trackedObject, poseAtDetection);
                    } else {
                        System.err.println("No pose found for time: " + trackedObject.getTime());
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing tracked objects: " + e.getMessage());
            }
        });
            
    }


    
}
