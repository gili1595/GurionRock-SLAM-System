package bgu.spl.mics.application.objects;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * Holds statistical information about the system's operation.
 * This class aggregates metrics such as the runtime of the system,
 * the number of objects detected and tracked, and the number of landmarks identified.
 */
public class StatisticalFolder {

    //atomic because multiple threads can increment or read the statistics simultaneously
    private final AtomicInteger systemRuntime;       // Total runtime of the system in ticks
    private final AtomicInteger numDetectedObjects;  // Total number of objects detected by cameras
    private final AtomicInteger numTrackedObjects;   // Total number of objects tracked by LiDAR workers
    private final AtomicInteger numLandmarks;        // Total number of unique landmarks identified
    private final ConcurrentHashMap<Camera, StampedDetectedObjects> camerasLastFrame;
    private final ConcurrentHashMap<LiDarWorkerTracker, TrackedObject> lidarsLastFrame;
    private final ArrayList<Pose> poses;
    private final ConcurrentHashMap<String, STATUS> sensorStatuses = new ConcurrentHashMap<>();
    private final AtomicInteger activeSensors = new AtomicInteger(0);


    //constarctor
    public StatisticalFolder() {
        this.systemRuntime = new AtomicInteger(0);
        this.numDetectedObjects = new AtomicInteger(0);
        this.numTrackedObjects = new AtomicInteger(0);
        this.numLandmarks = new AtomicInteger(0);
        this.camerasLastFrame = new ConcurrentHashMap<>();
        this.lidarsLastFrame = new ConcurrentHashMap<>();
        this.poses = new ArrayList<>();

    }



    public int getSystemRuntime() {
        return systemRuntime.get();
    }

    public int getNumDetectedObjects() {
        return numDetectedObjects.get();
    }

    public int getNumTrackedObjects() {
        return numTrackedObjects.get();
    }

    public int getNumLandmarks() {
        return numLandmarks.get();
    }

    // setters
    public void incrementRuntime(int ticks) {
        systemRuntime.addAndGet(ticks);
    }


    public void incrementDetectedObjects(int count) {
        numDetectedObjects.addAndGet(count);
    }

    public void incrementTrackedObjects(int count) {
        numTrackedObjects.addAndGet(count);
    }

    public void incrementLandmarks(int count) {
        numLandmarks.addAndGet(count);
    }

    public String toString() {
        return "SystemRuntime: " + getSystemRuntime() + "\n" +
               "DetectedObjects: " + getNumDetectedObjects() + "\n" +
               "TrackedObjects: " + getNumTrackedObjects() + "\n" + 
               "Landmarks: " + getNumLandmarks();
    }

    public void updateCameraLastFrame(Camera camera, StampedDetectedObjects objects){
        if(camerasLastFrame.containsKey(camera)){
            camerasLastFrame.remove(camera);
       }
        camerasLastFrame.put(camera, objects);
    }

    public ConcurrentHashMap<Camera, StampedDetectedObjects> getCamerasLastFrame(){
        return camerasLastFrame;
    }


    public void updateLidarLastFrame (LiDarWorkerTracker lidar, TrackedObject trackedObject){
        if(lidarsLastFrame.containsKey(lidar)){
            lidarsLastFrame.remove(lidar);
        }
        lidarsLastFrame.put(lidar, trackedObject);
    }

    public ConcurrentHashMap<LiDarWorkerTracker, TrackedObject> getLidarsLastFrame(){
        return lidarsLastFrame;
    }

    public void updatePoses (Pose pose){
        poses.add(pose);
    }

    public ArrayList<Pose> getPoses(){
        return poses;
    }


    //the next 3 methods are to shut doen if all sensors are DOWN
    public synchronized void registerSensor(String sensorId) {
        sensorStatuses.put(sensorId, STATUS.UP);
        activeSensors.incrementAndGet();
    }

    public synchronized void updateSensorStatus(String sensorId, STATUS status) {
        STATUS oldStatus = sensorStatuses.get(sensorId);
        if (oldStatus == STATUS.UP && status == STATUS.DOWN) {
            activeSensors.decrementAndGet();
        }
        sensorStatuses.put(sensorId, status);
    }

    public synchronized boolean areAllSensorsDown() {
        return activeSensors.get() == 0;
    } 
}