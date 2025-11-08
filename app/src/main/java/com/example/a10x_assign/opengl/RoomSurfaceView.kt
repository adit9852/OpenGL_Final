package com.example.a10x_assign.opengl

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import dagger.hilt.android.qualifiers.ActivityContext
import javax.inject.Inject

class RoomSurfaceView @Inject constructor(
    @ActivityContext context: Context,
    val renderer: RoomRenderer
) : GLSurfaceView(context) {

    private var previousX = 0f
    private var previousY = 0f
    private var previousX2 = 0f
    private var previousY2 = 0f
    private var scaleGestureDetector: ScaleGestureDetector

    // Gesture mode
    private enum class GestureMode {
        NONE, ROTATE, PAN, ZOOM
    }
    private var currentMode = GestureMode.NONE

    init {
        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2)

        // Pass context to renderer for text rendering
        renderer.setContext(context)

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(renderer)

        // Render the view only when there is a change in the drawing data
        renderMode = RENDERMODE_CONTINUOUSLY

        // Initialize scale gesture detector for pinch to zoom
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Handle pinch to zoom first
        scaleGestureDetector.onTouchEvent(event)

        val pointerCount = event.pointerCount

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Single finger down - prepare for rotation
                previousX = event.x
                previousY = event.y
                currentMode = GestureMode.ROTATE
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                // Second finger down
                if (pointerCount == 2) {
                    // Two fingers - switch to pan mode
                    currentMode = GestureMode.PAN
                    previousX = event.getX(0)
                    previousY = event.getY(0)
                    previousX2 = event.getX(1)
                    previousY2 = event.getY(1)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (scaleGestureDetector.isInProgress) {
                    // Zoom in progress
                    currentMode = GestureMode.ZOOM
                } else if (pointerCount == 2 && currentMode == GestureMode.PAN) {
                    // Pan with two fingers
                    val x1 = event.getX(0)
                    val y1 = event.getY(0)
                    val x2 = event.getX(1)
                    val y2 = event.getY(1)

                    // Calculate average movement of both fingers
                    val dx = ((x1 - previousX) + (x2 - previousX2)) / 2f
                    val dy = ((y1 - previousY) + (y2 - previousY2)) / 2f

                    // Pan the camera
                    renderer.camera.pan(dx, -dy)

                    previousX = x1
                    previousY = y1
                    previousX2 = x2
                    previousY2 = y2

                    requestRender()
                } else if (pointerCount == 1 && currentMode == GestureMode.ROTATE) {
                    // Rotate with one finger
                    val x = event.x
                    val y = event.y

                    val dx = x - previousX
                    val dy = y - previousY

                    // Rotate the camera
                    renderer.camera.rotate(dx, dy)

                    previousX = x
                    previousY = y

                    requestRender()
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                // A finger was lifted
                if (pointerCount == 2) {
                    // Going from 2 fingers to 1, switch back to rotate mode
                    currentMode = GestureMode.ROTATE
                    val remainingIndex = if (event.actionIndex == 0) 1 else 0
                    previousX = event.getX(remainingIndex)
                    previousY = event.getY(remainingIndex)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                currentMode = GestureMode.NONE
            }
        }

        return true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            renderer.camera.adjustZoom(detector.scaleFactor)
            requestRender()
            return true
        }
    }
}
