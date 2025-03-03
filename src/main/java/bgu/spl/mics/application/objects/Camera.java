package bgu.spl.mics.application.objects;
import java.util.ArrayList;
/**
 * Represents a camera sensor on the robot.
 * Responsible for detecting objects in the environment.
 */
public class Camera {
    private final int id;
    private final int frequency;
    private STATUS status;
    private ArrayList<StampedDetectedObjects> detectedObjectsList;   //store detected objects orginaized according yo yime stamp
    private int lastProcessedTick = 0;
    private int totalDetections; 
    private int processedDetections = 0;
    private final StatisticalFolder stats; 

    //constractur
    public Camera (int id, int frequency, StatisticalFolder stats){
        this.id = id;
        this.frequency = frequency;
        this.status = STATUS.UP; //camera starts in opreational state
        this.detectedObjectsList = new ArrayList<>();
        this.stats = stats;
        stats.registerSensor(getIDString());   //updading the number of active sensors in statistical folder
    }

    public void initializeTotalDetections(){
        this.totalDetections = 0;
        for (StampedDetectedObjects obj : detectedObjectsList){
            this.totalDetections += obj.getDetectedObjects().size();
        }
    }
 
    // gets Camera's ID
    public int getID(){
        return this.id;
    }

    public String getIDString(){
        return ("camera:" +getID());
    }

    // gets Camera's frequency
    public int getFrequency(){
        return this.frequency;
    }

    // gets Camera's status
    public STATUS getStatus(){
        return this.status;
    }

    //gets Camera's detected objects list
    public ArrayList<StampedDetectedObjects> getDetectedObjectsList(){
        return this.detectedObjectsList;
    }

    //method to process detections for a given tick
    public StampedDetectedObjects processDetectionsForTick(int currentTick) {
        if (status == STATUS.ERROR) {
            return null;
        }
        for (StampedDetectedObjects objects : detectedObjectsList) {
            // Process data when:
            // 1. Detection time plus frequency equals current time
            // 2. Haven't processed this detection before
            if (objects.getTime() + frequency == currentTick && 
                objects.getTime() > lastProcessedTick) {    
                
                lastProcessedTick = objects.getTime();
                processedDetections += objects.getDetectedObjects().size();

                // Check if this was the last detection
                if (processedDetections >= totalDetections) {
                    status = STATUS.DOWN;
                    stats.updateSensorStatus(getIDString(), status);   //updating the number of active sensors in statistical folder
                    System.out.println("Camera " + id + " finished processing all detections. Status changed to DOWN");
                }
                if(isValid(objects)==null){
                    stats.updateCameraLastFrame(this, objects);
                    stats.incrementDetectedObjects(objects.getDetectedObjects().size());
                }
                return objects;
            }
        }
        return null;
    }

    public void clearDetections() {
        detectedObjectsList.clear();
        lastProcessedTick = 0;
    }

    public void setStatus(STATUS newStatus) {
        this.status = newStatus;
        if (newStatus == STATUS.ERROR) {
            clearDetections();
        }
    }

    //check if we encounter ERROR
    public DetectedObject isValid(StampedDetectedObjects stampedDetectedObjects){
        if(stampedDetectedObjects != null){
            for (DetectedObject o : stampedDetectedObjects.getDetectedObjects()) {
                if(o.getId().equals("ERROR")){
                    System.out.println("Camera: "+ getID()+ "recieved ERROR:" + o.getDescription());
                    return o;
                }
            }
        }
        return null;
    }
    
    //checkong through sstatistical folder if all the sensors are DOWN
    public boolean areAllSensorsDown(){ 
        return stats.areAllSensorsDown();
    }
}
