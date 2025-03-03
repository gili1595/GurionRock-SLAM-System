package bgu.spl.mics.application.objects;
import java.util.ArrayList;

/**
 * Represents a landmark in the environment map.
 * Landmarks are identified and updated by the FusionSlam service.
 */
public class LandMark {
    private final String id;
    private final String description;
    private ArrayList<CloudPoint> coordinates;

    //constructor
    public LandMark(String id, String description, ArrayList<CloudPoint> coordinates){
        this.id = id;
        this.description = description;
        this.coordinates = coordinates;
    }

    //if an object is detected angain will take the avarage coordinates
    public synchronized void updateCoordinates(ArrayList<CloudPoint> newCoordinates) {
        try {
            ArrayList<CloudPoint> updatedCoordinates = new ArrayList<>();
         //   int count = avg.get();
            for (int i = 0; i < Math.min(coordinates.size(), newCoordinates.size()); i++) {
                CloudPoint existing = coordinates.get(i);
                CloudPoint newPoint = newCoordinates.get(i);
                
                double newX = (existing.getCloudPointX() + newPoint.getCloudPointX()) / 2;
                                   
                double newY = (existing.getCloudPointY() + newPoint.getCloudPointY()) / 2;
                            
                updatedCoordinates.add(new CloudPoint(newX, newY));
            }

            if(newCoordinates.size() > coordinates.size()){
                for(int i = newCoordinates.size() - coordinates.size(); i < newCoordinates.size(); i ++){
                    updatedCoordinates.add(newCoordinates.get(i));
                }
            }

            // Save the newly averaged coordinates
            this.coordinates = updatedCoordinates;
                
        } catch (Exception e) {
            System.err.println("Failed to update coordinates: " + e.getMessage());
        }
    }

    //getter
    public String getLandmarkId(){
        return this.id;
    }
    public String getDescription(){
        return this.description;
    }
    public synchronized ArrayList<CloudPoint> getCoordinates(){
        return this.coordinates;
    }
}
