# Rename Project and References to hotkeyHydra

Rename all references to `ct70keyboard_v3` and `ct70keyboard` to `hotkeyHydra` / `hotkeyhydra`, updating the project name, package name, applicationId, namespace, themes, strings, service class, and build outputs.

## User Review Required

> [!IMPORTANT]
> This refactor will rename:
> - Root project name to `hotkeyHydra`
> - Package and namespace to `com.example.hotkeyhydra`
> - Application ID to `com.example.hotkeyhydra`
> - Service class `CT70KeyboardService` to `HotkeyHydraService`
> - Themes `Theme.Ct70keyboard` to `Theme.HotkeyHydra`
> - App display name and strings to "Hotkey Hydra"

## Proposed Changes

### Build and Settings Configuration

#### [MODIFY] [settings.gradle.kts](file:///C:/Users/garri/AndroidStudioProjects/hotkeyHydra/settings.gradle.kts)
- Change `rootProject.name = "ct70keyboard_v3"` to `rootProject.name = "hotkeyHydra"`

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/garri/AndroidStudioProjects/hotkeyHydra/app/build.gradle.kts)
- Change namespace to `"com.example.hotkeyhydra"`
- Change applicationId to `"com.example.hotkeyhydra"`
- Change APK output filename to `"hotkeyhydra-debug.apk"`

---

### Package and Source Files

#### [DELETE] [CT70KeyboardService.kt](file:///C:/Users/garri/AndroidStudioProjects/hotkeyHydra/app/src/main/java/com/example/ct70keyboard/CT70KeyboardService.kt)
#### [NEW] [HotkeyHydraService.kt](file:///C:/Users/garri/AndroidStudioProjects/hotkeyHydra/app/src/main/java/com/example/hotkeyhydra/HotkeyHydraService.kt)
- Rename package to `com.example.hotkeyhydra`
- Rename class `CT70KeyboardService` to `HotkeyHydraService`
- Update preferences name to `"hotkey_hydra_prefs"`

#### [MODIFY] [MainActivity.kt](file:///C:/Users/garri/AndroidStudioProjects/hotkeyHydra/app/src/main/java/com/example/ct70keyboard/MainActivity.kt) -> [MainActivity.kt](file:///C:/Users/garri/AndroidStudioProjects/hotkeyHydra/app/src/main/java/com/example/hotkeyhydra/MainActivity.kt)
- Update package to `com.example.hotkeyhydra`

#### [MODIFY] [ExampleInstrumentedTest.kt](file:///C:/Users/garri/AndroidStudioProjects/hotkeyHydra/app/src/androidTest/java/com/example/ct70keyboard/ExampleInstrumentedTest.kt)
- Update package and expected package assertion to `com.example.hotkeyhydra`

#### [MODIFY] [ExampleUnitTest.kt](file:///C:/Users/garri/AndroidStudioProjects/hotkeyHydra/app/src/test/java/com/example/ct70keyboard/ExampleUnitTest.kt)
- Update package to `com.example.hotkeyhydra`

---

### Resources and Manifest

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/garri/AndroidStudioProjects/hotkeyHydra/app/src/main/AndroidManifest.xml)
- Update theme to `@style/Theme.HotkeyHydra`
- Update service reference to `.HotkeyHydraService`

#### [MODIFY] [strings.xml](file:///C:/Users/garri/AndroidStudioProjects/hotkeyHydra/app/src/main/res/values/strings.xml)
- Update app name, IME name, and settings string to reference "Hotkey Hydra"

#### [MODIFY] [themes.xml](file:///C:/Users/garri/AndroidStudioProjects/hotkeyHydra/app/src/main/res/values/themes.xml) and [themes.xml](file:///C:/Users/garri/AndroidStudioProjects/hotkeyHydra/app/src/main/res/values-night/themes.xml)
- Rename styles from `Theme.Ct70keyboard` and `Base.Theme.Ct70keyboard` to `Theme.HotkeyHydra` and `Base.Theme.HotkeyHydra`

## Verification Plan

### Automated Tests
- Run Gradle build (`app:assembleDebug`) to verify compilation and resource packaging.
- Run unit and instrumented tests (`app:testDebugUnit`, `app:connectedAndroidTest`).

### Manual Verification
- Deploy app to device and verify application name, keyboard service enablement, and UI rendering.
