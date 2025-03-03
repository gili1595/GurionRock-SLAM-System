package bgu.spl.mics.application.objects;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;


public class CameraTest {
    private Camera camera;
    private StatisticalFolder stats;


    @BeforeEach
    public void setUp() {
        System.out.println("Setting up test environment for Camera");
        stats = new StatisticalFolder();
        camera = new Camera(1, 2, stats); 
    }

     @Test
    public void testProcessDetectionsForTick() {
        System.out.println("Starting testProcessDetectionsForTick...");
       
        ArrayList<DetectedObject> objects = new ArrayList<>();
        objects.add(new DetectedObject("obj1", "test object 1"));
        objects.add(new DetectedObject("obj2", "test object 2"));
        StampedDetectedObjects detections = new StampedDetectedObjects(1, objects);

        // Add detections to the camera
        camera.getDetectedObjectsList().add(detections);
        camera.initializeTotalDetections();

        //check if camera's detectedObjectsList is null or empty
        assertNotNull(camera.getDetectedObjectsList(), "DetectedObjectsList should not be null");
        assertFalse(camera.getDetectedObjectsList().isEmpty(), "DetectedObjectsList should not be empty");

        //first tick
        StampedDetectedObjects result1 = camera.processDetectionsForTick(1);
        assertNull(result1, "Should return null for tick 1 (too early)");

        //second tick
        StampedDetectedObjects result2 = camera.processDetectionsForTick(2);
        assertNull(result2, "Should return null for tick 2 (too early)");

        //third tick
        StampedDetectedObjects processed = camera.processDetectionsForTick(3);
        assertNotNull(processed, "Should return valid detections for tick 3");

        //validations
        assertEquals(2, processed.getDetectedObjects().size(), "Should return 2 detected objects");
        assertEquals("obj1", processed.getDetectedObjects().get(0).getId(), "First detected object ID should match");
    }


    @Test //test the handling of error in the detection (isValid method)
    public void testErrorDetection() {
        System.out.println("Starting testErrorDetection...");

        ArrayList<DetectedObject> objects = new ArrayList<>();
        objects.add(new DetectedObject("ERROR", "Test Error"));
        
        StampedDetectedObjects detections = new StampedDetectedObjects(1, objects);
        DetectedObject errorObj = camera.isValid(detections);
        
        //validations
        assertNotNull(errorObj);
        assertEquals("ERROR", errorObj.getId());
        assertEquals("Test Error", errorObj.getDescription());
    }

    @Test //tesing the move to DOWN status
    public void testStatusTransition() {
        System.out.println("Starting testStatusTransition...");

        ArrayList<DetectedObject> objects = new ArrayList<>();
        objects.add(new DetectedObject("obj1", "test object"));
        StampedDetectedObjects detections = new StampedDetectedObjects(1, objects);

        camera.getDetectedObjectsList().add(detections);
        camera.initializeTotalDetections();
            
        assertEquals(STATUS.UP, camera.getStatus());
        camera.processDetectionsForTick(3); // time + frequency
        assertEquals(STATUS.DOWN, camera.getStatus());
    }
}