package com.example.a10x_assign.ui.roomviewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
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

        // Set up camera position callback
        glSurfaceView.renderer.onCameraPositionChanged = { isInside ->
            viewModel.updateCameraPosition(isInside)
        }

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

                    // Update wall render mode in PLY model
                    LaunchedEffect(state.wallRenderMode) {
                        glSurfaceView.renderer.plyModel.renderMode = when (state.wallRenderMode) {
                            WallRenderMode.WIREFRAME -> 0  // Wireframe mode (smaller points)
                            WallRenderMode.MESH -> 1       // Mesh mode (larger points)
                        }
                    }

                    // Update robot size in renderer
                    LaunchedEffect(state.robotSize) {
                        glSurfaceView.renderer.robotSize = state.robotSize
                    }

                    // Handle robot placement mode
                    // Robot placement now happens via tap on floor in touch interceptor
                    // No automatic placement needed here

                    // Show help dialog on first launch
                    var showHelp by remember { mutableStateOf(!state.isInitialized) }

                    if (showHelp) {
                        HelpDialog(onDismiss = {
                            showHelp = false
                            viewModel.onRoomInitialized()
                        })
                    }

                    RoomViewerUI(
                        state = state,
                        onAnnotationModeToggle = { viewModel.toggleAnnotationMode() },
                        onRobotPlacementToggle = { viewModel.toggleRobotPlacementMode() },
                        onAnnotationTypeSelected = { viewModel.setAnnotationType(it) },
                        onAnnotationListToggle = { viewModel.toggleAnnotationList() },
                        onDeleteAnnotation = { viewModel.deleteAnnotation(it) },
                        onClearRobot = { viewModel.clearRobot() },
                        onToggleMeshWalls = { viewModel.toggleMeshWalls() },
                        onCancelRobotPlacement = { viewModel.toggleRobotPlacementMode() },
                        onIncreaseRobotSize = { viewModel.increaseRobotSize() },
                        onDecreaseRobotSize = { viewModel.decreaseRobotSize() }
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

        // Update model dimensions for annotations and raycaster after initialization
        // Use a post to ensure OpenGL context is ready
        glSurfaceView.post {
            glSurfaceView.queueEvent {
                glSurfaceView.renderer.updateModelDimensions(rayCaster)
            }
        }

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
    onToggleMeshWalls: () -> Unit,
    onCancelRobotPlacement: () -> Unit,
    onIncreaseRobotSize: () -> Unit,
    onDecreaseRobotSize: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera position indicator (floating in canvas) with animation
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInHorizontally(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 120.dp)
        ) {
            val backgroundColor by animateColorAsState(
                targetValue = if (state.isCameraInsideRoom) Color(0x88009688) else Color(0x88FF5722),
                animationSpec = tween(durationMillis = 300), label = "cameraIndicatorColor"
            )

            Surface(
                color = backgroundColor,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Animated indicator dot
                    val scale by rememberInfiniteTransition(label = "pulse").animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ), label = "pulseScale"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .scale(scale)
                            .background(Color.White, CircleShape)
                    )

                    Text(
                        text = if (state.isCameraInsideRoom) "Inside Room" else "Outside Room",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                    )
                }
            }
        }

        // Top toolbar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            color = Color(0xDD000000),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Robot Operator",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onToggleMeshWalls,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (state.wallRenderMode) {
                                WallRenderMode.MESH -> Color(0xFF7B1FA2)
                                WallRenderMode.WIREFRAME -> Color(0xFF0097A7)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            when (state.wallRenderMode) {
                                WallRenderMode.MESH -> "Mesh"
                                WallRenderMode.WIREFRAME -> "Wire"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = onAnnotationListToggle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.showAnnotationList) Color(0xFF1976D2) else Color(0xFF546E7A)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Notes (${state.annotations.size})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Bottom controls
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = Color(0xDD000000),
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
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
                            containerColor = if (state.isAnnotationMode) Color(0xFF43A047) else Color(0xFF1E88E5)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp)
                    ) {
                        Text(
                            if (state.isAnnotationMode) "Exit Annotation" else "Add Annotation",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                        )
                    }

                    Button(
                        onClick = onRobotPlacementToggle,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isRobotPlacementMode) Color(0xFFE53935) else Color(0xFFFB8C00)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp)
                    ) {
                        Text(
                            if (state.isRobotPlacementMode) "Cancel Place" else "Place Robot",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                        )
                    }

                    if (state.robotPosition != null) {
                        Button(
                            onClick = onClearRobot,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Text(
                                "Clear",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                // Robot size controls (shown when robot is placed)
                if (state.robotPosition != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Robot Size:",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Button(
                            onClick = onDecreaseRobotSize,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF546E7A)
                            ),
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text("-", style = MaterialTheme.typography.titleLarge)
                        }

                        Text(
                            String.format("%.1fx", state.robotSize),
                            color = Color(0xFF4CAF50),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            ),
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Button(
                            onClick = onIncreaseRobotSize,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF546E7A)
                            ),
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text("+", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }

                // Animated mode indicators
                AnimatedVisibility(
                    visible = state.isAnnotationMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.padding(top = 8.dp),
                        color = Color(0xFF4CAF50),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✓", color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Annotation mode: Tap any wall to place ${state.selectedAnnotationType.name.replace("_", " ")}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = state.isRobotPlacementMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.padding(top = 8.dp),
                        color = Color(0xFFFF9800),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✓", color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Robot placement: Tap the floor to place robot",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
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
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Welcome to Robot Operator",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Quick Guide:",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                )

                HelpItem("🔄", "Rotate", "One finger drag to rotate the view")
                HelpItem("🔍", "Zoom", "Pinch to zoom in/out")
                HelpItem("👆", "Pan", "Two finger drag to move around")
                HelpItem("🤖", "Robot", "Place robot on floor and adjust size")
                HelpItem("📝", "Annotations", "Add notes to walls by tapping")
                HelpItem("🎨", "View Modes", "Toggle between Wire and Mesh views")

                Text(
                    "Tip: You can drag the robot after placing it!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1976D2)
                )
            ) {
                Text("Got it!")
            }
        },
        containerColor = Color(0xFF1E1E1E),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
fun HelpItem(icon: String, title: String, description: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            icon,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.width(32.dp)
        )
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                ),
                color = Color.White
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
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
