# GitHub Actions CI/CD Setup Guide

This guide explains how to set up and use the GitHub Actions workflows for VectorCam.

---

## 📋 Available Workflows

### 1. **`build-debug.yml`** - Basic Build
Simple workflow that builds all three flavor debug APKs and runs tests.

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`
- Manual trigger via GitHub UI

**Output:**
- Debug APKs for Colombia, Uganda, and Nigeria
- Test results

### 2. **`build-and-distribute.yml`** - Advanced Build & Distribution
Full-featured workflow with Firebase App Distribution integration.

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests
- Manual trigger with distribution option

**Features:**
- ✅ Builds all flavor debug APKs
- ✅ Runs unit tests
- ✅ Generates changelogs
- ✅ Uploads to Firebase App Distribution
- ✅ Comments on PRs with build info
- ✅ Creates comprehensive build summary

### 3. **`pr-check.yml`** - Pull Request Validation
Lightweight workflow for PR validation.

**Triggers:**
- Pull requests only

**Features:**
- ✅ Builds all flavors
- ✅ Runs lint checks
- ✅ Runs unit tests
- ✅ Comments PR with results

---

## 🔧 Setup Instructions

### Step 1: Add Repository Secrets

Go to your GitHub repository: **Settings → Secrets and variables → Actions**

Add the following secrets:

| Secret Name | Description | Required |
|------------|-------------|----------|
| `POSTHOG_API_KEY` | PostHog API key for analytics | ✅ Yes |
| `POSTHOG_HOST` | PostHog host URL | ✅ Yes |
| `DEBUG_VECTORCAM_API_KEY` | API key for debug builds | ✅ Yes |
| `RELEASE_VECTORCAM_API_KEY` | API key for release builds | ⚠️ For release workflows |
| `FIREBASE_APP_ID` | Firebase App ID (for distribution) | 🔥 For Firebase |
| `FIREBASE_SERVICE_ACCOUNT` | Firebase service account JSON | 🔥 For Firebase |
| `KEYSTORE_BASE64` | Base64 encoded keystore (optional) | 📦 Optional |

#### How to Create Secrets

##### 1. PostHog Secrets
From your `secrets.properties` file:
```bash
# Copy values from secrets.properties
POSTHOG_API_KEY=your_posthog_api_key
POSTHOG_HOST=your_posthog_host
```

##### 2. VectorCam API Keys
```bash
DEBUG_VECTORCAM_API_KEY=your_debug_api_key
RELEASE_VECTORCAM_API_KEY=your_release_api_key
```

##### 3. Firebase Setup (Optional, for Distribution)

**Get Firebase App ID:**
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to Project Settings → General
4. Find App ID for your Android app

**Create Service Account:**
1. Go to Project Settings → Service Accounts
2. Click "Generate new private key"
3. Save the JSON file
4. Convert to secret:
```bash
cat firebase-service-account.json | base64 > firebase-base64.txt
# Copy content and add as FIREBASE_SERVICE_ACCOUNT secret
```

**Or use the JSON directly:**
```bash
# Just copy the entire JSON content as the secret value
cat firebase-service-account.json
```

##### 4. Keystore (Optional)
If you have a signing keystore:
```bash
base64 -i app/keystore.jks -o keystore-base64.txt
# Copy content and add as KEYSTORE_BASE64 secret
```

---

## 🚀 Usage

### Basic Build (build-debug.yml)

#### Automatic Triggers
Automatically runs on:
- Push to `main` or `develop`
- Pull requests

#### Manual Trigger
1. Go to **Actions** tab
2. Select "Build Debug APKs"
3. Click "Run workflow"
4. Select branch
5. Click "Run workflow"

### Advanced Distribution (build-and-distribute.yml)

#### Enable Firebase Distribution
Push to `main` branch automatically distributes to Firebase.

#### Manual Distribution
1. Go to **Actions** tab
2. Select "Build and Distribute Debug APKs"
3. Click "Run workflow"
4. Check "Distribute to Firebase"
5. Click "Run workflow"

### PR Validation (pr-check.yml)

Automatically runs on all pull requests. No manual action needed.

---

## 📦 Downloading APKs

### From Workflow Run

1. Go to **Actions** tab
2. Click on a workflow run
3. Scroll to **Artifacts** section
4. Download desired APK:
   - `VectorCam-Colombia-{build_number}-debug`
   - `VectorCam-Uganda-{build_number}-debug`
   - `VectorCam-Nigeria-{build_number}-debug`

### From Firebase App Distribution

Testers automatically receive notifications when new builds are available.

---

## 🧪 Test Results

Test results are uploaded as artifacts:
- `test-results-Colombia`
- `test-results-Uganda`
- `test-results-Nigeria`

Download and open HTML reports to view test details.

---

## 🔥 Firebase App Distribution Setup

### 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create new project or use existing
3. Add Android app for each flavor:
   - `com.vci.vectorcamapp.colombia`
   - `com.vci.vectorcamapp.uganda`
   - `com.vci.vectorcamapp.nigeria`

### 2. Enable App Distribution

1. In Firebase Console, go to **App Distribution**
2. Click "Get started"
3. Follow setup instructions

### 3. Create Tester Groups

1. In App Distribution, go to **Testers & Groups**
2. Create groups:
   - `colombia-testers`
   - `uganda-testers`
   - `nigeria-testers`
3. Add testers to respective groups

### 4. Configure Workflow

The workflow is already configured! Just add the secrets mentioned above.

---

## 📊 Build Matrix Strategy

The workflows use GitHub Actions matrix strategy to build all flavors in parallel:

```yaml
strategy:
  matrix:
    flavor: [Colombia, Uganda, Nigeria]
```

**Benefits:**
- ✅ Faster builds (parallel execution)
- ✅ Independent failure (one flavor fails doesn't stop others)
- ✅ Clear separation of artifacts

---

## 🎯 Customization

### Modify Triggers

**Run only on specific branches:**
```yaml
on:
  push:
    branches: [ main, staging, production ]
```

**Run on tags:**
```yaml
on:
  push:
    tags:
      - 'v*'
```

**Schedule builds:**
```yaml
on:
  schedule:
    - cron: '0 0 * * *' # Daily at midnight
```

### Add More Flavors

If you add a new flavor, just update the matrix:

```yaml
strategy:
  matrix:
    flavor: [Colombia, Uganda, Nigeria, Kenya] # Added Kenya
    include:
      - flavor: Kenya
        tester_groups: kenya-testers
```

### Modify Test Commands

Add more test types:

```yaml
- name: Run all tests
  run: |
    ./gradlew test${{ matrix.flavor }}DebugUnitTest
    ./gradlew connected${{ matrix.flavor }}DebugAndroidTest
    ./gradlew lint${{ matrix.flavor }}Debug
```

### Change Retention Days

```yaml
- name: Upload APK
  uses: actions/upload-artifact@v4
  with:
    retention-days: 90 # Changed from 30 to 90 days
```

---

## 🐛 Troubleshooting

### Build Fails: "secrets.properties not found"

**Solution:** Make sure all required secrets are added in GitHub repository settings.

### Firebase Distribution Fails

**Solutions:**
1. Check Firebase App ID is correct
2. Verify service account JSON is valid
3. Ensure tester groups exist in Firebase
4. Check app is registered in Firebase project

### Gradle Build Fails

**Solutions:**
1. Check JDK version (should be 17)
2. Verify `gradlew` has execute permissions
3. Look at detailed error in workflow logs
4. Test build locally first

### Out of Memory

**Solution:** Increase Gradle memory in workflow:

```yaml
- name: Build with more memory
  run: ./gradlew assemble${{ matrix.flavor }}Debug -Dorg.gradle.jvmargs="-Xmx4g"
```

### Artifacts Not Uploading

**Solution:** Check the path is correct:

```bash
# Verify path locally
ls -la app/build/outputs/apk/colombia/debug/
```

---

## 🔔 Notifications

### Slack Integration

Add Slack notifications:

```yaml
- name: Notify Slack
  if: always()
  uses: 8398a7/action-slack@v3
  with:
    status: ${{ job.status }}
    text: 'Build ${{ matrix.flavor }} ${{ job.status }}'
    webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

### Email Notifications

GitHub automatically sends emails to:
- Workflow author on failure
- Repository watchers (if configured)

---

## 📈 Advanced Features

### Code Coverage

Add coverage reporting:

```yaml
- name: Generate coverage report
  run: ./gradlew jacocoTestReport

- name: Upload coverage
  uses: codecov/codecov-action@v3
  with:
    files: ./app/build/reports/jacoco/test/jacocoTestReport.xml
```

### Static Analysis

Add Detekt or other static analysis:

```yaml
- name: Run Detekt
  run: ./gradlew detekt

- name: Upload Detekt results
  uses: github/codeql-action/upload-sarif@v2
  with:
    sarif_file: app/build/reports/detekt/detekt.sarif
```

### Performance Monitoring

Add build performance tracking:

```yaml
- name: Build with scan
  run: ./gradlew assemble${{ matrix.flavor }}Debug --scan
```

---

## 📚 Best Practices

### 1. **Use Gradle Build Cache**
Already configured in workflows with:
```yaml
cache: gradle
```

### 2. **Fail Fast Strategy**
Use for quick feedback:
```yaml
strategy:
  fail-fast: true # Stop all if one fails
```

Or continue all builds:
```yaml
strategy:
  fail-fast: false # Continue even if one fails
```

### 3. **Conditional Distribution**
Only distribute from `main` branch:
```yaml
if: github.ref == 'refs/heads/main'
```

### 4. **Version Tagging**
Automatically tag releases:
```yaml
- name: Create tag
  if: github.ref == 'refs/heads/main'
  run: |
    git tag "v$VERSION_NAME-build${{ github.run_number }}"
    git push origin --tags
```

---

## 🔍 Monitoring

### View Build Status

**Badge in README:**
```markdown
![Build Debug APKs](https://github.com/YOUR_USERNAME/vectorcamapp/workflows/Build%20Debug%20APKs/badge.svg)
```

**Check Workflow Runs:**
- Go to **Actions** tab
- View history and logs
- Filter by status, branch, or workflow

### Build Analytics

- Average build time
- Success rate
- Artifact sizes
- Test results trends

---

## 💡 Tips

1. **Test Locally First:** Run `./gradlew assembleDebug` locally before pushing
2. **Use Draft PRs:** Create draft PRs to test workflows without notifying team
3. **Check Logs:** Always check full logs if build fails
4. **Cache Dependencies:** Already configured, but can be optimized further
5. **Parallel Builds:** Matrix strategy runs flavors in parallel for speed

---

## 📋 Quick Command Reference

| Task | Command |
|------|---------|
| View workflows | Go to Actions tab |
| Manual trigger | Actions → Select workflow → Run workflow |
| Download APK | Workflow run → Artifacts → Download |
| View logs | Workflow run → Click job → View steps |
| Cancel run | Workflow run → Cancel workflow |
| Re-run failed | Workflow run → Re-run failed jobs |

---

## 🎉 Summary

You now have three workflows configured:

1. **`build-debug.yml`** - Basic builds for all flavors
2. **`build-and-distribute.yml`** - Advanced with Firebase distribution
3. **`pr-check.yml`** - PR validation

**Next Steps:**
1. Add repository secrets
2. Push code to trigger first build
3. Check Actions tab for results
4. Download APKs from artifacts
5. (Optional) Set up Firebase for distribution

---

**Last Updated:** February 6, 2026  
**Workflows Version:** 1.0  
**Maintained by:** VectorCam Team
