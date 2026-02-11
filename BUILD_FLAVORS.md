# VectorCam Build Flavors Guide

This document explains how to build VectorCam for different regions using Android Product Flavors.

---

## 📋 Available Flavors

VectorCam supports five regional flavors:

| Flavor | Region | Version Code | Version Name | App ID Suffix | Flag |
|--------|--------|--------------|--------------|---------------|------|
| `uganda` | Uganda | 2007 | 1.0.7 | `.uganda` | 🇺🇬 |
| `colombia` | Colombia | 1007 | 1.0.7 | `.colombia` | 🇨🇴 |
| `nigeria` | Nigeria | 3001 | 1.0.1 | `.nigeria` | 🇳🇬 |
| `kenya` | Kenya | 4001 | 1.0.1 | `.kenya` | 🇰🇪 |
| `ghana` | Ghana | 5001 | 1.0.1 | `.ghana` | 🇬🇭 |

---

## 🔨 Build Commands

### Debug Builds

```bash
# Uganda Debug (Default)
./gradlew assembleUgandaDebug

# Colombia Debug
./gradlew assembleColombiaDebug

# Nigeria Debug
./gradlew assembleNigeriaDebug

# Kenya Debug
./gradlew assembleKenyaDebug

# Ghana Debug
./gradlew assembleGhanaDebug
```

### Release Builds

```bash
# Uganda Release
./gradlew assembleUgandaRelease

# Colombia Release
./gradlew assembleColombiaRelease

# Nigeria Release
./gradlew assembleNigeriaRelease

# Kenya Release
./gradlew assembleKenyaRelease

# Ghana Release
./gradlew assembleGhanaRelease
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
   - `ugandaDebug` / `ugandaRelease` (Default)
   - `colombiaDebug` / `colombiaRelease`
   - `nigeriaDebug` / `nigeriaRelease`
   - `kenyaDebug` / `kenyaRelease`
   - `ghanaDebug` / `ghanaRelease`
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
- Uganda: `com.vci.vectorcamapp.uganda`
- Colombia: `com.vci.vectorcamapp.colombia`
- Nigeria: `com.vci.vectorcamapp.nigeria`
- Kenya: `com.vci.vectorcamapp.kenya`
- Ghana: `com.vci.vectorcamapp.ghana`

**Benefit:** All five apps can be installed simultaneously on the same device.

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
val region = BuildConfig.REGION              // "uganda", "colombia", "nigeria", "kenya", or "ghana"
val regionCode = BuildConfig.REGION_CODE     // "UG", "CO", "NG", "KE", or "GH"
val regionName = BuildConfig.REGION_DISPLAY_NAME  // "Uganda", "Colombia", "Nigeria", "Kenya", or "Ghana"
```

### 4. String Resources
Each flavor has a region-specific app name:

```kotlin
// Access in code
val regionAppName = getString(R.string.app_name_region)
// "VectorCam Uganda", "VectorCam Colombia", "VectorCam Nigeria", 
// "VectorCam Kenya", or "VectorCam Ghana"
```

---

## 🧪 Testing Specific Flavors

### Unit Tests

```bash
# Test specific flavor
./gradlew testUgandaDebugUnitTest
./gradlew testColombiaDebugUnitTest
./gradlew testNigeriaDebugUnitTest
./gradlew testKenyaDebugUnitTest
./gradlew testGhanaDebugUnitTest

# Test all debug flavors
./gradlew testDebugUnitTest
```

### Instrumented Tests

```bash
# Run instrumented tests for specific flavor
./gradlew connectedUgandaDebugAndroidTest
./gradlew connectedColombiaDebugAndroidTest
./gradlew connectedNigeriaDebugAndroidTest
./gradlew connectedKenyaDebugAndroidTest
./gradlew connectedGhanaDebugAndroidTest
```

---

## 📱 Installing on Device

### Via Gradle

```bash
# Install specific flavor
./gradlew installUgandaDebug
./gradlew installColombiaRelease
./gradlew installNigeriaDebug
./gradlew installKenyaDebug
./gradlew installGhanaRelease

# Install and run
./gradlew installUgandaDebug && adb shell am start -n com.vci.vectorcamapp.uganda/.MainActivity
```

### Via ADB

```bash
# Install APK directly
adb install app/build/outputs/apk/uganda/debug/app-uganda-debug.apk

# Uninstall
adb uninstall com.vci.vectorcamapp.uganda
adb uninstall com.vci.vectorcamapp.colombia
adb uninstall com.vci.vectorcamapp.nigeria
adb uninstall com.vci.vectorcamapp.kenya
adb uninstall com.vci.vectorcamapp.ghana
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
├── nigeria/               # Nigeria-specific
│   └── res/
├── kenya/                 # Kenya-specific
│   └── res/
└── ghana/                 # Ghana-specific
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
        flavor: [uganda, colombia, nigeria, kenya, ghana]
    
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
- **Uganda**: 2xxx series (e.g., 2007)
- **Colombia**: 1xxx series (e.g., 1007)
- **Nigeria**: 3xxx series (e.g., 3001)
- **Kenya**: 4xxx series (e.g., 4001)
- **Ghana**: 5xxx series (e.g., 5001)

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
- Uganda: 2xxx
- Colombia: 1xxx
- Nigeria: 3xxx
- Kenya: 4xxx
- Ghana: 5xxx

---

## 📚 Additional Resources

- [Android Product Flavors Documentation](https://developer.android.com/studio/build/build-variants)
- [Managing Build Variants](https://developer.android.com/studio/build/build-variants)
- [Configure Product Flavors](https://developer.android.com/build/gradle-tips#configure-project-wide-properties)

---

## 🎯 Quick Reference

| Task | Command |
|------|---------|
| Build Uganda Debug | `./gradlew assembleUgandaDebug` |
| Build Colombia Release | `./gradlew assembleColombiaRelease` |
| Build Nigeria Debug | `./gradlew assembleNigeriaDebug` |
| Build Kenya Release | `./gradlew assembleKenyaRelease` |
| Build Ghana Debug | `./gradlew assembleGhanaDebug` |
| Install Uganda Debug | `./gradlew installUgandaDebug` |
| Test Colombia | `./gradlew testColombiaDebugUnitTest` |
| Build All Debug | `./gradlew assembleDebug` |
| Clean Build | `./gradlew clean assemble` |

---

**Last Updated:** February 11, 2026  
**Flavors:** 5 (Uganda, Colombia, Nigeria, Kenya, Ghana)  
**Maintained by:** VectorCam Team
