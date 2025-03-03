package bgu.spl.mics.application.services;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.PoseEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.GPSIMU;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.STATUS;


/**
 * PoseService is responsible for maintaining the robot's current pose (position and orientation)
 * and broadcasting PoseEvents at every tick.
 */
public class PoseService extends MicroService {
    private final GPSIMU gpsimu;
    private Pose lastValidPose;
    private boolean gotPose = true;

    /**
     * Constructor for PoseService.
     *
     * @param gpsimu The GPSIMU object that provides the robot's pose data.
     */
    public PoseService(GPSIMU gpsimu) {
        super("PoseService");
        this.gpsimu = gpsimu;
        this.lastValidPose = null;
    }

    //Subscribes to TickBroadcast and sends PoseEvents at every tick based on the current pose
    @Override
    protected void initialize() {
        System.out.println(getName() + " starting up...");

        //subscribe to time updates to know when to publish pose data
        subscribeBroadcast(TickBroadcast.class, (TickBroadcast tick) -> {     
            handleTick(tick);
            
        });

        //Subscribe to crash notifications
        subscribeBroadcast(CrashedBroadcast.class, broadcast -> {
            System.out.println(getName() + " shutting down due to system crash.");
            terminate();
        });

         //Subscribe to terminate notifications
        subscribeBroadcast(TerminatedBroadcast.class, broadcast -> {
            System.out.println(getName() + " received TerminatedBroadcast. Terminating...");
            terminate();
        });
    }
    
    //handles incoming time ticks by updating the current pose
    //this method is called every time a new TickBroadcast arrives
    private void handleTick(TickBroadcast tick) {
        
         // Check if GPSIMU is in an error state
        if (gpsimu.getStatus() == STATUS.ERROR) {
            System.out.println(getName() + " detected an error in GPSIMU. Sending CrashedBroadcast...");
            
            // Send CrashedBroadcast with the name of this service as the source
            sendBroadcast(new CrashedBroadcast(getName(), "GPSIMU status is ERROR"));
            
            // Terminate this service after notifing the rest
            terminate();
            return; // Stop further processing
        }
        //test
        if(!gotPose){
            System.out.println(getName() + " - No more poses available. Sending CrashedBroadcast...");
            
            // Send CrashedBroadcast with the name of this service as the source
            sendBroadcast(new TerminatedBroadcast());
            
            // Terminate this service after notifing the rest
            terminate();
            return; // Stop further processing
        }


        try {        
            gpsimu.setCurrentTick(tick.getCurrentTime());
            Pose currentPose = gpsimu.getPose();
            lastValidPose = currentPose;
            gpsimu.updatePose(currentPose);

            PoseEvent poseEvent = new PoseEvent(currentPose, tick.getCurrentTime());
           
            sendEvent(poseEvent);
            
        } catch (IndexOutOfBoundsException e) {
            if(lastValidPose != null) {
                System.out.println(getName() + "- Using last valid pose for tick: " + tick.getCurrentTime());
                PoseEvent poseEvent = new PoseEvent(lastValidPose, tick.getCurrentTime());
                sendEvent(poseEvent);
                gotPose = false;
            }
        }
    }           
}
