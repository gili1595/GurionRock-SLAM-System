package bgu.spl.mics.application.objects;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;



/**
 * LiDarDataBase is a singleton class responsible for managing LiDAR data.
 * It provides access to cloud point data and other relevant information for tracked objects.
 */
public class LiDarDataBase {
    // Store cloud points with concurrent access 
    private final ConcurrentHashMap<String, ArrayList<StampedCloudPoints>> objectDataMap;
    private final AtomicInteger leftToTrack;

    private static class SingletonHolder{
        private static LiDarDataBase instance; 
    } 
    public static synchronized LiDarDataBase getInstance(String filePath){
        if (SingletonHolder.instance == null){
            SingletonHolder.instance = new LiDarDataBase(filePath);
        }
        return SingletonHolder.instance;
    }

    //Private constructor that loads data from the JSON file.
    //We make it private to enforce the Singleton pattern.
    private LiDarDataBase(String filePath) {
        objectDataMap = new ConcurrentHashMap<>();
        leftToTrack = new AtomicInteger(0);
        loadDataFromFile(filePath);
    }


     //Loads cloud point data from a JSON file into our database
    private void loadDataFromFile(String filePath) {
        Gson gson = new Gson();
        File file = new File(filePath);
        System.out.println("Loading LiDAR data from: " + file.getAbsolutePath());
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            Type lidarDataType = new TypeToken<ArrayList<StampedCloudPoints>>(){}.getType();
            ArrayList<StampedCloudPoints> data = gson.fromJson(reader, lidarDataType);
            
            if (data == null) {
                throw new RuntimeException("Failed to parse LiDAR data: null result");
            }
            
            //organize data by object ID for faster lookups
            for (StampedCloudPoints point : data) {
                String objectId = point.getId();
                objectDataMap.computeIfAbsent(objectId, k -> new ArrayList<>()).add(point);
                leftToTrack.incrementAndGet();
            }       
        } catch (IOException e) {
            System.err.println("Failed to load LiDAR data from file: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }


    //return the cooresponding CloudPoints
    public ArrayList<CloudPoint> getCoordinates(String objectId, int time) {
        // Get all points for this object
        ArrayList<StampedCloudPoints> objectPoints = objectDataMap.get(objectId);
        if (objectPoints == null) {
            throw new IllegalArgumentException("No data found for object: " + objectId);
        }
    
        // Find points for the specific time
        for (StampedCloudPoints p : objectPoints) {
            if (p.getTime() == time) {
                //convert the points to CloudPoint objects
                ArrayList<ArrayList<Double>> points = p.getCloudPoints();
                ArrayList<CloudPoint> coordinates = new ArrayList<>();
                
                // Create CloudPoint objects from the coordinates
                for (int i = 0; i < points.size(); i++) {
                    ArrayList<Double> point = points.get(i);
                    coordinates.add(new CloudPoint(point.get(0), point.get(1)));
                }                
                return coordinates;
            }
        }
        //if we get here, we didn't find points for the requested time
        throw new IllegalArgumentException(
            "No data found for object " + objectId + " at time " + time);
    }

    
     //Checks if we have data for a specific object.
    public boolean hasObjectData(String objectId) {
        return objectDataMap.containsKey(objectId);
    }

    public void setLeftToTrack(){
        leftToTrack.decrementAndGet();
    }

    public boolean isLeftToTrack(){
        return leftToTrack.get()!=0;
    }

    public ArrayList<String> getObjectsIDsAtTime(int time) {
        ArrayList<String> objectIds = new ArrayList<>();
        for (ArrayList<StampedCloudPoints> points : objectDataMap.values()) {
            for (StampedCloudPoints stampedPoints : points) {
                if (stampedPoints.getTime() == time) {
                    objectIds.add(stampedPoints.getId());
                }
            }
        }
        return objectIds;
     }

}