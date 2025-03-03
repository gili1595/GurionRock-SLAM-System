package bgu.spl.mics.application.objects;
import java.util.ArrayList;


/**
 * Represents a group of cloud points corresponding to a specific timestamp.
 * Used by the LiDAR system to store and process point cloud data for tracked objects.
 */

public class StampedCloudPoints {
    //fields  
   private final String id;
   private int time;
   private ArrayList<ArrayList<Double>> cloudPoints;

    //bulder
    public StampedCloudPoints (String id, int time, ArrayList<ArrayList<Double>> cloudPoints){
        this.id = id;
        this.time = time;
        this.cloudPoints = cloudPoints;
    }

    //getters
    public String getId(){
        return this.id;
    }
    public int getTime(){
        return this.time;
    }
    public ArrayList<ArrayList<Double>> getCloudPoints(){
        return this.cloudPoints;
    }
}    
