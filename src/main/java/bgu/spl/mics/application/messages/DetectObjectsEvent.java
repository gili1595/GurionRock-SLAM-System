package bgu.spl.mics.application.messages;
import java.util.ArrayList;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.StampedDetectedObjects;

public class DetectObjectsEvent implements Event<Boolean>{
    /**
     *  The Camera send DetectedObjectsEvent of the Objects with time T for all the
     *  subscribed Lidar workers to this event at time T, and one of them deals with a single
     *  event.
     *  The LiDar gets the X’s,Y’s coordinates from the DataBase of them and sends a new
     *  TrackedObjectsEvent to the Fusion.
     *  After the LiDar Worker completes the event, it saves the coordinates in the lastObjects
     *  variable in DataBase and sends True value to the Camera
     */

   private final StampedDetectedObjects detectedObjects;

   public DetectObjectsEvent(StampedDetectedObjects detectedObjects){
      this.detectedObjects = detectedObjects;
   }

   public ArrayList<DetectedObject> getDetectedObjects() {  
      return detectedObjects.getDetectedObjects();
   }

   //Gets the timestamp of when these objects were detected
    public int getTimeStamp() {
        return detectedObjects.getTime();
    }

}  
