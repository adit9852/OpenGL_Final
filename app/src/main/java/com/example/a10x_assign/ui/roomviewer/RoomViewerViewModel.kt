package com.example.a10x_assign.ui.roomviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a10x_assign.data.AnnotationEntity
import com.example.a10x_assign.data.AnnotationType
import com.example.a10x_assign.data.RobotEntity
import com.example.a10x_assign.data.WallType
import com.example.a10x_assign.repository.AnnotationRepo
import com.example.a10x_assign.repository.RobotRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class WallRenderMode {
    WIREFRAME,
    MESH
}

data class RoomViewerState(
    val isInitialized: Boolean = false,
    val errorMessage: String? = null,
    val selectedAnnotationType: AnnotationType = AnnotationType.SPRAY_AREA,
    val selectedWall: WallType? = null,
    val isAnnotationMode: Boolean = false,
    val isRobotPlacementMode: Boolean = false,
    val annotations: List<AnnotationEntity> = emptyList(),
    val robotPosition: RobotEntity? = null,
    val showAnnotationList: Boolean = false,
    val wallRenderMode: WallRenderMode = WallRenderMode.WIREFRAME,
    val isCameraInsideRoom: Boolean = true,
    val robotSize: Float = 1.0f
)

@HiltViewModel
class RoomViewerViewModel @Inject constructor(
    private val annotationRepo: AnnotationRepo,
    private val robotRepo: RobotRepo
) : ViewModel() {
    private val _uiState = MutableStateFlow(RoomViewerState())
    val uiState: StateFlow<RoomViewerState> = _uiState.asStateFlow()

    init {
        loadAnnotations()
        loadRobotPosition()
    }

    private fun loadAnnotations() {
        viewModelScope.launch(Dispatchers.IO) {
            annotationRepo.getAllAnnotations().collect { annotations ->
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(annotations = annotations)
                }
            }
        }
    }

    private fun loadRobotPosition() {
        viewModelScope.launch(Dispatchers.IO) {
            robotRepo.getRobotPosition().collect { robot ->
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(robotPosition = robot)
                }
            }
        }
    }

    fun onRoomInitialized() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isInitialized = true,
                errorMessage = null
            )
        }
    }

    fun setAnnotationType(type: AnnotationType) {
        _uiState.value = _uiState.value.copy(selectedAnnotationType = type)
    }

    fun setSelectedWall(wall: WallType?) {
        _uiState.value = _uiState.value.copy(selectedWall = wall)
    }

    fun toggleAnnotationMode() {
        val newMode = !_uiState.value.isAnnotationMode
        _uiState.value = _uiState.value.copy(
            isAnnotationMode = newMode,
            isRobotPlacementMode = false
        )
    }

    fun toggleRobotPlacementMode() {
        _uiState.value = _uiState.value.copy(
            isRobotPlacementMode = !_uiState.value.isRobotPlacementMode,
            isAnnotationMode = false
        )
    }

    fun toggleAnnotationList() {
        _uiState.value = _uiState.value.copy(
            showAnnotationList = !_uiState.value.showAnnotationList
        )
    }

    fun toggleMeshWalls() {
        val newMode = when (_uiState.value.wallRenderMode) {
            WallRenderMode.WIREFRAME -> WallRenderMode.MESH
            WallRenderMode.MESH -> WallRenderMode.WIREFRAME
        }
        _uiState.value = _uiState.value.copy(wallRenderMode = newMode)
    }

    fun addAnnotation(wall: WallType, x: Float, y: Float, width: Float, height: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val annotation = AnnotationEntity(
                    type = _uiState.value.selectedAnnotationType,
                    wallType = wall,
                    x = x,
                    y = y,
                    width = width,
                    height = height
                )
                annotationRepo.insertAnnotation(annotation)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to add annotation: ${e.message}"
                    )
                }
            }
        }
    }

    fun deleteAnnotation(annotation: AnnotationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                annotationRepo.deleteAnnotation(annotation)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to delete annotation: ${e.message}"
                    )
                }
            }
        }
    }

    fun placeRobot(x: Float, y: Float, z: Float, rotationY: Float = 0f) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // First clear any existing robots
                robotRepo.clearRobot()

                // Insert the new robot
                val robot = RobotEntity(x = x, y = y, z = z, rotationY = rotationY)
                val insertedId = robotRepo.insertRobot(robot)

                // Verify insertion was successful
                if (insertedId > 0) {
                    withContext(Dispatchers.Main) {
                        // Force a refresh of the robot position
                        // The Flow should automatically update, but we ensure it here
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to place robot: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearRobot() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                robotRepo.clearRobot()
                withContext(Dispatchers.Main) {
                    // Explicitly set robot position to null after clearing
                    _uiState.value = _uiState.value.copy(robotPosition = null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to clear robot: ${e.message}"
                    )
                }
            }
        }
    }

    fun onError(message: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                errorMessage = message
            )
        }
    }

    fun clearError() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                errorMessage = null
            )
        }
    }

    fun updateCameraPosition(isCameraInsideRoom: Boolean) {
        _uiState.value = _uiState.value.copy(
            isCameraInsideRoom = isCameraInsideRoom
        )
    }

    fun increaseRobotSize() {
        val newSize = (_uiState.value.robotSize + 0.1f).coerceAtMost(3.0f)
        _uiState.value = _uiState.value.copy(robotSize = newSize)
    }

    fun decreaseRobotSize() {
        val newSize = (_uiState.value.robotSize - 0.1f).coerceAtLeast(0.3f)
        _uiState.value = _uiState.value.copy(robotSize = newSize)
    }
}
