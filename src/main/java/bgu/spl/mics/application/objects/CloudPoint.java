package bgu.spl.mics.application.objects;

/**
 * CloudPoint represents a specific point in a 3D space as detected by the LiDAR.
 * These points are used to generate a point cloud representing objects in the environment.
 */
public class CloudPoint {
    //fields
    private final Double x;
    private final Double y;

    //builder
    public CloudPoint(Double x, Double y){
        this.x = x;
        this.y = y;
    }

    //getter
    public Double getCloudPointX(){
        return this.x;  
    }
    public Double getCloudPointY(){
        return this.y;
    }

    //toString
    public String toString(){
        return "("+x+","+y+")";
    }

}
