# 🤖 Robot Operator – 3D Construction Site Viewer (v2)

An intuitive Android app that lets you visualize construction rooms in 3D, mark work areas on walls, and place a robot in the scene — built with **Kotlin, OpenGL ES, and Jetpack Compose**.

---

## 🚀 Try It Instantly

- 🎮 **Live Browser Demo**  
  https://appetize.io/app/b_zjzbzpuon35dwzkjay7zhjgvi4

- 📱 **Download APK (v2)**  
  https://github.com/adit9852/OpenGL_Final/blob/main/Apk/openGL_V2.apk  
  _Click **“View raw”** to download._

- 🎥 **Version 2 Demo (YouTube Short)**  
  https://youtube.com/shorts/-3sKSWeaGA0?feature=share

- 🎥 **Older Full-Length Demo (v1)**  
  https://youtu.be/Nq4MBZ0mCZk?si=XCak05pDgomhHSyk

---

## ✨ What You Can Do

- 🧱 Explore a 3D construction room with smooth camera controls  
- 🎯 Mark walls with color-coded annotations:
  - 🔴 Spray Area
  - 🟡 Sand Area
  - 🟠 Obstacle
- 🤖 Place a robot on the floor using tap-to-place ray casting  
- 🧱 Switch wall rendering between **Flat**, **Mesh**, and **Wireframe**  
- 🧭 See if the camera is **inside** or **outside** the room in real-time  
- 💾 Auto-save all annotations and robot position using **Room DB**

---

## 📱 Controls

### 🎮 Camera

- **One-finger drag** → Rotate camera around the room  
- **Two-finger drag** → Pan camera (move left/right/up/down)  
- **Pinch** → Zoom in/out  

### 🧱 Annotations

1. Tap **“Annotations”** from the top bar  
2. Select annotation type: _Spray_, _Sand_, or _Obstacle_  
3. Select the wall  
4. A colored rectangle appears on that wall  
5. Tap an annotation to view details or delete it  

### 🤖 Robot Placement

1. Tap **“Place Robot”** to enter placement mode  
2. Tap on the floor to drop the robot cube  
3. Tap **“Clear”** to remove the robot  

### 🧱 Wall Modes

- **Flat** → Solid-colored walls (default)  
- **Mesh** → Grid-style walls for depth perception  
- **Wireframe** → See-through wire grid  

---

## 🏗️ Architecture Overview

The project follows **MVVM + Clean Architecture**.

```mermaid
graph TB
    subgraph UI_Layer
        A[RoomViewerFragment<br/>Jetpack Compose UI]
        B[RoomViewerViewModel<br/>State Management]
    end

    subgraph Domain_Layer
        C[AnnotationRepo<br/>Annotation Logic]
        D[RobotRepo<br/>Robot Logic]
    end

    subgraph Data_Layer
        E[AppDatabase<br/>Room DB]
        F[AnnotationEntity]
        G[RobotEntity]
    end

    subgraph Rendering_Layer
        H[RoomRenderer<br/>OpenGL ES]
        I[Camera<br/>3D Controls]
        J[Room<br/>Room Geometry]
        K[RobotCube<br/>Robot Model]
        L[AnnotationOverlay<br/>Wall Markers]
        M[TextRenderer<br/>Labels]
    end

    A --> B
    B --> C
    B --> D
    C --> E
    D --> E
    E --> F
    E --> G

    A --> H
    H --> I
    H --> J
    H --> K
    H --> L
    H --> M

🔄 Application Flow
flowchart TD
    Start([App Launch])
    Init[Init DB & OpenGL]
    Load[Load Saved Annotations & Robot]
    Render[Render 3D Room]
    Wait{User Action?}

    Start --> Init --> Load --> Render --> Wait

    Wait -->|Move Camera| Cam[Update Camera]
    Cam --> Check{Inside Room?}
    Check -->|Yes| In[Show Inside Indicator]
    Check -->|No| Out[Show Outside Indicator]
    In --> Render
    Out --> Render

    Wait -->|Add Annotation| A1[Select Type & Wall]
    A1 --> A2[Compute Wall Position]
    A2 --> A3[Save Annotation to DB]
    A3 --> A4[Render Annotation]
    A4 --> Render

    Wait -->|Place Robot| R1[Enter Placement Mode]
    R1 --> R2{Tap on Floor?}
    R2 -->|Yes| R3[Raycast to Floor]
    R3 --> R4[Save Robot Position]
    R4 --> R5[Render Robot]
    R5 --> Render

    Wait -->|Switch Wall Mode| W1[Update Room Geometry]


    📁 Project Structure
app/src/main/java/com/example/a10x_assign/
│
├── data/                         # Room database & entities
│   ├── AnnotationEntity.kt       # Annotation table
│   ├── Annotations.kt            # Annotation DAO
│   ├── RobotEntity.kt            # Robot table
│   ├── Robot.kt                  # Robot DAO
│   └── AppDatabase.kt            # Room database setup
│
├── di/                           # Dependency Injection
│   └── AppModule.kt              # Hilt modules
│
├── opengl/                       # 3D Rendering Engine
│   ├── Camera.kt                 # Camera controls & inside/outside logic
│   ├── Room.kt                   # Room geometry (flat/mesh/wireframe)
│   ├── RobotCube.kt              # Robot 3D model (cube)
│   ├── AnnotationOverlay.kt      # Wall annotation rendering
│   ├── TextRenderer.kt           # Labels for walls & annotations
│   ├── RoomRenderer.kt           # Main OpenGL renderer
│   └── RoomSurfaceView.kt        # GLSurfaceView + touch handling
│
├── repository/                   # Business Logic Layer
│   ├── AnnotationRepo.kt         # Annotation operations
│   └── RobotRepo.kt              # Robot operations
│
└── ui/roomviewer/                # User Interface
    ├── RoomViewerFragment.kt     # Hosts Compose UI + GLSurfaceView
    └── RoomViewerViewModel.kt    # State management & events

🛠️ Tech Stack
Category	Technologies
Language	Kotlin
UI	Jetpack Compose (Material 3)
3D Rendering	OpenGL ES 2.0
Architecture	MVVM + Clean Architecture
Dependency Inject	Hilt / Dagger
Database	Room (SQLite)
Async	Kotlin Coroutines + Flow
Build System	Gradle
💡 Key Technical Highlights

🧩 Hybrid UI:
OpenGL GLSurfaceView for 3D + Jetpack Compose for modern controls.

🎥 Smart Camera System:
Camera orbits, pans, zooms, and detects if you are inside or outside the room.

🔀 Thread-Safe Rendering:

OpenGL operations strictly on the GL thread

UI & ViewModel logic on the main thread

Shared flags for wall mode / annotations / robot placement.

🎯 Ray-Casting for Placement:
Converts 2D tap → 3D ray → floor intersection to place the robot accurately.

🧱 Dynamic Geometry:
Regenerates vertex data for Flat / Mesh / Wireframe room walls without recreating the GL context.

💾 Persistent State Management:
Annotations and robot position are stored in Room DB and automatically restored on app relaunch.

🎨 Color Coding

Spray Area → 🔴 Red

Sand Area → 🟡 Yellow

Obstacle → 🟠 Orange

Inside Room → 🟢 Green indicator

Outside Room → 🔴 Red indicator

🐛 Known Limitations

Robot uses a simple cube representation instead of a full UR10e model

Annotations cannot be resized or dragged (only deleted)

No undo/redo history for actions

🚀 Future Roadmap & Recommendations

 Full UR10e robot model with articulated joints

 Drag-to-move and resize wall annotations

 Export room configuration to JSON

 Multiple room layouts with different dimensions

 AR mode using ARCore

 Path planning visualization for robot movement

 Low-performance mode using RENDERMODE_WHEN_DIRTY

🎯 Quick Start (Developers)
# Clone the repository
git clone https://github.com/adit9852/OpenGL_Final.git

# Build & install debug APK
./gradlew assembleDebug
./gradlew installDebug


Requirements

Android 7.0+ (API 24+)

Android Studio Hedgehog or newer

Device/emulator with OpenGL ES 2.0 support

🔧 Troubleshooting

Blank screen?

Check if your device supports OpenGL ES 2.0

Confirm the minimum Android version is met

Slow performance?

Prefer running on a physical device

Consider changing render mode from continuous to on-demand

Build errors?

./gradlew clean
./gradlew assembleDebug

📄 License

Created as part of an Android development internship assignment.

Made with ❤️ using Kotlin, OpenGL ES, and Jetpack Compose by 𝓐𝓭𝓲𝓽𝔂𝓪
    W1 --> Render

    Wait -->|Delete Annotation / Clear Robot| D1[Update DB]
    D1 --> Render


