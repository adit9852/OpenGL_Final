# UI Improvement: Robot Size Controls

## Problem
The robot size control buttons (minus/plus and Clear) were always visible, taking up too much space at the bottom of the screen. This made the main control buttons (Add Annotation, Place Robot) too small and difficult to use.

## Solution
Implemented a collapsible UI pattern for robot controls:

### Changes Made

#### 1. RoomViewerViewModel.kt
- **Line 38**: Added new state field `showRobotSizeControls: Boolean = false`
- **Lines 126-130**: Added `toggleRobotSizeControls()` function to show/hide size controls

#### 2. RoomViewerFragment.kt
- **Line 362**: Added `onToggleRobotSizeControls` parameter to `RoomViewerUI` composable
- **Line 311**: Connected the callback to ViewModel's toggle function
- **Lines 518-604**: Restructured robot controls UI:
  - Default view shows: "Adjust Size" button + "Clear" button
  - Clicking "Adjust Size" expands to show: minus/plus buttons + size display
  - Button text changes from "Adjust Size" → "Hide Size" based on state
  - Size controls have better styling with larger text and padding

### UI Behavior

**Before**:
- Always visible: [−] [Robot Size: 1.0x] [+] [Clear]
- Took up ~80-100% of bottom control bar width

**After** (collapsed):
- [Adjust Size] [Clear]
- Only takes up ~40% of bottom control bar width

**After** (expanded):
- [Hide Size] [Clear]
- Size: [−] [1.0x] [+]
- Hidden by default, only shows when user clicks "Adjust Size"

### Benefits
1. **More space** for main control buttons (Add Annotation, Place Robot)
2. **Better UX** - advanced controls only shown when needed
3. **Cleaner interface** - reduces visual clutter
4. **Better button sizing** - main controls are now larger and more usable
5. **Intuitive** - toggle button clearly indicates expand/collapse state

## Build Status
✅ All changes compile successfully with `./gradlew assembleDebug`

## Testing
Test that:
1. "Adjust Size" button expands to show size controls
2. "Hide Size" button collapses the controls
3. Main control buttons are now larger and easier to tap
4. Size adjustment still works correctly
5. "Clear" button remains easily accessible
