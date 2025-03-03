package bgu.spl.mics.application.messages;
import java.util.ArrayList;
import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.TrackedObject;

public class TrackedObjectsEvent implements Event<Boolean>{
/**
 * Sent by: a LiDar worker
 * Handled by: Fusion-SLAM
 * Details:
 *  Includes a list of TrackedObjects.
 *  Upon receiving this event, Fusion:
 *      Transforms the cloud points to the charging station's coordinate system using the
 *          current pose.
 *      Checks if the object is new or previously detected:
 *          If new, add it to the map.
 *          If previously detected, updates measurements by averaging with previous data.
 */
    private final ArrayList<TrackedObject> trackedObjects;

    public TrackedObjectsEvent(ArrayList<TrackedObject> trackedObjects){
        this.trackedObjects = trackedObjects;
    }

    public ArrayList<TrackedObject> getTrackedObjects(){
        return trackedObjects;
    }

}