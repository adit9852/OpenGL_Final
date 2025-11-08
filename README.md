# Robot Operator Application

An Android application for visualizing and annotating construction site rooms with robot placement capabilities. Built with OpenGL ES, Jetpack Compose, and modern Android architecture.

## Features

### Core Features
- **3D Room Visualization**: Interactive 3D rectangular room with proper perspective, lighting, and camera controls
- **Camera Controls**:
  - Single finger drag to rotate camera
  - Two-finger drag to pan
  - Pinch to zoom in/out
- **Annotation System**: Mark areas on walls with different annotation types:
  - Spray Area
  - Sand Area
  - Obstacle
- **Robot Placement**: Place and visualize a robot (UR10e representation) in the room
- **Data Persistence**: All annotations and robot positions are saved to local database

### Technical Implementation
- **Architecture**: MVVM with Clean Architecture principles
- **Dependency Injection**: Hilt/Dagger for dependency management
- **UI**: Jetpack Compose for 2D UI elements overlaid on OpenGL ES rendering
- **3D Rendering**: OpenGL ES 2.0 for room and robot visualization
- **Database**: Room database for persistent storage
- **Async Operations**: Kotlin Coroutines and Flow for reactive data

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 24 or higher
- Gradle 8.13
- JDK 11

### Building the Project

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle dependencies
4. Build the project:
   ```bash
   ./gradlew assembleDebug
   ```
5. Run on device or emulator:
   ```bash
   ./gradlew installDebug
   ```

## Libraries and Tools Used

### Core Android
- **androidx.core:core-ktx** - Kotlin extensions
- **androidx.lifecycle:lifecycle-viewmodel-ktx** - ViewModel with Kotlin extensions
- **androidx.fragment:fragment-ktx** - Fragment KTX extensions

### UI
- **Jetpack Compose BOM** - Modern declarative UI framework
  - Material3 - Material Design 3 components
  - UI Tooling - Preview and debugging tools
- **androidx.appcompat** - Legacy UI support for GLSurfaceView

### Dependency Injection
- **Hilt 2.50** - Dependency injection framework
  - hilt-android - Android integration
  - hilt-compiler - Annotation processor

### Database
- **Room 2.6.1** - SQLite object mapping library
  - room-runtime - Core runtime
  - room-ktx - Kotlin extensions with coroutines support
  - room-compiler - Annotation processor

### Async & Reactive
- **Kotlin Coroutines** - Asynchronous programming
- **Flow** - Reactive streams

### 3D Rendering
- **OpenGL ES 2.0** - Native Android 3D graphics API

## Technical Decisions

### 1. OpenGL Context Initialization
**Problem**: The original blank screen was caused by OpenGL resources being initialized before the GL context was created.

**Solution**: Implemented lazy initialization pattern where shaders and buffers are created in `onSurfaceCreated()` callback after the OpenGL context is ready.

```kotlin
class Room {
    fun initialize() {
        if (isInitialized) return
        // Initialize buffers and compile shaders here
    }
}
```

### 2. Compose + OpenGL Integration
**Challenge**: Combining Jetpack Compose with OpenGL rendering.

**Solution**: Used `FrameLayout` to layer a transparent ComposeView over GLSurfaceView, allowing touch events to pass through to the GL view while Compose handles UI interactions.

### 3. Annotation Storage Design
**Decision**: Store annotations with normalized coordinates (0-1) rather than absolute world coordinates.

**Rationale**:
- Makes annotations resolution-independent
- Easier to scale to different room sizes
- Simplifies rendering calculations

### 4. Single Activity Architecture
**Implementation**: MainActivity hosts a single RoomViewerFragment, following modern Android navigation patterns.

**Benefits**:
- Simplified navigation
- Easier state management
- Better performance (no activity recreation overhead)

### 5. Robot Representation
**Decision**: Simplified robot as an orange cube instead of full URDF parsing.

**Rationale**:
- URDF parsing requires additional libraries (ROS integration)
- Cube provides clear visual representation
- Focuses on core functionality first
- Can be enhanced later with proper URDF loader

### 6. Database Schema
**Design**: Separate tables for Annotations and Robots with type converters for enums.

```kotlin
@Entity(tableName = "annotations")
data class AnnotationEntity(
    val type: AnnotationType,
    val wallType: WallType,
    val x: Float, val y: Float,
    val width: Float, val height: Float
)
```

**Benefits**:
- Clear separation of concerns
- Easy querying by wall or annotation type
- Timestamps for audit trail

### 7. Face Culling Configuration
**Problem**: Walls were disappearing when viewing from certain camera angles.

**Solution**: Disabled face culling in the renderer since the camera is positioned inside the room looking at the inner faces of walls.

```kotlin
GLES20.glDisable(GLES20.GL_CULL_FACE)  // See walls from inside
```

**Rationale**: With default back-face culling, the inside faces of walls would be culled when visible, causing them to disappear. Disabling culling allows all wall faces to render correctly.

### 8. Annotation Rendering System
**Implementation**: Created dedicated `AnnotationOverlay` class for rendering semi-transparent colored rectangles on walls.

**Key Features**:
- Alpha blending for semi-transparent overlays
- Color-coded by annotation type (red, yellow, orange)
- Proper depth ordering with slight offset to prevent z-fighting
- Wall-specific vertex calculations for all six surfaces

```kotlin
GLES20.glEnable(GLES20.GL_BLEND)
GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
```

## Known Limitations

### 1. Annotation Placement
**Current**: Annotations are added programmatically via UI buttons, not by directly tapping on walls in the 3D view.

**Reason**: Ray-casting for 3D hit detection requires additional math to convert 2D screen touches to 3D world coordinates and determine which wall was touched.

**Workaround**: Annotations can be created with predefined positions for demonstration.

### 2. Robot Model
**Current**: Robot is represented as a simple orange cube.

**Limitation**: Not using the full UR10e URDF model as specified in the assignment.

**Reason**:
- URDF parsing requires ROS libraries or custom parser
- Complex joint hierarchies need inverse kinematics
- Focused on core annotation functionality first

**Future Enhancement**: Implement URDF loader using libraries like `urdf4j` or custom parser.

### 3. Wall Disappearing Issue
**Status**: ✅ RESOLVED

**Previous Issue**: Walls were disappearing when the camera angle changed.

**Fix**: Disabled face culling in `RoomRenderer.kt` since the camera is inside the room viewing inner wall faces.

### 4. Annotation Visual Rendering
**Status**: ✅ RESOLVED

**Previous Issue**: Annotations were not visible on the 3D walls.

**Fix**: Implemented `AnnotationOverlay.kt` class with semi-transparent colored rectangles rendered on wall surfaces using alpha blending.

### 5. Limited Camera Controls
**Current**: Basic rotate, pan, and zoom.

**Missing**:
- Camera position reset
- Preset camera angles (top, front, side views)
- Animation smoothing

### 6. No Undo/Redo
**Current**: No undo mechanism for annotations or robot placement.

**Impact**: Users must manually delete annotations to correct mistakes.

## Project Structure

```
app/src/main/java/com/example/a10x_assign/
├── data/                      # Data layer
│   ├── AnnotationEntity.kt    # Annotation data model
│   ├── Annotations.kt         # Annotation DAO
│   ├── RobotEntity.kt         # Robot data model
│   ├── Robot.kt               # Robot DAO
│   └── AppDatabase.kt         # Room database
├── di/                        # Dependency injection
│   └── AppModule.kt           # Hilt modules
├── opengl/                    # OpenGL rendering
│   ├── Camera.kt              # Camera controls
│   ├── Room.kt                # 3D room model
│   ├── RobotCube.kt           # Robot 3D model
│   ├── AnnotationOverlay.kt   # Annotation rendering
│   ├── RoomRenderer.kt        # GL renderer
│   └── RoomSurfaceView.kt     # GL surface view
├── repository/                # Repository layer
│   ├── AnnotationRepo.kt      # Annotation repository
│   └── RobotRepo.kt           # Robot repository
└── ui/                        # UI layer
    └── roomviewer/
        ├── RoomViewerFragment.kt   # Main fragment
        └── RoomViewerViewModel.kt  # ViewModel
```

## Usage Guide

### Camera Controls
1. **Rotate**: Drag with one finger
2. **Pan**: Drag with two fingers
3. **Zoom**: Pinch to zoom in/out

### Adding Annotations
1. Tap "Add Annotation" button
2. Select annotation type (Spray Area, Sand Area, or Obstacle)
3. A sample annotation appears on the back wall (semi-transparent colored rectangle)
4. View all annotations by tapping "Annotations (N)" in top bar
5. Annotations are color-coded:
   - Spray Area: Red
   - Sand Area: Yellow
   - Obstacle: Orange

### Robot Placement
1. Tap "Place Robot" button
2. Robot appears at center of room floor
3. Tap "Clear" button to remove robot

### Managing Annotations
1. Tap "Annotations" button in top bar
2. View list of all annotations
3. Tap "✕" button to delete individual annotations

## Future Enhancements

### High Priority
1. **3D Touch Interaction**: Implement ray-casting for direct wall selection with precise annotation placement
2. **URDF Support**: Full UR10e robot model with articulated joints
3. **Drag-and-Drop Robot**: Interactive robot placement with ground plane detection
4. **Annotation Resizing**: Allow users to adjust annotation dimensions after placement

### Medium Priority
1. **Export/Import**: Save room configurations as JSON/XML
2. **Multiple Rooms**: Support for different room dimensions
3. **Measurement Tools**: Display dimensions and distances
4. **Path Planning**: Visualize robot movement paths

### Low Priority
1. **AR Mode**: Use ARCore for real-world room scanning
2. **Collaborative Editing**: Multi-user annotation support
3. **Cloud Sync**: Backup annotations to cloud storage
4. **Material Textures**: Realistic wall/floor textures

## Testing

### Manual Testing Checklist
- [x] App launches without crashes
- [x] 3D room renders correctly
- [x] Camera controls work (rotate, pan, zoom)
- [x] Annotations can be created
- [x] Annotations persist after app restart
- [x] Annotations visible on 3D walls with correct colors
- [x] Walls remain visible from all camera angles
- [x] Robot placement works
- [x] Robot clears properly
- [x] UI overlays don't interfere with camera controls

### Known Working Configuration
- **Device**: Android Emulator / Physical Device
- **Android Version**: 7.0 (API 24) and above
- **OpenGL ES**: 2.0 or higher

## Troubleshooting

### Blank Screen Issue
**Fixed**: The OpenGL initialization issue has been resolved through lazy initialization pattern. If you still see a blank screen:
1. Ensure device supports OpenGL ES 2.0
2. Check Logcat for GL errors
3. Verify AndroidManifest.xml has GL feature requirement

### Wall Disappearing Issue
**Fixed**: Face culling has been disabled to allow viewing walls from inside the room. All walls now remain visible regardless of camera angle.

### Build Errors
1. Clean and rebuild: `./gradlew clean assembleDebug`
2. Invalidate caches in Android Studio
3. Update Android Studio to latest version
4. Ensure JDK 11 is being used

### Performance Issues
1. Lower render quality in Camera.kt
2. Reduce complexity of Room mesh
3. Optimize database queries
4. Use RENDERMODE_WHEN_DIRTY instead of RENDERMODE_CONTINUOUSLY for battery saving

## License

This project is created as part of an Android internship assignment.

## Contact

For questions or issues, please create an issue in the repository.
