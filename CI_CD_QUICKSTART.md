# GitHub Actions CI/CD - Quick Start

Successfully set up GitHub Actions workflows for automated builds and distribution of VectorCam debug APKs for all flavors!

---

## ✅ What Was Created

### Workflow Files

1. **`.github/workflows/build-debug.yml`**
   - **Purpose:** Simple debug builds for all flavors
   - **Triggers:** Push to main/develop, PRs, manual
   - **Output:** Debug APKs + test results
   - **Time:** ~5-10 minutes

2. **`.github/workflows/build-and-distribute.yml`**
   - **Purpose:** Full build with Firebase App Distribution
   - **Triggers:** Push to main/develop, PRs, manual
   - **Features:** 
     - Build all flavors
     - Run tests
     - Generate changelogs
     - Distribute to Firebase
     - PR comments
     - Build summaries
   - **Time:** ~5-10 minutes + distribution

3. **`.github/workflows/pr-check.yml`**
   - **Purpose:** Fast PR validation
   - **Triggers:** Pull requests only
   - **Features:**
     - Build verification
     - Lint checks
     - Unit tests
     - PR status comments
   - **Time:** ~5-8 minutes

### Documentation

4. **`GITHUB_ACTIONS_SETUP.md`**
   - Complete setup guide
   - Secrets configuration
   - Usage instructions
   - Troubleshooting
   - Customization examples

5. **`setup-github-secrets.sh`**
   - Helper script to set up GitHub secrets
   - Interactive wizard
   - Uses GitHub CLI

---

## 🚀 Quick Start (3 Steps)

### Step 1: Add Repository Secrets

Go to: **GitHub Repository → Settings → Secrets and variables → Actions**

#### Required Secrets (from your `secrets.properties`):

```bash
POSTHOG_API_KEY         → Your PostHog API key
POSTHOG_HOST            → Your PostHog host URL
DEBUG_VECTORCAM_API_KEY → Debug build API key
RELEASE_VECTORCAM_API_KEY → Release build API key
```

#### Using the Helper Script:

```bash
# Make script executable
chmod +x setup-github-secrets.sh

# Run setup wizard
./setup-github-secrets.sh
```

#### Manual Setup:

**In GitHub:**
1. Go to **Settings → Secrets → Actions**
2. Click **"New repository secret"**
3. Add each secret:
   - Name: `POSTHOG_API_KEY`
   - Value: [Copy from secrets.properties]
4. Repeat for all secrets

### Step 2: Push Your Code

```bash
git add .github/
git commit -m "Add GitHub Actions CI workflows"
git push origin main
```

### Step 3: Watch the Magic! ✨

1. Go to **Actions** tab in GitHub
2. See workflows running
3. Wait for builds to complete (~5-10 min)
4. Download APKs from **Artifacts**

---

## 📦 What Gets Built

After each push, you'll get **3 debug APKs**:

| Artifact Name | Flavor | Package ID |
|--------------|--------|------------|
| `VectorCam-Colombia-{build#}-debug` | Colombia | `com.vci.vectorcamapp.colombia` |
| `VectorCam-Uganda-{build#}-debug` | Uganda | `com.vci.vectorcamapp.uganda` |
| `VectorCam-Nigeria-{build#}-debug` | Nigeria | `com.vci.vectorcamapp.nigeria` |

Plus:
- Test results for each flavor
- Lint reports
- Build summaries

---

## 🎯 Workflow Selection Guide

### Use `build-debug.yml` when:
- ✅ You just need APKs built
- ✅ Simple CI setup
- ✅ No distribution needed

### Use `build-and-distribute.yml` when:
- ✅ You want Firebase App Distribution
- ✅ Need comprehensive reporting
- ✅ Want PR comments with build info
- ✅ Production-ready CI/CD

### Use `pr-check.yml` when:
- ✅ Fast feedback on PRs
- ✅ Lint + test validation
- ✅ Lightweight checks only

**Recommendation:** Use all three! They work together:
- `pr-check.yml` → Fast PR validation
- `build-and-distribute.yml` → Main branch builds + distribution
- `build-debug.yml` → Fallback simple builds

---

## 🔥 Firebase App Distribution (Optional)

### Benefits
- 📱 Automatic tester notifications
- 📊 Download analytics
- 👥 Organized tester groups
- 🚀 Easy beta distribution

### Setup

1. **Create Firebase Project:**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create or select project
   - Add 3 Android apps (one per flavor)

2. **Enable App Distribution:**
   - In Firebase, go to **App Distribution**
   - Click "Get started"

3. **Create Tester Groups:**
   ```
   colombia-testers → For Colombia flavor
   uganda-testers   → For Uganda flavor
   nigeria-testers  → For Nigeria flavor
   ```

4. **Get Firebase Credentials:**
   
   **App ID:**
   - Firebase Console → Project Settings → General
   - Copy App ID
   
   **Service Account:**
   - Project Settings → Service Accounts
   - Click "Generate new private key"
   - Download JSON file

5. **Add to GitHub Secrets:**
   ```bash
   FIREBASE_APP_ID → From step 4
   FIREBASE_SERVICE_ACCOUNT → Content of JSON file from step 4
   ```

6. **Done!** Builds pushed to `main` will auto-distribute.

---

## 📥 Downloading APKs

### From GitHub Actions

1. Go to **Actions** tab
2. Click on workflow run
3. Scroll to **Artifacts** section
4. Click to download:
   - `VectorCam-Colombia-{build}-debug.zip`
   - `VectorCam-Uganda-{build}-debug.zip`
   - `VectorCam-Nigeria-{build}-debug.zip`
5. Extract and install APK

### From Firebase (if configured)

Testers receive email/notification with download link automatically.

---

## 🎮 Manual Triggers

Sometimes you want to trigger builds manually:

### Via GitHub UI

1. Go to **Actions** tab
2. Select workflow (e.g., "Build Debug APKs")
3. Click **"Run workflow"** button
4. Select branch
5. (For advanced workflow) Choose options
6. Click **"Run workflow"**

### Via GitHub CLI

```bash
# Trigger simple build
gh workflow run build-debug.yml

# Trigger with distribution
gh workflow run build-and-distribute.yml

# Trigger PR check
gh workflow run pr-check.yml
```

---

## 📊 Build Matrix Strategy

Workflows use parallel builds for speed:

```yaml
strategy:
  matrix:
    flavor: [Colombia, Uganda, Nigeria]
```

**Result:**
- ✅ All 3 flavors build simultaneously
- ✅ Faster total build time (~5 min vs ~15 min sequential)
- ✅ Independent failure (one flavor fails ≠ all fail)

---

## 🧪 Testing Integration

Each workflow runs:

```bash
# Unit tests
./gradlew test{Flavor}DebugUnitTest

# Lint checks (pr-check only)
./gradlew lint{Flavor}Debug
```

Test results uploaded as artifacts:
- `test-results-Colombia`
- `test-results-Uganda`
- `test-results-Nigeria`

---

## 💬 PR Comments

When you create a pull request, `build-and-distribute.yml` automatically comments:

```
## 📱 Colombia Debug APK Built Successfully!

**Version:** 1.0.6 (1006)
**Size:** 25.3 MB
**Build:** #42

⬇️ Download from the **Artifacts** section in this workflow run.
```

Same for all 3 flavors!

---

## 🐛 Common Issues & Solutions

### Build Fails: "secrets.properties not found"

**Cause:** Missing GitHub secrets

**Fix:**
```bash
# Add all secrets to GitHub repository settings
# Or use the setup script:
./setup-github-secrets.sh
```

### Build Fails: "Permission denied: gradlew"

**Cause:** gradlew not executable

**Fix:**
```bash
git add --chmod=+x gradlew
git commit -m "Make gradlew executable"
git push
```

### Firebase Distribution Fails

**Cause:** Missing or invalid Firebase credentials

**Fix:**
1. Verify `FIREBASE_APP_ID` is correct
2. Check `FIREBASE_SERVICE_ACCOUNT` JSON is valid
3. Ensure tester groups exist in Firebase
4. Verify app is registered in Firebase

### Workflow Not Appearing

**Cause:** Workflow file syntax error or not in correct location

**Fix:**
```bash
# Check files exist
ls -la .github/workflows/

# Validate YAML syntax
yamllint .github/workflows/*.yml

# Push again
git push
```

---

## 🎨 Customization Examples

### Build Only One Flavor

Modify the matrix:

```yaml
strategy:
  matrix:
    flavor: [Colombia] # Only Colombia
```

### Add Slack Notifications

```yaml
- name: Notify Slack
  uses: 8398a7/action-slack@v3
  with:
    status: ${{ job.status }}
    webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

### Change Artifact Retention

```yaml
- name: Upload APK
  uses: actions/upload-artifact@v4
  with:
    retention-days: 90 # Keep for 90 days instead of 30
```

### Add Code Coverage

```yaml
- name: Generate coverage
  run: ./gradlew jacocoTestReport

- name: Upload to Codecov
  uses: codecov/codecov-action@v3
```

---

## 📈 CI/CD Pipeline Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Push to main/develop                     │
└────────────────────────┬────────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
        ▼                ▼                ▼
   ┌─────────┐      ┌─────────┐    ┌─────────┐
   │Colombia │      │ Uganda  │    │ Nigeria │
   │  Build  │      │  Build  │    │  Build  │
   └────┬────┘      └────┬────┘    └────┬────┘
        │                │                │
        ▼                ▼                ▼
   ┌─────────┐      ┌─────────┐    ┌─────────┐
   │  Tests  │      │  Tests  │    │  Tests  │
   └────┬────┘      └────┬────┘    └────┬────┘
        │                │                │
        ▼                ▼                ▼
   ┌─────────┐      ┌─────────┐    ┌─────────┐
   │  APK    │      │  APK    │    │  APK    │
   │ Upload  │      │ Upload  │    │ Upload  │
   └────┬────┘      └────┬────┘    └────┬────┘
        │                │                │
        └────────────────┼────────────────┘
                         │
                         ▼
              ┌──────────────────┐
              │ Firebase Distrib │
              │  (if enabled)    │
              └──────────────────┘
```

---

## 🎯 Best Practices

### 1. **Always Test Locally First**
```bash
# Before pushing, test locally
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

### 2. **Use Draft PRs for Testing**
Create draft PRs to test workflows without notifying team.

### 3. **Monitor Build Times**
Check Actions tab for build time trends. Optimize if > 10 minutes.

### 4. **Review Artifacts Regularly**
Clean up old artifacts to save storage space.

### 5. **Keep Secrets Updated**
When rotating API keys, update GitHub secrets.

---

## 📋 Checklist

Setup:
- [ ] Added all required secrets to GitHub
- [ ] Pushed workflow files to repository
- [ ] (Optional) Configured Firebase App Distribution
- [ ] Tested workflow with manual trigger

Verification:
- [ ] First build completed successfully
- [ ] All 3 flavor APKs generated
- [ ] Tests passed
- [ ] Artifacts available for download
- [ ] (Optional) Firebase distribution working

---

## 🎉 Success Indicators

You'll know it's working when:

✅ Green checkmarks in Actions tab  
✅ APKs appear in Artifacts section  
✅ PR comments show build status  
✅ Firebase sends notifications (if enabled)  
✅ Test results show passing  
✅ Build completes in < 10 minutes  

---

## 📚 Additional Resources

- **Full Documentation:** `GITHUB_ACTIONS_SETUP.md`
- **Workflow Files:** `.github/workflows/`
- **GitHub Actions Docs:** https://docs.github.com/en/actions
- **Firebase App Distribution:** https://firebase.google.com/docs/app-distribution

---

## 🤝 Support

### Workflow Issues
1. Check workflow logs in Actions tab
2. Verify all secrets are set correctly
3. Test build locally
4. Check `GITHUB_ACTIONS_SETUP.md` for troubleshooting

### Firebase Issues
1. Verify Firebase project setup
2. Check service account permissions
3. Ensure tester groups exist
4. Test Firebase CLI locally

---

## 🎊 Summary

You now have a complete CI/CD pipeline that:

✅ **Builds** all 3 flavor debug APKs automatically  
✅ **Tests** each flavor independently  
✅ **Uploads** APKs as downloadable artifacts  
✅ **Comments** on PRs with build status  
✅ **Distributes** to Firebase (optional)  
✅ **Runs in parallel** for speed  
✅ **Provides** comprehensive build summaries  

**Next Steps:**
1. Add GitHub secrets (required)
2. Push code to trigger first build
3. Download APKs from Actions → Artifacts
4. (Optional) Set up Firebase for distribution
5. Celebrate! 🎉

**Build Time:** ~5-10 minutes per push  
**Artifacts:** 3 debug APKs + test results  
**Cost:** Free (within GitHub Actions limits)  

---

**Created:** February 6, 2026  
**Workflows:** 3  
**Flavors Supported:** Colombia, Uganda, Nigeria  
**Status:** ✅ Ready to use
