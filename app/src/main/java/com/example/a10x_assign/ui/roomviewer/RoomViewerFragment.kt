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
import androidx.lifecycle.lifecycleScope
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

        // Add transparent touch interceptor for annotation mode
        val touchInterceptor = View(requireContext()).apply {
            setOnTouchListener { v, event ->
                val state = viewModel.uiState.value
                if (state.isAnnotationMode && event.action == MotionEvent.ACTION_DOWN) {
                    // Cast ray to find which wall was touched
                    val hit = rayCaster.castRay(
                        event.x,
                        event.y,
                        v.width,
                        v.height,
                        glSurfaceView.renderer.camera
                    )

                    hit?.let {
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
                    }
                    true  // Consume the event in annotation mode
                } else {
                    false  // Let touch pass through to GLSurfaceView for camera controls
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

                    // Handle robot placement mode
                    LaunchedEffect(state.isRobotPlacementMode) {
                        if (state.isRobotPlacementMode) {
                            // Simple placement at center of room floor
                            viewModel.placeRobot(0f, -1.5f, 0f, 0f)
                            viewModel.toggleRobotPlacementMode()
                        }
                    }

                    RoomViewerUI(
                        state = state,
                        onAnnotationModeToggle = { viewModel.toggleAnnotationMode() },
                        onRobotPlacementToggle = { viewModel.toggleRobotPlacementMode() },
                        onAnnotationTypeSelected = { viewModel.setAnnotationType(it) },
                        onAnnotationListToggle = { viewModel.toggleAnnotationList() },
                        onDeleteAnnotation = { viewModel.deleteAnnotation(it) },
                        onClearRobot = { viewModel.clearRobot() }
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
    onClearRobot: () -> Unit
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
