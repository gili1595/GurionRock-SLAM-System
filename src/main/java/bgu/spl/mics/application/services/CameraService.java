package bgu.spl.mics.application.services;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StampedDetectedObjects;
import bgu.spl.mics.application.objects.StatisticalFolder;

/**
 * CameraService is responsible for processing data from the camera and
 * sending DetectObjectsEvents to LiDAR workers.
 * 
 * This service interacts with the Camera object to detect objects and updates
 * the system's StatisticalFolder upon sending its observations.
 */
public class CameraService extends MicroService {
    private final Camera camera;
    // private final StatisticalFolder stats;

    /**
     * Constructor for CameraService.
     *
     * @param camera The Camera object that this service will use to detect objects.
     */
    public CameraService(Camera camera, StatisticalFolder stats) {
        super("CameraService - " + camera.getID());
        this.camera = camera;
        // this.stats = stats;       
    }

    /**
     * Initializes the CameraService.
     * Registers the service to handle TickBroadcasts and sets up callbacks for sending DetectObjectsEvents.
     */
    @Override
    protected void initialize() {  
        System.out.println(getName() + "Starting up...");
        
        //initialize total detection counter
        camera.initializeTotalDetections();

        //subsribe to time updates (tickBroadcast) to know when to prosses
        subscribeBroadcast(TickBroadcast.class, (TickBroadcast tick) -> {     
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
    }

    private void handleTick(TickBroadcast tick) {
        if (camera.getStatus() == STATUS.ERROR) {
            System.out.println(getName() + " detected an error in Camera. Sending CrashedBroadcast...");
            sendBroadcast(new CrashedBroadcast(camera.getIDString(), "Camera status is ERROR") );
            terminate(); //after telling the rest terminate
            return;
        }
        if(camera.getStatus()==STATUS.UP){
            
            StampedDetectedObjects detections = camera.processDetectionsForTick(tick.getCurrentTime());

            //Check if detected ERROR
            DetectedObject tmp = camera.isValid(detections);
            if(tmp != null){
                System.out.println(getName()+" sending crashed broadcast and terminating...");
                sendBroadcast(new CrashedBroadcast(camera.getIDString(), tmp.getDescription()));
                terminate();
            }

            if (detections != null) {
                // Send detection event
                sendEvent(new DetectObjectsEvent(detections));
            }
        } 
        //if camera stutus is down:
        else{ 
            //if all the sensors are down terminate
            if(camera.areAllSensorsDown()){
                System.out.println(getName() + " detected all sensors are DOWN. Initiating termination...");
                sendBroadcast(new TerminatedBroadcast());
                terminate();
            }
        }   
    }
}
