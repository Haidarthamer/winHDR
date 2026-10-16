# WinHDR native Android UI patch (Arena handoff)

This is a **native Android Java/XML patch**, not an HTML mockup. It targets the Winlator app source pinned by the WinHDR repository at:

`brunodev85/winlator-app` commit `a030f552f452158a2db64fdb32b490fa19c0b48d`

## Apply it in Arena

1. Download `WinHDR-native-android-ui.patch`.
2. Open the actual Android project/submodule directory named `app` in the WinHDR checkout (the directory containing `src/main/java/com/winlator/MainActivity.java` and `src/main/res`).
3. Apply the patch from that `app` directory using `git apply WinHDR-native-android-ui.patch`. If Arena only accepts uploaded files and cannot run git commands, tell it to apply the diff manually to the named files.
4. Build the existing Android project and fix any source/resource compatibility issues before delivering an APK.

## What it changes

- Adds a native WinHDR home dashboard in Java + Android XML.
- Makes the dashboard the normal initial screen.
- Wires its buttons to the existing Containers, Shortcuts/Game Library, Input Controls, and Settings screens.
- Keeps the existing Wine/Box64/container/game-launch architecture intact.

## Important scope

This is a UI integration patch, **not a built or tested APK**. It does not add new emulation capabilities, performance monitoring, or recent-game data. The original emulator features remain in the existing app source. Validate compilation and device layout before release.
