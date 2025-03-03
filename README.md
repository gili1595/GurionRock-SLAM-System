# GurionRock SLAM System

## Overview
This project implements a perception and mapping system for the fictional GurionRock Pro Max Ultra Over 9000 vacuum robot. It demonstrates a microservice-based architecture for processing sensor data using a custom message bus framework.

The system simulates a robot moving through its environment, using cameras and LiDAR sensors to detect objects, track them spatially, and build a map of landmarks relative to a global coordinate system (the charging station).

## Architecture

### Message Bus Framework
The core of the application is a custom microservice framework with a message bus for inter-service communication. Key components include:

- **MessageBus**: A thread-safe singleton that allows services to exchange messages
- **MicroService**: Abstract base class that all services extend to interact with the message bus
- **Future\<T\>**: Implements promise-based asynchronous operations

### Main Components

1. **TimeService**: Acts as the global system timer
2. **CameraService**: Detects objects in the environment 
3. **LiDarService**: Tracks objects with precise spatial coordinates
4. **PoseService**: Provides the robot's position and orientation
5. **FusionSlamService**: Integrates all data to build and update a global map

### Message Flow
- Camera detects objects → LiDAR workers track them with precise coordinates → Fusion-SLAM transforms coordinates to the global frame and updates the map

## Features

- Concurrent processing of sensor data using Java threads
- Thread-safe message passing between services
- Round-robin work distribution for load balancing
- Future-based asynchronous operations
- Coordinate transformation for global mapping
- Error handling and graceful termination

## Input and Output

### Input
The system takes JSON configuration files that define:
- Camera and LiDAR sensor configurations
- Simulated pose data
- Detected objects and cloud point data

### Output
The system generates a JSON file containing:
- A map of landmarks with their global coordinates
- System statistics (runtime, detected objects, etc.)
- Diagnostic information in case of errors

## Technical Implementation

- Thread-safe singleton for the message bus
- Proper synchronization to prevent race conditions
- Blocking queue implementation for message delivery
- Event-driven architecture with callbacks
- Coordinate system transformations for mapping

## Building and Running

### Prerequisites
- Java 8 or higher
- Maven

### Build
```bash
mvn clean install
```

### Run
```bash
java -jar target/gurionrock-slam-1.0.jar <path-to-config-file>
```

## Design Patterns Used

- Singleton Pattern (MessageBus)
- Observer Pattern (Publish/Subscribe messaging)
- Future Pattern (Asynchronous operations)
- Callback Pattern (Event handling)
- Message Loop Pattern (Service execution)
