package bgu.spl.mics.application.objects;

import java.util.ArrayList;

/**
 * Represents objects detected by the camera at a specific timestamp.
 * Includes the time of detection and a list of detected objects.
 */
public class StampedDetectedObjects {
    
    // fields
    private final int time;
    private final ArrayList<DetectedObject> DetectedObjects;

    // constructor
    public StampedDetectedObjects(int time, ArrayList<DetectedObject> list){
        this.time = time;
        this.DetectedObjects = list;
    }

    //add object to the list.
    public void add(DetectedObject o){
        this.DetectedObjects.add(o);
    }

    // get time
    public int getTime(){
        return this.time;
    }

    public ArrayList<DetectedObject> getDetectedObjects(){
        return DetectedObjects;}

}
