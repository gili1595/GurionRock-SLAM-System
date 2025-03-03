// FusionSlamTest.java

package bgu.spl.mics.application.objects;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;

public class FusionSlamTest {
    private FusionSlam fusionSlam;
    private StatisticalFolder stats;

    @BeforeEach
    public void setUp() {
        System.out.println("Setting up test environment for FusionSlam");
        try {
            stats = new StatisticalFolder();
            FusionSlam.initialize(stats, "test_output.json");
            fusionSlam = FusionSlam.getInstance();
        } catch (IllegalStateException e) {
            // If already initialized, just get the instance
            fusionSlam = FusionSlam.getInstance();
        }
    }

    @Test
    public void testTransformToGlobal() {
        System.out.println("Starting testTransformToGlobal...");
        
        // Create test data
        ArrayList<CloudPoint> localPoints = new ArrayList<>();
        localPoints.add(new CloudPoint(1.0, 1.0));
        localPoints.add(new CloudPoint(2.0, 2.0));

        LandMark localLandmark = new LandMark("test1", "test landmark", localPoints);

        Pose robotPose = new Pose(5.0f, 5.0f, 90.0f, 1);

        // Transform to global coordinates
        LandMark globalLandmark = fusionSlam.transformToGlobal(localLandmark, robotPose);

        // Get first point after transformation
        CloudPoint transformedPoint = globalLandmark.getCoordinates().get(0);
        
        // With 90 degree rotation and translation:
        double expectedX = -1.0 + 5.0; // -y + x_robot
        double expectedY = 1.0 + 5.0;  // x + y_robot
        double epsilon = 0.0001;

        // Allow for some floating point imprecision
        assertEquals(expectedX, transformedPoint.getCloudPointX(), epsilon);
        assertEquals(expectedY, transformedPoint.getCloudPointY(), epsilon);
    }

    @Test
    public void testTransformWithMultiPoints() {
        System.out.println("Starting testTransformWithMultiPoints...");
        ArrayList<CloudPoint> localPoints = new ArrayList<>();
        localPoints.add(new CloudPoint(0.0, 1.0));
        localPoints.add(new CloudPoint(-1.0, 0.0));
        
        LandMark localLandmark = new LandMark("test2", "multi-point test", localPoints);
        Pose robotPose = new Pose(0.0f, 0.0f, 45.0f, 1);
        
        LandMark globalLandmark = fusionSlam.transformToGlobal(localLandmark, robotPose);
        
        // Verify multiple points transformed correctly
        double sqrt2 = Math.sqrt(2);
        double epsilon = 0.0001;
        
        assertEquals(-1/sqrt2, globalLandmark.getCoordinates().get(0).getCloudPointX(), epsilon);
        assertEquals(1/sqrt2, globalLandmark.getCoordinates().get(0).getCloudPointY(), epsilon);
    }
}
