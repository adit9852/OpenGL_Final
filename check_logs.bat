@echo off
echo Checking for connected Android devices...
adb devices

echo.
echo Clearing old logs...
adb logcat -c

echo.
echo Starting logcat filter for PLYModel and Camera...
echo Press Ctrl+C to stop
echo.
adb logcat -s PLYModel:D Camera:D GLSurfaceView:D dalvikvm:E AndroidRuntime:E
