package com.example.a10x_assign.ui.roomviewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.a10x_assign.data.AnnotationType
import com.example.a10x_assign.opengl.RoomSurfaceView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RoomViewerFragment : Fragment() {
    private val viewModel: RoomViewerViewModel by viewModels()

    @Inject
    lateinit var glSurfaceView: RoomSurfaceView

    @Inject
    lateinit var rayCaster: com.example.a10x_assign.opengl.RayCaster

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Create a FrameLayout to hold both GL and Compose views
        val frameLayout = FrameLayout(requireContext())

        // Add GLSurfaceView
        frameLayout.addView(
            glSurfaceView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // Add transparent touch interceptor for annotation mode and robot drag
        val touchInterceptor = View(requireContext()).apply {
            var downX = 0f
            var downY = 0f
            var downTime = 0L
            var isTap = true
            var isDraggingRobot = false

            setOnTouchListener { v, event ->
                val state = viewModel.uiState.value

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.x
                        downY = event.y
                        downTime = System.currentTimeMillis()
                        isTap = true
                        isDraggingRobot = false

                        // Check if we're touching the robot (even if not in placement mode)
                        state.robotPosition?.let { robot ->
                            val hit = rayCaster.castRay(
                                event.x,
                                event.y,
                                v.width,
                                v.height,
                                glSurfaceView.renderer.camera
                            )

                            hit?.let {
                                // Check if the ray hit near the robot position
                                if (it.wallType == com.example.a10x_assign.data.WallType.FLOOR) {
                                    val dx = it.worldX - robot.x
                                    val dz = it.worldZ - robot.z
                                    val distance = Math.sqrt((dx * dx + dz * dz).toDouble()).toFloat()

                                    // If within 0.5 units of robot, start dragging
                                    if (distance < 0.5f) {
                                        isDraggingRobot = true
                                        return@setOnTouchListener true
                                    }
                                }
                            }
                        }

                        // Only intercept touches when in annotation or robot placement mode
                        if (!state.isAnnotationMode && !state.isRobotPlacementMode) {
                            return@setOnTouchListener false // Let GLSurfaceView handle all touches
                        }

                        true // Consume the event when in annotation/robot mode
                    }

                    MotionEvent.ACTION_MOVE -> {
                        // If dragging robot, update its position
                        if (isDraggingRobot) {
                            val hit = rayCaster.castRay(
                                event.x,
                                event.y,
                                v.width,
                                v.height,
                                glSurfaceView.renderer.camera
                            )

                            hit?.let {
                                if (it.wallType == com.example.a10x_assign.data.WallType.FLOOR) {
                                    // Update robot position as we drag
                                    state.robotPosition?.let { robot ->
                                        viewModel.placeRobot(it.worldX, it.worldY, it.worldZ, robot.rotationY)
                                    }
                                }
                            }
                            return@setOnTouchListener true
                        }

                        // Check if movement exceeds threshold
                        val deltaX = Math.abs(event.x - downX)
                        val deltaY = Math.abs(event.y - downY)
                        val touchSlop = 15f // pixels threshold for movement (increased for easier camera control)

                        if (deltaX > touchSlop || deltaY > touchSlop) {
                            isTap = false // It's a swipe/drag, not a tap
                        }

                        // If it's not a tap anymore and not dragging robot, pass through to camera controls
                        if (!isDraggingRobot) {
                            !isTap
                        } else {
                            true
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        // If we were dragging robot, complete the drag
                        if (isDraggingRobot) {
                            Toast.makeText(
                                requireContext(),
                                "Robot moved",
                                Toast.LENGTH_SHORT
                            ).show()
                            isDraggingRobot = false
                            return@setOnTouchListener true
                        }

                        val upTime = System.currentTimeMillis()
                        val touchDuration = upTime - downTime
                        val maxTapDuration = 400L // milliseconds (increased for easier tapping)

                        // Only process as tap if no significant movement occurred and touch was short
                        if (isTap && touchDuration < maxTapDuration) {

                            // Cast ray to find what was touched
                            val hit = rayCaster.castRay(
                                event.x,
                                event.y,
                                v.width,
                                v.height,
                                glSurfaceView.renderer.camera
                            )

                            hit?.let {
                                when {
                                    // Robot placement mode - only place on floor
                                    state.isRobotPlacementMode && it.wallType == com.example.a10x_assign.data.WallType.FLOOR -> {
                                        // Place robot at the tapped floor location
                                        viewModel.placeRobot(it.worldX, it.worldY, it.worldZ, 0f)
                                        viewModel.toggleRobotPlacementMode()

                                        // Show feedback
                                        Toast.makeText(
                                            requireContext(),
                                            "Robot placed at floor location",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@setOnTouchListener true
                                    }
                                    // Annotation mode - place on walls
                                    state.isAnnotationMode -> {
                                        // Add annotation at the touched location
                                        viewModel.addAnnotation(
                                            wall = it.wallType,
                                            x = it.x,
                                            y = it.y,
                                            width = 0.15f,  // Default size
                                            height = 0.15f
                                        )

                                        // Show feedback
                                        Toast.makeText(
                                            requireContext(),
                                            "Annotation added to ${it.wallType.name}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@setOnTouchListener true
                                    }
                                }
                            }

                            // If in robot placement mode but didn't hit floor, show message
                            if (state.isRobotPlacementMode && (hit == null || hit.wallType != com.example.a10x_assign.data.WallType.FLOOR)) {
                                Toast.makeText(
                                    requireContext(),
                                    "Tap on the floor to place robot",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@setOnTouchListener true
                            }
                        }

                        // Consume the event if it was a tap, otherwise let it through
                        isTap
                    }

                    else -> false
                }
            }
        }

        frameLayout.addView(
            touchInterceptor,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // Add Compose UI overlay
        val composeView = ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    val state by viewModel.uiState.collectAsState()

                    // Show error messages
                    LaunchedEffect(state.errorMessage) {
                        state.errorMessage?.let { message ->
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                            viewModel.clearError()
                        }
                    }

                    // Update robot position in renderer
                    LaunchedEffect(state.robotPosition) {
                        glSurfaceView.renderer.robotPosition = state.robotPosition
                    }

                    // Update annotations in renderer
                    LaunchedEffect(state.annotations) {
                        glSurfaceView.renderer.annotations = state.annotations
                    }

                    // Update wall render mode in room
                    LaunchedEffect(state.wallRenderMode) {
                        glSurfaceView.renderer.room.renderMode = when (state.wallRenderMode) {
                            WallRenderMode.FLAT -> 0
                            WallRenderMode.MESH -> 1
                            WallRenderMode.WIREFRAME -> 2
                        }
                    }

                    // Handle robot placement mode
                    // Robot placement now happens via tap on floor in touch interceptor
                    // No automatic placement needed here

                    RoomViewerUI(
                        state = state,
                        onAnnotationModeToggle = { viewModel.toggleAnnotationMode() },
                        onRobotPlacementToggle = { viewModel.toggleRobotPlacementMode() },
                        onAnnotationTypeSelected = { viewModel.setAnnotationType(it) },
                        onAnnotationListToggle = { viewModel.toggleAnnotationList() },
                        onDeleteAnnotation = { viewModel.deleteAnnotation(it) },
                        onClearRobot = { viewModel.clearRobot() },
                        onToggleMeshWalls = { viewModel.toggleMeshWalls() }
                    )
                }
            }
        }

        frameLayout.addView(
            composeView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // Mark as initialized
        viewModel.onRoomInitialized()

        return frameLayout
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()

        // Ensure robot position is refreshed when resuming
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.uiState.collect { state ->
                    glSurfaceView.renderer.robotPosition = state.robotPosition
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }
}

@Composable
fun RoomViewerUI(
    state: RoomViewerState,
    onAnnotationModeToggle: () -> Unit,
    onRobotPlacementToggle: () -> Unit,
    onAnnotationTypeSelected: (AnnotationType) -> Unit,
    onAnnotationListToggle: () -> Unit,
    onDeleteAnnotation: (com.example.a10x_assign.data.AnnotationEntity) -> Unit,
    onClearRobot: () -> Unit,
    onToggleMeshWalls: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top toolbar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            color = Color(0xCC000000),
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Robot Operator",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onToggleMeshWalls,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (state.wallRenderMode) {
                                WallRenderMode.FLAT -> Color(0xFF424242)
                                WallRenderMode.MESH -> Color(0xFF9C27B0)
                                WallRenderMode.WIREFRAME -> Color(0xFF00BCD4)
                            }
                        )
                    ) {
                        Text(when (state.wallRenderMode) {
                            WallRenderMode.FLAT -> "Flat Walls"
                            WallRenderMode.MESH -> "Mesh Walls"
                            WallRenderMode.WIREFRAME -> "Wireframe"
                        })
                    }

                    Button(
                        onClick = onAnnotationListToggle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.showAnnotationList) Color(0xFF2196F3) else Color(0xFF424242)
                        )
                    ) {
                        Text("Annotations (${state.annotations.size})")
                    }
                }
            }
        }

        // Bottom controls
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = Color(0xCC000000),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Annotation type selector
                if (state.isAnnotationMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnnotationType.values().forEach { type ->
                            Button(
                                onClick = { onAnnotationTypeSelected(type) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.selectedAnnotationType == type)
                                        Color(0xFF4CAF50) else Color(0xFF616161)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    type.name.replace("_", " "),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Main control buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onAnnotationModeToggle,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isAnnotationMode) Color(0xFF4CAF50) else Color(0xFF2196F3)
                        )
                    ) {
                        Text(if (state.isAnnotationMode) "Exit Annotation" else "Add Annotation")
                    }

                    Button(
                        onClick = onRobotPlacementToggle,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800)
                        )
                    ) {
                        Text("Place Robot")
                    }

                    if (state.robotPosition != null) {
                        Button(
                            onClick = onClearRobot,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF44336)
                            )
                        ) {
                            Text("Clear")
                        }
                    }
                }

                if (state.isAnnotationMode) {
                    Text(
                        "✓ Annotation mode active: Tap any wall to place ${state.selectedAnnotationType.name.replace("_", " ")}",
                        color = Color(0xFF4CAF50),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (state.isRobotPlacementMode) {
                    Text(
                        "✓ Robot placement active: Tap the floor to place robot",
                        color = Color(0xFFFF9800),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Annotation list overlay
        if (state.showAnnotationList) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .align(Alignment.Center),
                color = Color(0xEE121212),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Annotations",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                        TextButton(onClick = onAnnotationListToggle) {
                            Text("Close", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.annotations.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No annotations yet",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.annotations) { annotation ->
                                AnnotationItem(
                                    annotation = annotation,
                                    onDelete = { onDeleteAnnotation(annotation) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnnotationItem(
    annotation: com.example.a10x_assign.data.AnnotationEntity,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E1E1E),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    annotation.type.name.replace("_", " "),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    "Wall: ${annotation.wallType.name}",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDelete) {
                Text("✕", color = Color.Red, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
