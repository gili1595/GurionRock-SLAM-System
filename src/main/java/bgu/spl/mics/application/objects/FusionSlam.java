package bgu.spl.mics.application.objects;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Manages the fusion of sensor data for simultaneous localization and mapping (SLAM).
 * Combines data from multiple sensors (e.g., LiDAR, camera) to build and update a global map.
 * Implements the Singleton pattern to ensure a single instance of FusionSlam exists.
 */
public class FusionSlam {
    // Singleton instance holder
    private static class FusionSlamHolder {
        private static FusionSlam instance;       
    }
    
    private final ArrayBlockingQueue<LandMark> landmarks;
    private final ArrayList<Pose> poses;
    private final StatisticalFolder stats;
    private final String outputParent;
  
    private FusionSlam(StatisticalFolder stats, String outputParent) {
        this.landmarks = new ArrayBlockingQueue<>(1000);
        this.poses = new ArrayList<>();
        this.stats = stats;
        this.outputParent = outputParent;
    }

    //Initialization method
    public static synchronized void initialize(StatisticalFolder stats, String outPath) {
        if (FusionSlamHolder.instance != null) {
            throw new IllegalStateException("FusionSlam has already been initialized.");
        }
        if (stats == null) {
            throw new IllegalArgumentException("StatisticalFolder cannot be null.");
        }
        FusionSlamHolder.instance = new FusionSlam(stats, outPath);
    }
    

    public static FusionSlam getInstance(){
        if (FusionSlamHolder.instance == null) {
            throw new IllegalStateException("FusionSlam is not initialized. Call initialize() first.");
        }
        return FusionSlamHolder.instance;
    }

    private void updateLandMarkNum(){
        stats.incrementLandmarks(1);
    }

    public void processTrackedObject(TrackedObject trackedObject, Pose currentPose) {
        // Create local landmark
        try{
            LandMark localLandMark = new LandMark(
                trackedObject.getID(),
                trackedObject.getDescription(),
                trackedObject.getCoordinates()
            );

            // Transform to global coordinates
            LandMark globalLandmark = transformToGlobal(localLandMark, currentPose);
            // Update map
            updateLandmarkInMap(globalLandmark);
        } catch (Exception e){
            System.err.println("Failed to process tracked object: " +e.getMessage());
        }
    }


    private void updateLandmarkInMap(LandMark newLandmark) {
        try{
            boolean exists = false;
            ArrayList<LandMark> tmp = new ArrayList<>(landmarks) ;   

            for (LandMark existingLandmark : tmp) {
                if (existingLandmark.getLandmarkId().equals(newLandmark.getLandmarkId())) {
                    exists = true;
                    existingLandmark.updateCoordinates(newLandmark.getCoordinates());
                    break;
                }
            }
            if (!exists) {
                if(landmarks.offer(newLandmark)){
                    updateLandMarkNum();
                }
            }
        } catch (Exception e) {
            System.err.println("failes to update landmark map:" +e.getMessage());
        }   
    }


    //Transforms a local landmark to global coordinates
    public LandMark transformToGlobal(LandMark localLandmark, Pose robotPose) {
        ArrayList<CloudPoint> globalCoordinates = new ArrayList<>();
        try{
            // Transform each CloudPoint in the localLandmark coordinates
            for (CloudPoint localPoint : localLandmark.getCoordinates()) {
                double x = Math.cos(Math.toRadians(robotPose.getYaw())) * localPoint.getCloudPointX() 
                            - Math.sin(Math.toRadians(robotPose.getYaw()))*localPoint.getCloudPointY() + robotPose.getPoseX();

                double y = Math.sin(Math.toRadians(robotPose.getYaw())) * localPoint.getCloudPointX()
                             + Math.cos(Math.toRadians(robotPose.getYaw()))*localPoint.getCloudPointY() + robotPose.getPoseY();

                CloudPoint globalPoint = new CloudPoint(x, y);
                globalCoordinates.add(globalPoint);

        }
        } catch (Exception e) {
            System.err.println("failed to transform coordinates" + e.getMessage());
        }       
        // Return a new global LandMark
        return new LandMark(localLandmark.getLandmarkId(), localLandmark.getDescription(), globalCoordinates);
    
    }  

    public Pose getPose(int time){
        for(Pose p : poses){
            if(p.getTime() == time){
                return p;
            }
        }
        return null;
    }
    
    

    public synchronized void updatePose(Pose pose) {
        try {
            poses.add(pose);

        } catch (Exception e) {
            System.err.println("Failed to update pose: " + e.getMessage());
        }
    }

   

    public void writeOutputFile() throws IOException {
        String outputPath = new File(outputParent, "output_file.json").getPath();
        JsonObject output = new JsonObject();
        // Add statistics
        output.addProperty("systemRuntime", stats.getSystemRuntime());
        output.addProperty("numDetectedObjects", stats.getNumDetectedObjects());
        output.addProperty("numTrackedObjects", stats.getNumTrackedObjects());
        output.addProperty("numLandmarks", stats.getNumLandmarks());
    
        // Create landmarks object
        JsonObject landmarksObj = new JsonObject();
        
        // For each landmark in our list of landmarks
        for (LandMark landmark : landmarks) {
            JsonObject landmarkObj = new JsonObject();
            
            // Add basic landmark info
            landmarkObj.addProperty("id", landmark.getLandmarkId());
            landmarkObj.addProperty("description", landmark.getDescription());
            
            // Create coordinates array
            JsonArray coordinatesArr = new JsonArray();
            for (CloudPoint point : landmark.getCoordinates()) {
                JsonObject coordObj = new JsonObject();
                coordObj.addProperty("x", point.getCloudPointX());
                coordObj.addProperty("y", point.getCloudPointY());
                coordinatesArr.add(coordObj);
            }
            landmarkObj.add("coordinates", coordinatesArr);
            
            // Add this landmark to landmarks object using its ID as key
            landmarksObj.add(landmark.getLandmarkId(), landmarkObj);
        }
        output.add("landMarks", landmarksObj);
    
        // Write to file with pretty printing
        try (Writer writer = new FileWriter(outputPath)) {
        //try (Writer writer = new FileWriter(outPath)) {
            Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
            gson.toJson(output, writer);
        }
        System.out.println("finished output file");
    }

    public synchronized Pose getCurrentPose() {
        try {
            if (poses.isEmpty()) {
                return null;
            }
            return poses.get(poses.size() - 1);
        } catch (Exception e) {
            System.err.println("Failed to get current pose: " + e.getMessage());
            return null;
        }
    }

    public synchronized ArrayList<LandMark> getLandmarks() {
        try {
            return new ArrayList<>(landmarks);
        } catch (Exception e) {
            System.err.println("Failed to get landmarks: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void updateTime(){
        stats.incrementRuntime(1); //TODO:  not accurate solution !!!!
    }

   

    public void writeErrorOutputFile(String faultySensor, String errorDescription) throws IOException {
        String outputPath = new File(outputParent, "OutputError.json").getPath();
        JsonObject output = new JsonObject();
        
        output.addProperty("Error", errorDescription);
        output.addProperty("faultySensor", faultySensor);

        // Add lastFrames
        JsonObject lastFrames = new JsonObject();
        
        // Add cameras' last frames
        JsonObject camerasFrames = new JsonObject();
        for (Map.Entry<Camera, StampedDetectedObjects> entry : stats.getCamerasLastFrame().entrySet()) {
            JsonArray detections = new JsonArray();
            StampedDetectedObjects lastObjects = entry.getValue();
            if (lastObjects != null) {
                for (DetectedObject obj : lastObjects.getDetectedObjects()) {
                    JsonObject detection = new JsonObject();
                    detection.addProperty("time: ", lastObjects.getTime());
                    detection.addProperty("id", obj.getId());
                    detection.addProperty("description", obj.getDescription());
                    detections.add(detection);
                }
            }
            camerasFrames.add("camera" + entry.getKey().getID(), detections);
        }
        lastFrames.add("cameras", camerasFrames);

        // Add lidars' last frames
        JsonObject lidarsFrames = new JsonObject();
        for (Map.Entry<LiDarWorkerTracker, TrackedObject> entry : stats.getLidarsLastFrame().entrySet()) {
            JsonArray cloudPoints = new JsonArray();
            TrackedObject lastObject = entry.getValue();
            if (lastObject != null) {
                JsonObject tracked = new JsonObject();
                tracked.addProperty("time: ", lastObject.getTime());
                tracked.addProperty("id", lastObject.getID());
                tracked.addProperty("description", lastObject.getDescription());
                if (lastObject.getCoordinates() != null) {
                    JsonArray coordinates = new JsonArray();
                    for (CloudPoint point : lastObject.getCoordinates()) {
                        JsonObject coord = new JsonObject();
                        coord.addProperty("x", point.getCloudPointX());
                        coord.addProperty("y", point.getCloudPointY());
                        coordinates.add(coord);
                    }
                    tracked.add("coordinates", coordinates);
                }
                cloudPoints.add(tracked);
            }
            lidarsFrames.add("lidar" + entry.getKey().getID(), cloudPoints);
        }
        lastFrames.add("lidar", lidarsFrames);
        output.add("lastFrames", lastFrames);

        //Add poses
        JsonArray posesArray = new JsonArray();
        for (Pose pose : stats.getPoses()) {
            if (pose.getTime() <= stats.getSystemRuntime()) {
                JsonObject poseObj = new JsonObject();
                poseObj.addProperty("time", pose.getTime());
                poseObj.addProperty("x", pose.getPoseX());
                poseObj.addProperty("y", pose.getPoseY());
                poseObj.addProperty("yaw", pose.getYaw());
                posesArray.add(poseObj);
            }
        }
        output.add("poses", posesArray);

        output.addProperty("systemRuntime", stats.getSystemRuntime());
        output.addProperty("numDetectedObjects", stats.getNumDetectedObjects());
        output.addProperty("numTrackedObjects", stats.getNumTrackedObjects());
        output.addProperty("numLandmarks", stats.getNumLandmarks());

        // Create landmarks object
        JsonObject landmarksObj = new JsonObject();
        
        // For each landmark in our list of landmarks
        for (LandMark landmark : landmarks) {
            JsonObject landmarkObj = new JsonObject();
            
            // Add basic landmark info
            landmarkObj.addProperty("id", landmark.getLandmarkId());
            landmarkObj.addProperty("description", landmark.getDescription());
            
            // Create coordinates array
            JsonArray coordinatesArr = new JsonArray();
            for (CloudPoint point : landmark.getCoordinates()) {
                JsonObject coordObj = new JsonObject();
                coordObj.addProperty("x", point.getCloudPointX());
                coordObj.addProperty("y", point.getCloudPointY());
                coordinatesArr.add(coordObj);
            }
            landmarkObj.add("coordinates", coordinatesArr);
            
            // Add this landmark to landmarks object using its ID as key
            landmarksObj.add(landmark.getLandmarkId(), landmarkObj);
        }
        output.add("landMarks", landmarksObj);

        String rawJson = new GsonBuilder().create().toJson(output);
        String finalJson = rawJson
                // after the error, for instance:
                .replace("\"Error\":", "\n\"error\":")
                .replace("\"faultySensor\":", "\n\"faultySensor\":")
                .replace("\"lastFrames\":", "\n\"lastFrames\":")
                .replace("\"cameras\":", "\n\"lastCameraFrames\":")
                .replace("\"lidar\":", "\n\"lastLidarFrames\":")
                .replace("\"poses\":", "\n\"poses\":")
                .replace("\"systemRuntime\":", "\n\"statistics:  systemRuntime\":")
                .replace("\"numDetectedObjects\":", "\n\"numDetectedObjects\":")
                .replace("\"numTrackedObjects\":", "\n\"numTrackedObjects\":")
                .replace("\"numLandmarks\":", "\n\"numLandmarks\":")
                .replace("\"landMarks\":", "\n\"landMarks\":");

        
        try (Writer w = new FileWriter(outputPath)) {
         w.write(finalJson);
        }           
    }

    // public void handleTermination() {
    //     int finalTick = stats.getSystemRuntime();
    //     Pose finalPose = getPose(finalTick);
    //     if (finalPose != null) {
    //         // Use existing processTrackedObject method for each landmark
    //         ArrayList<LandMark> currentLandmarks = new ArrayList<>(landmarks);
    //         for (LandMark landmark : currentLandmarks) {
    //             TrackedObject obj = new TrackedObject(
    //                 landmark.getLandmarkId(),
    //                 finalTick,
    //                 landmark.getDescription(),
    //                 landmark.getCoordinates()
    //             );
    //             processTrackedObject(obj, finalPose);
    //         }
    //     }
    // }
    
}


