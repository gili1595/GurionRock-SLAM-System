package bgu.spl.mics.application;
import bgu.spl.mics.application.objects.*;
import bgu.spl.mics.application.services.*;
import com.google.gson.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
/**
 * The main entry point for the GurionRock Pro Max Ultra Over 9000 simulation.
 * <p>
 * This class initializes the system and starts the simulation by setting up
 * services, objects, and configurations.
 * </p>
 */
public class GurionRockRunner {

    /**
     * The main method of the simulation.
     * This method sets up the necessary components, parses configuration files,
     * initializes services, and starts the simulation.
     *
     * @param args Command-line arguments. The first argument is expected to be the path to the configuration file.
     */
    public static void main(String[] args) {
        System.out.println("Hello World!");
      


        if (args.length < 1) {
            System.err.println("Please provide the path to the configuration file.");
            return;
        }

        String configPath = args[0];
            System.out.println("Attempting to read configuration from: " + configPath);
            File configFile = new File(configPath);
            System.out.println("File exists: " + configFile.exists());
            System.out.println("File can read: " + configFile.canRead());
            System.out.println("File absolute path: " + configFile.getAbsolutePath());

        try {
            // Step 1: Parse configuration file
            JsonObject config = parseConfigFile(configPath);
            ArrayList<Thread> serviceThreads = new ArrayList<>();
            Thread timeServiceThread = null;
    
            // Create a CountDownLatch with count equal to number of services
            int numberOfServices = 4; // Camera, LiDar, Pose, and FusionSlam services
            CountDownLatch servicesLatch = new CountDownLatch(numberOfServices);
    
            // Step 2: Initialize objects and services
            StatisticalFolder stats = new StatisticalFolder();
            List<Camera> cameras = initializeCameras(configPath, config, stats);
            List<LiDarWorkerTracker> lidarTrackers = initializeLiDars(configPath, config, stats);
            GPSIMU gpsimu = initializeGPSIMU(configPath, config, stats);
    
            // Create shared objects
            FusionSlam.initialize(stats, configFile.getParent());
            FusionSlam fusionSlam = FusionSlam.getInstance();
    
            // Initialize all services except TimeService with the latch
            initializeCameraServices(cameras, stats, serviceThreads, servicesLatch);
            initializeLiDarServices(lidarTrackers, stats, serviceThreads, servicesLatch);
            initializePoseService(gpsimu, serviceThreads, servicesLatch);
            initializeFusionSlamService(fusionSlam, serviceThreads, servicesLatch);
    
            // Initialize TimeService separately
            int tickTime = config.get("TickTime").getAsInt();
            int duration = config.get("Duration").getAsInt();
            TimeService timeService = new TimeService(tickTime, duration);
            timeServiceThread = new Thread(timeService);
    
            // Step 3: Start all services except TimeService
            for (Thread thread : serviceThreads) {
                thread.start();
            }
    
            // Wait for all services to signal they're ready
            servicesLatch.await();
            System.out.println("All services initialized. Starting TimeService...");
            
            // Start TimeService last
            timeServiceThread.start();
    
            // Wait for all services to finish
        //     for (Thread thread : serviceThreads) {
        //         thread.join();
        //     }
        //     timeServiceThread.join();
    
        // } catch (Exception e) {
        //     System.err.println("Failed to run simulation: " + e.getMessage());
        //     e.printStackTrace();
        // }

        // Wait for all services to finish with timeout
            long timeout = 5000; // 5 seconds timeout after termination broadcast
            long startTime = System.currentTimeMillis();
            
            for (Thread thread : serviceThreads) {
                System.out.println("Waiting for thread " + thread.getName() + " to finish...");
                thread.join(timeout);
                if (thread.isAlive()) {
                    System.out.println("WARNING: Thread " + thread.getName() + " did not terminate properly");
                    // Print thread stack trace
                    StackTraceElement[] stack = thread.getStackTrace();
                    System.out.println("Stack trace for " + thread.getName() + ":");
                    for (StackTraceElement element : stack) {
                        System.out.println("\tat " + element);
                    }
                    // Optionally, interrupt the thread
                    thread.interrupt();
                }
            }
            
            System.out.println("Waiting for TimeService thread to finish...");
            timeServiceThread.join(timeout);
            if (timeServiceThread.isAlive()) {
                System.out.println("WARNING: TimeService thread did not terminate properly");
                timeServiceThread.interrupt();
            }

            long endTime = System.currentTimeMillis();
            System.out.println("All threads completed or timed out after " + (endTime - startTime) + "ms");
            System.out.println("System terminating...");

        } catch (Exception e) {
            System.err.println("Failed to run simulation: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Force exit if threads are still hanging
            System.exit(0);

        }
    }

    private static JsonObject parseConfigFile(String filePath) throws IOException {
        System.out.println("Reading configuration file: " + filePath);
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            String jsonContent = content.toString();
            System.out.println("File contents: " + jsonContent);

            JsonElement jsonElement = JsonParser.parseString(jsonContent);
            if (!jsonElement.isJsonObject()) {
                throw new IOException("Root element is not a JSON object");
            }
            return jsonElement.getAsJsonObject();
        } catch (Exception e) {
            System.err.println("Error reading file at: " + filePath);
            System.err.println("Absolute path: " + new File(filePath).getAbsolutePath());
            throw e;
        }
    }

    private static List<Camera> initializeCameras(String configPath, JsonObject config , StatisticalFolder stats) throws IOException {
        List<Camera> cameras = new ArrayList<>();
        try {
            // Get Cameras object
            JsonObject camerasConfig = config.getAsJsonObject("Cameras");
            if (camerasConfig == null) {
                System.err.println("Warning: No 'Cameras' configuration found");
                return cameras;
            }
    
            // Get camera data path
            String cameraDataPath = camerasConfig.get("camera_datas_path").getAsString();
            File configFile = new File(configPath);
            File dataFile = new File(configFile.getParent(), cameraDataPath);
            
            System.out.println("Loading camera data from: " + dataFile.getAbsolutePath());
            
            // Get camera configurations
            JsonArray cameraConfigs = camerasConfig.getAsJsonArray("CamerasConfigurations");
            if (cameraConfigs == null || cameraConfigs.size() == 0) {
                System.err.println("Warning: No camera configurations found");
                return cameras;
            }
    
            // Load camera data once
            JsonObject cameraData = loadCameraData(dataFile.getPath());
            
            // Initialize each camera
            for (JsonElement element : cameraConfigs) {
                JsonObject cameraConfig = element.getAsJsonObject();
                int id = cameraConfig.get("id").getAsInt();
                int frequency = cameraConfig.get("frequency").getAsInt();
                String cameraKey = cameraConfig.get("camera_key").getAsString();
                
                Camera camera = new Camera(id, frequency, stats);
                
                // Get data for this specific camera
                if (cameraData.has(cameraKey)) {
                    JsonArray cameraEvents = cameraData.getAsJsonArray(cameraKey);
                    List<StampedDetectedObjects> detectedObjects = parseCameraEvents(cameraEvents);
                    camera.getDetectedObjectsList().addAll(detectedObjects);
                    System.out.println("Successfully initialized camera " + id);
                } else {
                    System.err.println("No data found for camera key: " + cameraKey);
                }
                
                cameras.add(camera);
            }
    
            return cameras;
        } catch (Exception e) {
            System.err.println("Error initializing cameras: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
            
        
    private static List<LiDarWorkerTracker> initializeLiDars(String configPath, JsonObject config, StatisticalFolder stats) {
        List<LiDarWorkerTracker> lidarTrackers = new ArrayList<>();
        try {
            JsonObject lidarsConfig = config.getAsJsonObject("LiDarWorkers");
            if (lidarsConfig == null) {
                System.err.println("Warning: No 'LiDarWorkers' configuration found");
                return lidarTrackers;
            }
    
            String lidarsDataPath = lidarsConfig.get("lidars_data_path").getAsString();
            File configFile = new File(configPath);
            File dataFile = new File(configFile.getParent(), lidarsDataPath);
            
            System.out.println("Looking for LiDAR data at: " + dataFile.getAbsolutePath());
            
            JsonArray lidarConfigs = lidarsConfig.getAsJsonArray("LidarConfigurations");
            for (JsonElement element : lidarConfigs) {
                JsonObject lidarConfig = element.getAsJsonObject();
                int id = lidarConfig.get("id").getAsInt();
                int frequency = lidarConfig.get("frequency").getAsInt();
                lidarTrackers.add(new LiDarWorkerTracker(id, frequency, dataFile.getAbsolutePath(), stats));
                System.out.println("Successfully initialized LiDAR worker " + id);
            }
    
            return lidarTrackers;
        } catch (Exception e) {
            System.err.println("Error initializing LiDAR workers: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize LiDAR workers", e);
        }
    }
        
            private static GPSIMU initializeGPSIMU(String configPath, JsonObject config, StatisticalFolder stats) throws IOException {
                // Get the pose data file path and resolve it relative to config file location
                String poseJsonFile = config.get("poseJsonFile").getAsString();
                File configFile = new File(configPath);
                File poseFile = new File(configFile.getParent(), poseJsonFile);
                
                System.out.println("Looking for pose data at: " + poseFile.getAbsolutePath());
                if (!poseFile.exists()) {
                    throw new FileNotFoundException("Pose data file not found: " + poseFile.getAbsolutePath());
                }
            
                List<Pose> poses = loadPoseData(poseFile.getAbsolutePath());
                return new GPSIMU(0, new ArrayList<>(poses), stats);
            }
        
            private static JsonObject loadCameraData(String filePath) throws IOException {
                try (Reader reader = new FileReader(filePath)) {
                    return JsonParser.parseReader(reader).getAsJsonObject();
                }
            }

            private static List<StampedDetectedObjects> parseCameraEvents(JsonArray events) {
                List<StampedDetectedObjects> result = new ArrayList<>();
                for (JsonElement event : events) {
                    JsonObject eventObj = event.getAsJsonObject();
                    int time = eventObj.get("time").getAsInt();
                    ArrayList<DetectedObject> detectedObjects = new ArrayList<>();
                    
                    JsonArray objectsArray = eventObj.getAsJsonArray("detectedObjects");
                    for (JsonElement objElement : objectsArray) {
                        JsonObject detected = objElement.getAsJsonObject();
                        String id = detected.get("id").getAsString();
                        String description = detected.get("description").getAsString();
                        detectedObjects.add(new DetectedObject(id, description));
                    }
                    
                    result.add(new StampedDetectedObjects(time, detectedObjects));
                }
                return result;
            }
        
            private static List<Pose> loadPoseData(String filePath) throws IOException {
                try (Reader reader = new FileReader(filePath)) {
                    JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
                    List<Pose> poses = new ArrayList<>();
                    
                    for (JsonElement element : array) {
                        JsonObject obj = element.getAsJsonObject();
                        int time = obj.get("time").getAsInt();
                        float x = obj.get("x").getAsFloat();
                        float y = obj.get("y").getAsFloat();
                        float yaw = obj.get("yaw").getAsFloat();
                        poses.add(new Pose(x, y, yaw, time));
                    }
                    System.out.println("Successfully loaded " + poses.size() + " poses from " + filePath);
                    return poses;
                }
            }

            // private static void initializeCameraServices(List<Camera> cameras, StatisticalFolder stats, 
            //                                ArrayList<Thread> threads, CountDownLatch latch) {
            //     for (Camera camera : cameras) {
            //         CameraService service = new CameraService(camera, stats);
            //         threads.add(new Thread(new Runnable() {
            //             @Override
            //             public void run() {
            //                 latch.countDown();
            //                 service.run();
            //             }
            //         }));
            //     }
            // }
            private static void initializeCameraServices(List<Camera> cameras, StatisticalFolder stats, 
                                ArrayList<Thread> threads, CountDownLatch latch) {
                for (Camera camera : cameras) {
                    CameraService service = new CameraService(camera, stats);
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            latch.countDown();
                            service.run();
                        }
                    }, "CameraService-" + camera.getID());  // Named thread
                    threads.add(thread);
                }
            }

            // private static void initializeLiDarServices(List<LiDarWorkerTracker> trackers, StatisticalFolder stats,
            //                               ArrayList<Thread> threads, CountDownLatch latch) {
            //     for (LiDarWorkerTracker tracker : trackers) {
            //         LiDarService service = new LiDarService(tracker, stats);
            //         threads.add(new Thread(new Runnable() {
            //             @Override
            //             public void run() {
            //                 latch.countDown();
            //                 service.run();
            //             }
            //         }));
            //     }
            // }

            private static void initializeLiDarServices(List<LiDarWorkerTracker> trackers, StatisticalFolder stats,
                              ArrayList<Thread> threads, CountDownLatch latch) {
                for (LiDarWorkerTracker tracker : trackers) {
                    LiDarService service = new LiDarService(tracker, stats);
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            latch.countDown();
                            service.run();
                        }
                    }, "LiDarService-" + tracker.getID());  // Named thread
                    threads.add(thread);
                }
            }

            // private static void initializePoseService(GPSIMU gpsimu, ArrayList<Thread> threads, CountDownLatch latch) {
            //     PoseService service = new PoseService(gpsimu);
            //     threads.add(new Thread(new Runnable() {
            //         @Override
            //         public void run() {
            //             latch.countDown();
            //             service.run();
            //         }
            //     }));
            // }

            private static void initializePoseService(GPSIMU gpsimu, ArrayList<Thread> threads, CountDownLatch latch) {
                PoseService service = new PoseService(gpsimu);
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        latch.countDown();
                        service.run();
                    }
                }, "PoseService");  // Named thread
                threads.add(thread);
            }
        
            // private static void initializeFusionSlamService(FusionSlam fusionSlam, ArrayList<Thread> threads, 
            //                                   CountDownLatch latch) {
            //     FusionSlamService service = new FusionSlamService(fusionSlam);
            //     threads.add(new Thread(new Runnable() {
            //         @Override
            //         public void run() {
            //             latch.countDown();
            //             service.run();
            //         }
            //     }));
            // }

            private static void initializeFusionSlamService(FusionSlam fusionSlam, ArrayList<Thread> threads, 
                                  CountDownLatch latch) {
                FusionSlamService service = new FusionSlamService(fusionSlam);
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        latch.countDown();
                        service.run();
                    }
                }, "FusionSlamService");  // Named thread
                threads.add(thread);
            }
        }


