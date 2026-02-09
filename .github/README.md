# VectorCam GitHub Actions CI/CD

Automated build and distribution system for VectorCam Android app with multi-flavor support.

---

## 🎯 What You Get

- ✅ **Automated builds** for 3 regional flavors (Colombia, Uganda, Nigeria)
- ✅ **Parallel execution** - all flavors build simultaneously
- ✅ **Test automation** - unit tests run on every build
- ✅ **Artifact storage** - APKs available for 30 days
- ✅ **PR validation** - automatic checks on pull requests
- ✅ **Firebase distribution** - optional automated beta distribution
- ✅ **Build summaries** - comprehensive reports and PR comments

---

## 📦 Files Created

```
.github/workflows/
├── build-debug.yml              # Simple debug builds
├── build-and-distribute.yml     # Advanced with Firebase
└── pr-check.yml                 # PR validation

Documentation:
├── CI_CD_QUICKSTART.md          # Quick start guide (START HERE)
├── GITHUB_ACTIONS_SETUP.md      # Detailed setup instructions
└── CI_CD_ARCHITECTURE.txt       # Visual diagrams

Scripts:
└── setup-github-secrets.sh      # Helper script for secrets setup
```

---

## 🚀 Quick Start (3 Steps)

### 1. Add GitHub Secrets

**Using the helper script (recommended):**
```bash
chmod +x setup-github-secrets.sh
./setup-github-secrets.sh
```

**Or manually** in GitHub: Settings → Secrets → Actions

Required secrets from your `secrets.properties`:
- `POSTHOG_API_KEY`
- `POSTHOG_HOST`
- `DEBUG_VECTORCAM_API_KEY`
- `RELEASE_VECTORCAM_API_KEY`

### 2. Push Your Code

```bash
git add .github/
git commit -m "Add GitHub Actions CI workflows"
git push origin main
```

### 3. Watch Builds

Go to **Actions** tab in GitHub and watch the magic happen! ✨

---

## 📊 Workflows Overview

| Workflow | Purpose | Triggers | Time |
|----------|---------|----------|------|
| **build-debug.yml** | Simple builds + tests | Push, PR, Manual | ~5-7 min |
| **build-and-distribute.yml** | Full CI/CD + Firebase | Push, PR, Manual | ~7-10 min |
| **pr-check.yml** | Fast PR validation | PR only | ~5-8 min |

---

## 📥 Download APKs

After build completes:

1. Go to **Actions** tab
2. Click on workflow run
3. Scroll to **Artifacts**
4. Download:
   - `VectorCam-Colombia-{build#}-debug`
   - `VectorCam-Uganda-{build#}-debug`
   - `VectorCam-Nigeria-{build#}-debug`

---

## 🔥 Firebase Distribution (Optional)

To enable automatic distribution to testers:

1. Set up Firebase App Distribution
2. Add secrets:
   - `FIREBASE_APP_ID`
   - `FIREBASE_SERVICE_ACCOUNT`
3. Create tester groups:
   - `colombia-testers`
   - `uganda-testers`
   - `nigeria-testers`

See `GITHUB_ACTIONS_SETUP.md` for detailed Firebase setup.

---

## 🎮 Manual Triggers

Trigger builds manually:

1. Go to **Actions** tab
2. Select workflow
3. Click "Run workflow"
4. Select branch
5. Click "Run workflow"

Or use GitHub CLI:
```bash
gh workflow run build-debug.yml
```

---

## 📚 Documentation

- **[CI_CD_QUICKSTART.md](CI_CD_QUICKSTART.md)** - Start here! Quick setup guide
- **[GITHUB_ACTIONS_SETUP.md](GITHUB_ACTIONS_SETUP.md)** - Detailed instructions
- **[CI_CD_ARCHITECTURE.txt](CI_CD_ARCHITECTURE.txt)** - Visual diagrams

---

## 🧪 What Gets Tested

Each build runs:
- ✅ Unit tests for all flavors
- ✅ Lint checks (in pr-check workflow)
- ✅ Build verification
- ✅ APK generation

Test results uploaded as artifacts.

---

## 🎯 Build Matrix

Builds run in parallel for speed:

```yaml
strategy:
  matrix:
    flavor: [Colombia, Uganda, Nigeria]
```

**Result:** All 3 flavors build simultaneously in ~5-10 minutes total.

---

## 💬 PR Comments

Pull requests automatically get commented with:
- ✅ Build status
- ✅ APK size
- ✅ Version info
- ✅ Download links

---

## 🐛 Troubleshooting

### Build fails with "secrets.properties not found"
→ Add all required secrets to GitHub repository settings

### Workflow doesn't appear in Actions tab
→ Check files are in `.github/workflows/` and YAML is valid

### Firebase distribution fails
→ Verify Firebase credentials and tester groups exist

See `GITHUB_ACTIONS_SETUP.md` for more troubleshooting tips.

---

## ⚙️ Customization

All workflows are customizable. Common changes:

**Build only one flavor:**
```yaml
strategy:
  matrix:
    flavor: [Colombia]  # Just Colombia
```

**Change artifact retention:**
```yaml
retention-days: 90  # Keep for 90 days instead of 30
```

**Add Slack notifications:**
```yaml
- uses: 8398a7/action-slack@v3
  with:
    webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

---

## 📈 What Gets Built

For each push/PR, you get:

**APKs (3 flavors):**
- `app-colombia-debug.apk` - Version 1.0.6 (1006)
- `app-uganda-debug.apk` - Version 1.0.4 (2004)
- `app-nigeria-debug.apk` - Version 1.0.1 (3001)

**Artifacts:**
- Debug APKs for all flavors
- Test results
- Lint reports (pr-check)
- Build summaries

---

## ✅ Success Indicators

You'll know it's working when:

- ✅ Green checkmarks in Actions tab
- ✅ APKs available in Artifacts
- ✅ PR comments appear automatically
- ✅ Tests pass
- ✅ Build time < 10 minutes
- ✅ Firebase sends notifications (if enabled)

---

## 🎊 Features

### Automated
- ✅ Builds on every push
- ✅ PR validation
- ✅ Test execution
- ✅ Artifact uploads

### Parallel
- ✅ All flavors build simultaneously
- ✅ Independent failure handling
- ✅ Faster total build time

### Reporting
- ✅ Build summaries
- ✅ PR comments
- ✅ Test results
- ✅ Changelog generation

### Distribution
- ✅ GitHub Artifacts (always)
- ✅ Firebase App Distribution (optional)
- ✅ Tester group support

---

## 💡 Tips

1. **Test locally first**: Run `./gradlew assembleDebug` before pushing
2. **Use draft PRs**: Test workflows without notifying team
3. **Monitor build times**: Optimize if exceeds 10 minutes
4. **Keep secrets updated**: Rotate API keys regularly
5. **Clean artifacts**: Delete old builds to save storage

---

## 🔗 Related Documentation

- [BUILD_FLAVORS.md](BUILD_FLAVORS.md) - Product flavors guide
- [FLAVOR_MIGRATION_SUMMARY.md](FLAVOR_MIGRATION_SUMMARY.md) - Migration from gradle properties
- [NAVIGATION_README.md](app/src/main/java/com/vci/vectorcamapp/navigation/NAVIGATION_README.md) - Navigation refactoring

---

## 📊 Cost

**GitHub Actions (Free tier):**
- Public repos: Unlimited minutes
- Private repos: 2,000 minutes/month
- Storage: 500 MB

**Your usage:**
- Per build: ~30 minutes (all 3 flavors)
- Per month (daily builds): ~900 minutes
- Well within free tier limits! ✅

---

## 🎯 Recommendations

**For most projects, use:**
1. `pr-check.yml` - Fast PR validation
2. `build-and-distribute.yml` - Main branch builds

**Skip:**
- `build-debug.yml` if using `build-and-distribute.yml`

**Enable:**
- Firebase distribution for organized beta testing
- Slack notifications for team alerts

---

## 📞 Support

**Issues with workflows:**
1. Check workflow logs in Actions tab
2. Verify secrets are set
3. Test build locally
4. See troubleshooting in `GITHUB_ACTIONS_SETUP.md`

**Questions about setup:**
- Read `CI_CD_QUICKSTART.md`
- Check `GITHUB_ACTIONS_SETUP.md`
- Review workflow YAML comments

---

## 🎉 Summary

**What you achieved:**
- ✅ Full CI/CD pipeline for 3 flavors
- ✅ Automated testing on every push
- ✅ PR validation workflow
- ✅ APK artifacts with 30-day retention
- ✅ Optional Firebase distribution
- ✅ Comprehensive build reporting
- ✅ Parallel builds for speed
- ✅ Well-documented setup

**Next steps:**
1. Add secrets to GitHub
2. Push code to trigger first build
3. Check Actions tab for results
4. Download APKs from artifacts
5. (Optional) Configure Firebase

**Total setup time:** ~10 minutes  
**Build time per push:** ~5-10 minutes  
**Flavors supported:** 3 (Colombia, Uganda, Nigeria)  
**Cost:** Free (within limits)  

---

🚀 **Ready to build!** Push your code and watch the automation work!

---

**Created:** February 6, 2026  
**Version:** 1.0  
**Status:** ✅ Production Ready
