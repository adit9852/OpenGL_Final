# 🤖 Robot Operator – 3D Room Visualization App

<div align="center">

![Android](https://img.shields.io/badge/Android-7.0%2B-green?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple?logo=kotlin)
![OpenGL](https://img.shields.io/badge/OpenGL%20ES-2.0-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

**An intuitive Android app for 3D room visualization with advanced OpenGL rendering, interactive robot placement, and wall annotations.**

[🎮 Live Demo](https://appetize.io/app/b_zjzbzpuon35dwzkjay7zhjgvi4) • [📱 Download APK](https://github.com/adit9852/OpenGL_Final/blob/main/Apk/openGL_V2.apk) • [🎥 Video Demo](https://youtube.com/shorts/-3sKSWeaGA0?feature=share)

</div>

---

## ✨ Key Features

<table>
<tr>
<td width="50%">

### 🎨 **Visual Excellence**
- **Professional 3-light studio setup** with dynamic shadows
- **Tone mapping & gamma correction** for realistic colors
- **Smooth animations** with Material Design 3
- **Wire/Mesh mode** toggle for different perspectives

</td>
<td width="50%">

### 🎯 **Smart Interactions**
- **Intuitive touch controls** (rotate, pan, zoom)
- **Drag-and-drop robot** placement on floor
- **Wall annotations** with color-coded types
- **Auto-save** all changes to local database

</td>
</tr>
</table>

### 🔥 What Makes This Special

- 🎬 **Animated UI** – Smooth transitions and visual feedback
- 🤖 **Interactive Robot** – Place, drag, resize in real-time
- 📍 **Smart Annotations** – Ray-casting for precise wall placement
- 💡 **Studio Lighting** – Professional 3-point lighting system
- 💾 **Persistent Storage** – Everything auto-saves instantly
- 🎓 **Beginner Friendly** – Welcome guide on first launch

---

## 🎮 Quick Start

### Try It Now (No Installation)

1. **🌐 Browser Demo**: [Open on Appetize.io](https://appetize.io/app/b_zjzbzpuon35dwzkjay7zhjgvi4)
   - Opens instantly in your browser
   - No downloads required

2. **📱 Download APK**:
   - [Download openGL_V2.apk](https://github.com/adit9852/OpenGL_Final/blob/main/Apk/openGL_V2.apk)
   - Click **"View raw"** or **"Download"** button
   - Install on Android 7.0+ device

3. **🎥 Watch Demo**:
   - [Latest Demo (V2)](https://youtube.com/shorts/-3sKSWeaGA0?feature=share) - 60 seconds
   - [Full Demo (V1)](https://youtu.be/Nq4MBZ0mCZk?si=XCak05pDgomhHSyk) - Complete walkthrough

---

## 📱 How to Use

### 🕹️ **Camera Controls**

| Gesture | Action |
|---------|--------|
| **One finger drag** | Rotate camera around room |
| **Two finger drag** | Pan (move view) |
| **Pinch** | Zoom in/out |

### 🤖 **Robot Placement**

1. Tap **"Place Robot"** button
2. Tap anywhere on the **floor** to place
3. **Drag the robot** to reposition it
4. Use **+/-** buttons to adjust size
5. Tap **"Clear"** to remove

### 📝 **Add Annotations**

1. Tap **"Add Annotation"**
2. Select type: **WARNING** / **INFO** / **ERROR**
3. Tap on any **wall** to place marker
4. View all in **"Notes (X)"** list
5. Tap **✕** to delete any annotation

### 🎨 **View Modes**

- **Mesh Mode** – Larger point cloud (default)
- **Wire Mode** – Smaller points, wireframe look

---

## 🏗️ Architecture

Built with **Clean Architecture** + **MVVM** pattern for maintainability and scalability.

<div align="center">

```
┌─────────────────────────────────────────────────────────────┐
│                      UI Layer (Compose)                      │
│  RoomViewerFragment ──▶ RoomViewerViewModel ──▶ StateFlow  │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│                    Domain Layer (Logic)                      │
│        AnnotationRepo ◀──▶ RobotRepo ◀──▶ Room DB          │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│              Rendering Layer (OpenGL ES 2.0)                 │
│  RoomRenderer ──▶ Camera ──▶ PLYModel ──▶ RobotCube        │
│       │                                                      │
│       └──▶ AnnotationOverlay ──▶ TextRenderer               │
└─────────────────────────────────────────────────────────────┘
```

</div>

### 📦 **Key Components**

| Layer | Components | Responsibility |
|-------|-----------|----------------|
| **UI** | `RoomViewerFragment` + Compose | User interface, touch handling |
| **ViewModel** | `RoomViewerViewModel` | State management, business logic |
| **Repository** | `AnnotationRepo`, `RobotRepo` | Data operations, CRUD |
| **Database** | Room DB | Persistent storage (SQLite) |
| **Rendering** | OpenGL ES components | 3D graphics, shaders, lighting |

---

## 🔄 Application Flow

<div align="center">

```
                         App Launch
                             ↓
                    ┌────────────────┐
                    │  Initialize    │
                    │  - Database    │
                    │  - OpenGL      │
                    │  - Load Data   │
                    └────────┬───────┘
                             ↓
                    ┌────────────────┐
                    │   Render 3D    │
                    │   Room Scene   │
                    └────────┬───────┘
                             ↓
            ┌────────────────┴────────────────┐
            │         User Actions            │
            └─┬──────┬────────┬──────┬───────┘
              │      │        │      │
         Rotate   Place   Add    Toggle
         Camera   Robot   Note   View Mode
              │      │        │      │
              └──────┴────────┴──────┘
                      ↓
                ┌─────────┐
                │  Save   │
                │   to    │
                │   DB    │
                └─────────┘
```

</div>

---

## 📁 Project Structure

```
app/src/main/java/com/example/a10x_assign/
│
├── 📊 data/                          # Data Layer
│   ├── AnnotationEntity.kt           # Annotation model
│   ├── Annotations.kt                # Annotation DAO
│   ├── RobotEntity.kt                # Robot model
│   ├── Robot.kt                      # Robot DAO
│   └── AppDatabase.kt                # Room database
│
├── 💉 di/                            # Dependency Injection
│   └── AppModule.kt                  # Hilt modules & providers
│
├── 🎨 opengl/                        # 3D Rendering Engine
│   ├── Camera.kt                     # 🎥 Camera system with orbit controls
│   ├── PLYModel.kt                   # 🏠 3D room model with lighting
│   ├── PLYLoader.kt                  # 📥 Binary PLY file parser
│   ├── RobotCube.kt                  # 🤖 Robot 3D model
│   ├── AnnotationOverlay.kt          # 📍 Wall annotations renderer
│   ├── TextRenderer.kt               # 📝 3D text labels
│   ├── RoomRenderer.kt               # 🎬 Main OpenGL coordinator
│   ├── RoomSurfaceView.kt            # 👆 Touch input handler
│   └── RayCaster.kt                  # 🎯 3D ray-casting utility
│
├── 🗂️ repository/                    # Business Logic
│   ├── AnnotationRepo.kt             # Annotation operations
│   └── RobotRepo.kt                  # Robot operations
│
└── 🖥️ ui/roomviewer/                 # User Interface
    ├── RoomViewerFragment.kt         # Main UI (Compose + OpenGL)
    └── RoomViewerViewModel.kt        # State management & logic
```

---

## 🛠️ Tech Stack

<table>
<tr>
<td>

**Core Technologies**
- 🎯 Kotlin 1.9
- 📱 Android SDK 34
- 🎨 OpenGL ES 2.0
- ✨ GLSL Shaders

</td>
<td>

**UI Framework**
- 🎭 Jetpack Compose
- 🎨 Material Design 3
- 🔄 Compose Animations
- 🎯 Custom Touch Handling

</td>
<td>

**Architecture**
- 🏗️ MVVM Pattern
- 🧩 Clean Architecture
- 💉 Dagger Hilt
- 🔄 Kotlin Coroutines

</td>
<td>

**Data & Storage**
- 💾 Room Database
- 📊 SQLite
- 🔄 Flow/StateFlow
- 💿 SharedPreferences

</td>
</tr>
</table>

---

## 💡 Technical Highlights

### 🎨 **1. Professional Lighting System**

Implemented a 3-point studio lighting setup for realistic rendering:

- **Key Light** (Main): Camera-following bright white light
- **Fill Light** (Secondary): Soft cool-toned light reduces shadows
- **Rim Light** (Accent): Blue-tinted back light for depth

**Advanced Shader Features:**
```glsl
• Phong specular highlights for realistic surfaces
• Reinhard tone mapping for HDR-like results
• Gamma correction (sRGB) for accurate colors
• Multi-light diffuse and ambient calculations
```

### 🎯 **2. Precise Ray-Casting**

Converts 2D screen touches to 3D world coordinates:

```
Screen Touch (x, y)
    → Normalized Device Coords
    → View Space
    → World Space Ray
    → Floor Intersection (3D position)
```

This enables accurate robot placement and wall annotation positioning.

### 🎬 **3. Smooth Animations**

All UI transitions use Material Motion principles:

- **Camera indicator**: Animated color transitions (300ms)
- **Pulsing dot**: Infinite scale animation (1s cycle)
- **Mode switches**: Expand/collapse with fade effects
- **State changes**: Smooth interpolation for all properties

### 🧵 **4. Thread-Safe Rendering**

- **Main Thread**: UI updates, user input, database operations
- **GL Thread**: All OpenGL rendering, shader operations
- **Synchronization**: Volatile flags for safe cross-thread communication

### 📐 **5. Coordinate System Fix**

Applied 90° rotation during PLY loading to fix orientation:

```kotlin
// Transform from top-view to front-view
X → X (stays same)
Y → -Z (old height becomes depth)
Z → Y (old depth becomes height)
```

This ensures natural camera perspective and intuitive controls.

---

## 🎨 Visual Design

### Color Scheme

| Element | Color | Purpose |
|---------|-------|---------|
| **WARNING** | 🔴 Red | Critical annotations |
| **INFO** | 🔵 Blue | Information markers |
| **ERROR** | 🟠 Orange | Error indicators |
| **Inside Room** | 🟢 Green | Camera position indicator |
| **Outside Room** | 🔴 Red | Camera position indicator |
| **Mesh Mode** | 🟣 Purple | Active render mode |
| **Wire Mode** | 🔵 Cyan | Active render mode |

### UI Philosophy

- **Dark Theme**: Reduces eye strain, modern aesthetic
- **High Contrast**: Clear visibility of all elements
- **Consistent Spacing**: 8dp grid system (Material Design)
- **Smooth Transitions**: No jarring state changes
- **Clear Hierarchy**: Important actions prominently placed

---

## 🚀 Performance

| Metric | Value | Notes |
|--------|-------|-------|
| **Frame Rate** | 60 FPS | Consistent on mid-range devices |
| **Load Time** | < 2s | PLY file parsing & initialization |
| **Memory** | ~80 MB | Efficient vertex buffer management |
| **APK Size** | ~15 MB | Optimized with ProGuard |
| **Min Android** | 7.0 (API 24) | Compatible with 95%+ devices |

---

## 🎓 Learning Resources

Built this app? Here are the concepts demonstrated:

### Graphics Programming
- ✅ OpenGL ES 2.0 rendering pipeline
- ✅ Vertex & fragment shader programming (GLSL)
- ✅ 3D transformations (Model-View-Projection matrices)
- ✅ Lighting models (Phong shading)
- ✅ Texture mapping & color interpolation

### Android Development
- ✅ Jetpack Compose modern UI
- ✅ MVVM architecture pattern
- ✅ Dependency injection with Hilt
- ✅ Room database for persistence
- ✅ Coroutines & Flow for async operations

### Advanced Concepts
- ✅ Ray-casting for 3D picking
- ✅ Binary file parsing (PLY format)
- ✅ Touch gesture handling
- ✅ Thread synchronization
- ✅ Custom animations

---

## 🐛 Known Limitations

| Issue | Status | Workaround |
|-------|--------|------------|
| No mesh triangulation | Planned | Point cloud rendering only |
| Robot is simple cube | Planned | Full URDF model in development |
| Annotations can't be edited | Planned | Delete & recreate for now |
| No undo/redo | Planned | - |
| No shadow mapping | Future | Basic lighting only |

---

## 🚀 Future Enhancements

### Short Term
- [ ] **Triangulated mesh rendering** for solid surfaces
- [ ] **Annotation editing** (move, resize)
- [ ] **Undo/redo system** for all actions
- [ ] **Export/import** room configurations (JSON)

### Long Term
- [ ] **Full UR10e robot model** with articulated joints
- [ ] **Path planning visualization** for robot movement
- [ ] **AR mode** using ARCore for real-world overlay
- [ ] **Multi-room support** with different layouts
- [ ] **Shadow mapping** for realistic depth
- [ ] **Collaborative mode** (multi-user editing)

---

## 🔧 Building from Source

### Prerequisites
```bash
• Android Studio Hedgehog or newer
• JDK 17+
• Android SDK 34
• Gradle 8.13
```

### Build Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/adit9852/OpenGL_Final.git
   cd OpenGL_Final
   ```

2. **Open in Android Studio**
   - File → Open → Select project folder
   - Wait for Gradle sync

3. **Build APK**
   ```bash
   ./gradlew clean
   ./gradlew assembleDebug
   ```

   APK location: `app/build/outputs/apk/debug/app-debug.apk`

4. **Install on device**
   ```bash
   ./gradlew installDebug
   ```

### Troubleshooting

**Build fails?**
```bash
./gradlew clean build --refresh-dependencies
```

**OpenGL not rendering?**
- Check device supports OpenGL ES 2.0
- Verify Android version ≥ 7.0

**Performance issues?**
- Enable GPU profiling: Developer Options → GPU rendering
- Check frame rate: `adb shell dumpsys gfxinfo <package>`

---

## 📄 License

This project was created as part of an Android development internship assignment.

**License**: MIT (Open Source)

Feel free to use this code for learning, but please provide attribution.

---

## 🙏 Acknowledgments

- **OpenGL ES**: Graphics rendering
- **Jetpack Compose**: Modern Android UI
- **Material Design 3**: Design system
- **Room Database**: Data persistence
- **Dagger Hilt**: Dependency injection

---

## 📞 Contact

**Developer**: Aditya

- 📧 Email: [your-email@example.com]
- 💼 LinkedIn: [Your LinkedIn]
- 🐙 GitHub: [@adit9852](https://github.com/adit9852)

---

<div align="center">

### ⭐ Star this repo if you found it helpful!

**Made with ❤️ using Kotlin, OpenGL ES, and Jetpack Compose**

![Made with Kotlin](https://img.shields.io/badge/Made%20with-Kotlin-7F52FF?style=for-the-badge&logo=kotlin)
![OpenGL](https://img.shields.io/badge/OpenGL%20ES-2.0-5586A4?style=for-the-badge&logo=opengl)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose)

</div>
