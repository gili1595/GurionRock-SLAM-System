package bgu.spl.mics.application.objects;

/**
 * Represents the robot's pose (position and orientation) in the environment.
 * Includes x, y coordinates and the yaw angle relative to a global coordinate system.
 */
public class Pose {
    private final float x;
    private final float y;      //locatiod according to charging station
    private final float yaw;    //the orientation angle relative to the charging station's coordinate system.
    private final int time;     //the time when the robot reaches the pose

    //constructor
    public Pose (float x, float y, float yaw, int time){
        this.x = x;
        this.y = y; 
        this.yaw = yaw;
        this.time = time;
    }

    //getters
    public float getPoseX(){
        return this.x;
    }
    public float getPoseY(){
        return this.y;
    }
    public float getYaw(){
        return this.yaw;
    }
    public int getTime(){
        return this.time;
    }
    public String toString(){
         return "location Pose: ("+x+","+y+"), yaw: "+yaw+", time when reached pose: "+time;
    }  
}

