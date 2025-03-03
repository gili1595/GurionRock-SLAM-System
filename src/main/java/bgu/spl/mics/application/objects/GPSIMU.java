package bgu.spl.mics.application.objects;
import java.util.ArrayList;

/**
 * Represents the robot's GPS and IMU system.
 * Provides information about the robot's position and movement.
 */
public class GPSIMU {
    private int currentTick;    
    private STATUS status;      //Up, Down, Error
    private final ArrayList<Pose> poseList; //represents a list of time-stamped poses
    private final StatisticalFolder stats;

    //constructor
    public GPSIMU (int currentTick, ArrayList<Pose> poseList, StatisticalFolder stats){
        this.currentTick = currentTick;
        this.status = STATUS.UP; 
        this.poseList = poseList;
        this.stats = stats;
    }

    //getters
    public int getCurrentTick(){
        return this.currentTick;
    }
    public STATUS getStatus(){
        return this.status;
    }
    public ArrayList<Pose> getPoseList(){
        return this.poseList;
    }
   
    public Pose getPose() {
        for (Pose pose : poseList) {
            if (pose.getTime() == currentTick) {
                return pose;
            }
        }
        throw new IndexOutOfBoundsException("No pose found for tick: " + currentTick);
    }

    //setters
    public void setCurrentTick(int currentTick){
        this.currentTick = currentTick;
    }

    public void setStatus(STATUS newStatus) {
        this.status = newStatus;
    }
   

    public void updatePose(Pose pose){
        stats.updatePoses(pose);
    }
}          


 




