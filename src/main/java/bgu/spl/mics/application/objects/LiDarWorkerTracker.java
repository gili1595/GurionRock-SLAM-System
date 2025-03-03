package bgu.spl.mics.application.objects;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;


/**
 * LiDarWorkerTracker is responsible for managing a LiDAR worker.
 * It processes DetectObjectsEvents and generates TrackedObjectsEvents by using data from the LiDarDataBase.
 * Each worker tracks objects and sends observations to the FusionSlam service.
 */
public class LiDarWorkerTracker {
    // fields
    private final int id;
    private final int frequency;
    private STATUS status;
    private final ArrayList<TrackedObject> lastTrackedObjects;
    private final ConcurrentLinkedQueue<TrackedObject> pendingObjects;
    private final LiDarDataBase database;
    // private int totalObjectsToProcess;
    // private int processedObjects;
    StatisticalFolder stats;

    //consturctor
    public LiDarWorkerTracker(int id, int frequency, String dataPath, StatisticalFolder stats){
        this.id = id;
        this.frequency = frequency;
        this.status = STATUS.UP;
        this.lastTrackedObjects = new ArrayList<>();
        this.pendingObjects = new ConcurrentLinkedQueue<>();
        this.database = LiDarDataBase.getInstance(dataPath); 
        // this.totalObjectsToProcess = 0;
        // this.processedObjects = 0;
        this.stats = stats;
        stats.registerSensor(getIDString());
    }

    public int getID(){return id;}

    public String getIDString(){
        return ("liar: " + getID());
    }

    public int getFrequency(){return frequency;}

    public STATUS getStatus(){return status;}

    
    
    //Objects will be processed when their time + frequency equals current tick.
    public void addPendingObjects(List<TrackedObject> objectsToTrack) {
        if (objectsToTrack == null) 
            return;

      //  totalObjectsToProcess += objectsToTrack.size();
        for (TrackedObject object : objectsToTrack) {
            pendingObjects.offer(object);
        }
    }

    
    /**
     * Processes a detection event by retrieving point cloud data from the database
     * This converts camera detections into precise measurements.
     */
    public TrackedObjectsEvent processDetection(DetectObjectsEvent event) {
        if (event == null || event.getDetectedObjects() == null || event.getDetectedObjects().isEmpty()) {
            return null;
        }

        //Convert DetectedObjects to TrackedObjects (initially without coordinates)
        ArrayList<TrackedObject> pendingTrackedObjects = new ArrayList<>();

        //Get the detected objects from the camera event
        for (DetectedObject detected : event.getDetectedObjects()) {
            TrackedObject tracked = new TrackedObject(
                detected.getId(),
                event.getTimeStamp(),
                detected.getDescription(),
                null //Coordinates will be filled in later during processing
            );
            //check for error condition
            if(!isValid(tracked)){
                System.out.println(getID() + " detected ERROR");
                status = STATUS.ERROR;
                return null;
            }

            pendingTrackedObjects.add(tracked); 
        }
        addPendingObjects(pendingTrackedObjects);
        // return new TrackedObjectsEvent(pendingTrackedObjects);
        return null;
    }


    
    /**
     * Processes time ticks and returns events that are ready for processing.
     * An event is ready when current tick equals detection time plus frequency.
     */
    public ArrayList<TrackedObjectsEvent> processTimeStep(int currentTick) {
        if (status == STATUS.ERROR) {
            return null;
        }
        ArrayList<TrackedObjectsEvent> readyEvents = new ArrayList<>();
        ArrayList<TrackedObject> objectsToProcess = getReadyObjects(currentTick);
        
        if (!objectsToProcess.isEmpty()) {
            // Check database for ERROR at this time tick
            for(String obj : database.getObjectsIDsAtTime(currentTick)) {
                if(obj.equals("ERROR")) {
                    status = STATUS.ERROR;
                    return null;
                }
            }
            ArrayList<TrackedObject> processedObjects = new ArrayList<>();
            for (TrackedObject obj : objectsToProcess) {
                //check for error condition before processing
                if(!isValid(obj)){
                    status = STATUS.ERROR;
                    return null;
                }
                try {
                    ArrayList<CloudPoint> coordinates = database.getCoordinates(obj.getID(), obj.getTime());
                    TrackedObject processedObj = new TrackedObject(
                        obj.getID(),
                        obj.getTime(),
                        obj.getDescription(),
                        coordinates
                    );
                    processedObjects.add(processedObj);
                    stats.updateLidarLastFrame(this, processedObj);
                    stats.incrementTrackedObjects(1);
                    database.setLeftToTrack();
                    System.out.println("LiDar successfully tracked object " + obj.getID() + " at time " + obj.getTime() + ", total tracked: " + stats.getNumTrackedObjects());
             //       this.processedObjects++; //increment processed count

                } catch (Exception e) {
                    System.err.println("Failed to process object " + obj.getID());
                }
            }
            if (!processedObjects.isEmpty()) {
                TrackedObjectsEvent event = new TrackedObjectsEvent(processedObjects);
                readyEvents.add(event);
                updateLastTrackedObjects(processedObjects);
            }
        }    
        return readyEvents;
    }        

    // Check pending objects to see which are ready
    private ArrayList<TrackedObject> getReadyObjects(int currentTick) {
        ArrayList<TrackedObject> readyObjects = new ArrayList<>();
        TrackedObject object;
        while ((object = pendingObjects.peek()) != null) {
            if (object.getTime() + frequency <= currentTick) {
                pendingObjects.poll();
                readyObjects.add(object);
            } else {
                // If this object isn't ready, later ones won't be either
                break;
            }
        }
        return readyObjects;
    }
    
    private void updateLastTrackedObjects(ArrayList<TrackedObject> objects) {
        lastTrackedObjects.clear();
        lastTrackedObjects.addAll(objects);
    }

    //method to check for error
    public boolean isValid(TrackedObject trackedObject){
        if(trackedObject!=null){
            System.out.println("LiDar " + getID() + " checking object ID: " + trackedObject.getID());
            if(trackedObject.getID().equals("ERROR")){
                System.out.println("LiDar: "+ getID() + " detected error");
                return false;
            }
        }
        return true;
    }

    public boolean isLeft(){
        if(database.isLeftToTrack())
            return true;
        else{
            status = STATUS.DOWN;
            stats.updateSensorStatus(getIDString(), status);  //updating the number of active sensors in statistical folder
            return false;
        }
    }

    public boolean areAllSensorsDown(){
        return stats.areAllSensorsDown();
    }
}
