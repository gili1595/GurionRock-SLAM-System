package bgu.spl.mics.application.objects;

public class PoseResult {
       // Result class to keep pose processing results
        private final Pose pose;
        private final boolean success;
        private final String message;

        public PoseResult(Pose pose, boolean success, String message) {
            this.pose = pose;
            this.success = success;
            this.message = message;
        }

        public Pose getPose() { return pose; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }  
