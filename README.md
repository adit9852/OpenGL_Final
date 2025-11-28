# 🤖 Robot Operator – 3D Construction Site Viewer (v2)

An intuitive **Android app** that lets you visualize construction rooms in 3D, mark work areas on walls, and place a robot in the scene — built with **Kotlin, OpenGL ES, and Jetpack Compose**.

---

## 🚀 Try It Instantly

- 🎮 **Live Interactive Demo (Browser)**  
  👉 [Open on Appetize](https://appetize.io/app/b_zjzbzpuon35dwzkjay7zhjgvi4)

- 📱 **Download APK (v2)**  
  👉 [Download openGL_V2.apk](https://github.com/adit9852/OpenGL_Final/blob/main/Apk/openGL_V2.apk)  
  _Click **“View raw”** to download the APK._

- 🎥 **Version 2 Video Demo**  
  👉 [Watch V2 Demo (YouTube Short)](https://youtube.com/shorts/-3sKSWeaGA0?feature=share)

> Older full-length demo (Version 1):  
> [Watch V1 Demo](https://youtu.be/Nq4MBZ0mCZk?si=XCak05pDgomhHSyk)

---

## ✨ Features

- 🧱 **Explore a 3D Room**  
  Rotate, pan, and zoom around a virtual construction room.

- 🎯 **Mark Work Areas on Walls**  
  Add color-coded annotations like:
  - Spray Area (Red)  
  - Sand Area (Yellow)  
  - Obstacle (Orange)

- 🤖 **Place a Robot in the Scene**  
  Tap on the floor to place a robot cube and visualize its position in the room.

- 🧱 **Switch Wall Rendering Modes**
  - Flat – solid walls  
  - Mesh – grid-style walls  
  - Wireframe – see-through structure

- 🧭 **Inside/Outside Indicator**  
  Real-time indicator shows whether the camera is **inside** or **outside** the room.

- 💾 **Auto-Save State**  
  All annotations and robot position are saved to **Room DB** and restored on next launch.

---

## 📱 Controls

### 🎮 Camera Controls

- **One-finger drag** → Rotate the camera around the room  
- **Two-finger drag** → Pan (move view left/right/up/down)  
- **Pinch** → Zoom in/out  

### 🧱 Add Annotations

1. Tap **“Annotations”** in the top bar  
2. Choose the type:
   - Spray Area  
   - Sand Area  
   - Obstacle  
3. Select the wall  
4. A colored rectangle appears on that wall  
5. Tap an annotation to:
   - View details  
   - Delete it  

### 🤖 Place the Robot

1. Tap **“Place Robot”** (button becomes active)  
2. Tap on the floor to place the robot cube  
3. (If enabled) Drag the robot to reposition  
4. Tap **“Clear”** to remove the robot  

### 🧱 Wall Rendering Modes

- **Flat** – solid walls (default)  
- **Mesh** – grid pattern for better depth perception  
- **Wireframe** – see-through wire grid  

---

## 🏗️ High-Level How It Works

- The app renders a **3D room** using **OpenGL ES 2.0**
- A **Camera system** allows rotation, panning, and zoom
- Touch input is used for:
  - **Annotations**: mapping screen taps to wall coordinates
  - **Robot Placement**: converting 2D tap → 3D ray → floor intersection
- All annotation and robot data is stored using **Room Database**
- On relaunch, data is loaded and the scene is restored automatically

---

## 🧬 Architecture Overview

The project follows **MVVM + Clean Architecture**.

```mermaid
graph TB
    subgraph UI Layer
        A[RoomViewerFragment<br/>Jetpack Compose UI]
        B[RoomViewerViewModel<br/>State Management]
    end

    subgraph Domain Layer
        C[AnnotationRepo<br/>Annotation Logic]
        D[RobotRepo<br/>Robot Logic]
    end

    subgraph Data Layer
        E[AppDatabase<br/>Room DB]
        F[AnnotationEntity]
        G[RobotEntity]
    end

    subgraph Rendering Layer
        H[RoomRenderer<br/>OpenGL ES]
        I[Camera<br/>3D Controls & Position]
        J[Room<br/>Flat/Mesh/Wireframe Geometry]
        K[RobotCube<br/>Robot 3D Model]
        L[AnnotationOverlay<br/>Wall Annotations]
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

    style A fill:#4CAF50
    style B fill:#2196F3
    style C fill:#FF9800
    style D fill:#FF9800
    style E fill:#9C27B0
    style H fill:#F44336


## 🔄 Application Flow

```mermaid
flowchart TD
    Start([App Launch]) --> Init[Initialize Database<br/>& OpenGL Context]
    Init --> Load[Load Saved<br/>Annotations & Robot]
    Load --> Render[Render 3D Room]

    Render --> Wait{User Action?}

    Wait -->|Rotate/Pan/Zoom| Camera[Update Camera<br/>Position]
    Camera --> CheckPos{Inside Room?}
    CheckPos -->|Yes| ShowGreen[Show Green Indicator]
    CheckPos -->|No| ShowRed[Show Red Indicator]
    ShowGreen --> Render
    ShowRed --> Render

    Wait -->|Add Annotation| Ann1[Select Annotation Type]
    Ann1 --> Ann2[Select Wall]
    Ann2 --> Ann3[Calculate Position]
    Ann3 --> Ann4[Save to Database]
    Ann4 --> Ann5[Render on Wall]
    Ann5 --> Render

    Wait -->|Place Robot| Rob1[Enter Placement Mode]
    Rob1 --> Rob2{Tap on Floor?}
    Rob2 -->|Yes| Rob3[Ray-cast to 3D]
    Rob2 -->|Cancel| Render
    Rob3 --> Rob4[Save Position]
    Rob4 --> Rob5[Render Robot Cube]
    Rob5 --> Render

    Wait -->|Switch Wall Mode| Wall1{Select Mode}
    Wall1 -->|Flat| UpdateFlat[Update Geometry<br/>Solid Faces]
    Wall1 -->|Mesh| UpdateMesh[Update Geometry<br/>Grid Pattern]
    Wall1 -->|Wireframe| UpdateWire[Update Geometry<br/>Wire Grid]
    UpdateFlat --> Render
    UpdateMesh --> Render
    UpdateWire --> Render

    Wait -->|Clear Robot| Clear[Delete from DB]
    Clear --> Render

    Wait -->|Delete Annotation| DelAnn[Remove from DB]
    DelAnn --> Render

    style Start fill:#4CAF50
    style Render fill:#2196F3
    style Wait fill:#FF9800
    style CheckPos fill:#9C27B0
    style ShowGreen fill:#00BCD4
    style ShowRed fill:#F44336
```

## 📁 Project Structure

```
app/src/main/java/com/example/a10x_assign/
│
├── 📊 data/                     # Data Models & Database
│   ├── AnnotationEntity.kt      # Annotation data class
│   ├── Annotations.kt           # Annotation DAO
│   ├── RobotEntity.kt           # Robot data class
│   ├── Robot.kt                 # Robot DAO
│   └── AppDatabase.kt           # Room database setup
│
├── 💉 di/                       # Dependency Injection
│   └── AppModule.kt             # Hilt modules
│
├── 🎨 opengl/                   # 3D Rendering Engine
│   ├── Camera.kt                # Camera controls & position tracking
│   ├── Room.kt                  # 3D room geometry (flat/mesh/wireframe)
│   ├── RobotCube.kt             # Robot 3D model
│   ├── AnnotationOverlay.kt     # Wall annotation rendering
│   ├── TextRenderer.kt          # Wall & annotation labels
│   ├── RoomRenderer.kt          # Main OpenGL renderer
│   └── RoomSurfaceView.kt       # Touch input handler
│
├── 🗂️ repository/               # Business Logic Layer
│   ├── AnnotationRepo.kt        # Annotation operations
│   └── RobotRepo.kt             # Robot operations
│
└── 🖥️ ui/roomviewer/            # User Interface
    ├── RoomViewerFragment.kt    # Main UI (Compose + OpenGL)
    └── RoomViewerViewModel.kt   # State management
```

## 🛠️ Tech Stack

| Category | Technologies |
|----------|-------------|
| **Language** | Kotlin |
| **UI** | Jetpack Compose (Material 3) |
| **3D Graphics** | OpenGL ES 2.0 |
| **Architecture** | MVVM + Clean Architecture |
| **Dependency Injection** | Hilt/Dagger |
| **Database** | Room (SQLite) |
| **Async** | Kotlin Coroutines + Flow |
| **Build** | Gradle 8.13 |

## 💡 Key Technical Highlights

### 1. **Hybrid UI System**
Combines OpenGL ES for 3D rendering with Jetpack Compose for UI controls - the best of both worlds! A transparent `ComposeView` overlays the `GLSurfaceView`, allowing touch events to intelligently route to the appropriate layer.

### 2. **Smart Camera System**
The camera automatically detects when you're inside or outside the room bounds and updates the indicator in real-time. This helps users maintain spatial awareness while navigating.

### 3. **Thread-Safe Rendering**
All OpenGL operations happen on the GL thread, while UI updates occur on the main thread. Volatile flags ensure safe communication between threads when switching wall rendering modes.

### 4. **Ray-Casting for Placement**
When you tap to place the robot, the app converts your 2D screen touch into a 3D ray, calculates where it intersects with the floor plane, and positions the robot precisely at that point.

### 5. **Dynamic Geometry Generation**
The room can switch between flat, mesh, and wireframe modes on-the-fly by regenerating vertex buffers with different geometry patterns - all without recreating the OpenGL context.

### 6. **Persistent State Management**
Everything you create is immediately saved to the local Room database and automatically restored when you reopen the app - even after device rotation or app shutdown.

## 🎨 Color Coding

- **Spray Area** → Red annotations
- **Sand Area** → Yellow annotations
- **Obstacle** → Orange annotations
- **Inside Room** → Green indicator
- **Outside Room** → Red indicator
- **Flat Walls** → Blue-gray button
- **Mesh Walls** → Purple button
- **Wireframe** → Cyan button

## 🐛 Known Limitations

1. **Robot Model**: Currently uses a simple cube representation instead of the full UR10e URDF model (planned enhancement)
2. **Annotation Editing**: Once placed, annotations cannot be resized or moved (only deleted)
3. **No Undo**: No undo/redo functionality for actions

## 🚀 Future Roadmap

- [ ] Full UR10e robot model with articulated joints
- [ ] Drag-to-resize annotations
- [ ] Export room configuration to JSON/XML
- [ ] Multiple room support with different dimensions
- [ ] AR mode using ARCore
- [ ] Path planning visualization for robot movement

## 🔧 Troubleshooting

**Blank screen on launch?**
- Ensure your device supports OpenGL ES 2.0
- Check Android version is 7.0 or higher

**Performance issues?**
- The app uses continuous rendering for smooth animations
- On older devices, you can optimize by modifying `RENDERMODE_CONTINUOUSLY` to `RENDERMODE_WHEN_DIRTY`

**Build errors?**
```bash
./gradlew clean
./gradlew assembleDebug
```

## 📄 License

Created as part of an Android development internship assignment.

---

**Made with ❤️ using Kotlin, OpenGL ES, and Jetpack Compose** by **𝓐𝓭𝓲𝓽𝔂𝓪**
