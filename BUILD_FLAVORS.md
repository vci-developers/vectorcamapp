# VectorCam Build Flavors Guide

This document explains how to build VectorCam for different regions using Android Product Flavors.

---

## 📋 Available Flavors

VectorCam supports three regional flavors:

| Flavor | Region | Version Code | Version Name | App ID Suffix |
|--------|--------|--------------|--------------|---------------|
| `colombia` | Colombia | 1006 | 1.0.6 | `.colombia` |
| `uganda` | Uganda | 2004 | 1.0.4 | `.uganda` |
| `nigeria` | Nigeria | 3001 | 1.0.1 | `.nigeria` |

---

## 🔨 Build Commands

### Debug Builds

```bash
# Colombia Debug
./gradlew assembleColombiaDebug

# Uganda Debug
./gradlew assembleUgandaDebug

# Nigeria Debug
./gradlew assembleNigeriaDebug
```

### Release Builds

```bash
# Colombia Release
./gradlew assembleColombiaRelease

# Uganda Release
./gradlew assembleUgandaRelease

# Nigeria Release
./gradlew assembleNigeriaRelease
```

### Build All Variants

```bash
# Build all debug variants
./gradlew assembleDebug

# Build all release variants
./gradlew assembleRelease

# Build everything
./gradlew assemble
```

---

## 🚀 Running from Android Studio

1. Open **Build Variants** panel (View → Tool Windows → Build Variants)
2. Select desired variant from dropdown:
   - `colombiaDebug`
   - `colombiaRelease`
   - `ugandaDebug`
   - `ugandaRelease`
   - `nigeriaDebug`
   - `nigeriaRelease`
3. Click Run ▶️

---

## 📦 Build Output Locations

APKs are generated in:
```
app/build/outputs/apk/{flavor}/{buildType}/
```

Examples:
- `app/build/outputs/apk/colombia/debug/app-colombia-debug.apk`
- `app/build/outputs/apk/uganda/release/app-uganda-release.apk`

AABs (Android App Bundles) are generated in:
```
app/build/outputs/bundle/{flavor}Release/
```

Example:
- `app/build/outputs/bundle/colombiaRelease/app-colombia-release.aab`

---

## 🎯 Flavor-Specific Configuration

Each flavor has its own:

### 1. Application ID
- Colombia: `com.vci.vectorcamapp.colombia`
- Uganda: `com.vci.vectorcamapp.uganda`
- Nigeria: `com.vci.vectorcamapp.nigeria`

**Benefit:** All three apps can be installed simultaneously on the same device.

### 2. Version Code & Name
Managed independently per region in `app/build.gradle.kts`:

```kotlin
productFlavors {
    create("colombia") {
        versionCode = 1006
        versionName = "1.0.6"
    }
    // ... etc
}
```

### 3. BuildConfig Fields
Access region info in code:

```kotlin
val region = BuildConfig.REGION              // "colombia", "uganda", or "nigeria"
val regionCode = BuildConfig.REGION_CODE     // "CO", "UG", or "NG"
val regionName = BuildConfig.REGION_DISPLAY_NAME  // "Colombia", "Uganda", or "Nigeria"
```

### 4. String Resources
Each flavor has a region-specific app name:

```kotlin
// Access in code
val regionAppName = getString(R.string.app_name_region)
// "VectorCam Colombia", "VectorCam Uganda", or "VectorCam Nigeria"
```

---

## 🧪 Testing Specific Flavors

### Unit Tests

```bash
# Test specific flavor
./gradlew testColombiaDebugUnitTest
./gradlew testUgandaDebugUnitTest
./gradlew testNigeriaDebugUnitTest

# Test all debug flavors
./gradlew testDebugUnitTest
```

### Instrumented Tests

```bash
# Run instrumented tests for specific flavor
./gradlew connectedColombiaDebugAndroidTest
./gradlew connectedUgandaDebugAndroidTest
./gradlew connectedNigeriaDebugAndroidTest
```

---

## 📱 Installing on Device

### Via Gradle

```bash
# Install specific flavor
./gradlew installColombiaDebug
./gradlew installUgandaRelease
./gradlew installNigeriaDebug

# Install and run
./gradlew installColombiaDebug && adb shell am start -n com.vci.vectorcamapp.colombia/.MainActivity
```

### Via ADB

```bash
# Install APK directly
adb install app/build/outputs/apk/colombia/debug/app-colombia-debug.apk

# Uninstall
adb uninstall com.vci.vectorcamapp.colombia
adb uninstall com.vci.vectorcamapp.uganda
adb uninstall com.vci.vectorcamapp.nigeria
```

---

## 🎨 Flavor-Specific Resources (Optional)

You can create flavor-specific resources:

### Directory Structure
```
app/src/
├── main/                  # Shared resources
│   ├── java/
│   ├── res/
│   └── AndroidManifest.xml
├── colombia/              # Colombia-specific
│   ├── res/
│   │   ├── values/
│   │   │   └── strings.xml
│   │   └── drawable/
│   │       └── ic_launcher.png
│   └── AndroidManifest.xml
├── uganda/                # Uganda-specific
│   └── res/
└── nigeria/               # Nigeria-specific
    └── res/
```

### Example: Flavor-Specific Colors

**app/src/colombia/res/values/colors.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="primary">#FFD700</color> <!-- Colombia yellow -->
</resources>
```

**app/src/uganda/res/values/colors.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="primary">#FCDC04</color> <!-- Uganda yellow -->
</resources>
```

---

## 🔧 CI/CD Integration

### GitHub Actions Example

```yaml
name: Build All Flavors

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        flavor: [colombia, uganda, nigeria]
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          
      - name: Build ${{ matrix.flavor }} Release
        run: ./gradlew assemble${{ matrix.flavor }}Release
        
      - name: Upload APK
        uses: actions/upload-artifact@v3
        with:
          name: app-${{ matrix.flavor }}-release
          path: app/build/outputs/apk/${{ matrix.flavor }}/release/*.apk
```

### Build Specific Flavor in CI

```bash
# Environment variable or parameter
FLAVOR=colombia
./gradlew assemble${FLAVOR}Release
```

---

## 📊 Version Management Strategy

### Current Approach
Each flavor has independent version codes:
- **Colombia**: 1xxx series (e.g., 1006)
- **Uganda**: 2xxx series (e.g., 2004)
- **Nigeria**: 3xxx series (e.g., 3001)

This ensures:
- ✅ No version conflicts between regions
- ✅ Easy to identify region from version code
- ✅ Independent update cycles per region

### Updating Versions

Edit `app/build.gradle.kts`:

```kotlin
productFlavors {
    create("colombia") {
        versionCode = 1007  // Increment for new Colombia release
        versionName = "1.0.7"
    }
}
```

---

## 🚨 Common Issues & Solutions

### Issue: Build variant not showing

**Solution:** Sync Gradle files
```bash
File → Sync Project with Gradle Files
```

### Issue: Wrong flavor being built

**Solution:** Check Build Variants panel
1. View → Tool Windows → Build Variants
2. Verify correct variant is selected
3. Clean and rebuild: Build → Clean Project → Rebuild Project

### Issue: Flavor-specific resources not found

**Solution:** Ensure correct source set structure
```bash
# Check directory structure
ls -la app/src/
# Should show main/, colombia/, uganda/, nigeria/
```

### Issue: Version code conflict on Play Store

**Solution:** Ensure each flavor uses different version code series
- Colombia: 1xxx
- Uganda: 2xxx
- Nigeria: 3xxx

---

## 📚 Additional Resources

- [Android Product Flavors Documentation](https://developer.android.com/studio/build/build-variants)
- [Managing Build Variants](https://developer.android.com/studio/build/build-variants)
- [Configure Product Flavors](https://developer.android.com/build/gradle-tips#configure-project-wide-properties)

---

## 🎯 Quick Reference

| Task | Command |
|------|---------|
| Build Colombia Debug | `./gradlew assembleColombiaDebug` |
| Build Uganda Release | `./gradlew assembleUgandaRelease` |
| Install Nigeria Debug | `./gradlew installNigeriaDebug` |
| Test Colombia | `./gradlew testColombiaDebugUnitTest` |
| Build All Debug | `./gradlew assembleDebug` |
| Clean Build | `./gradlew clean assemble` |

---

**Last Updated:** February 6, 2026  
**Maintained by:** VectorCam Team
