# Codemagic CI/CD Setup Guide

This guide explains how to set up automated debug builds using Codemagic cloud CI/CD.

## Files Included

- `codemagic.yaml` - Main workflow configuration
- `gradlew` - Unix Gradle wrapper script (auto-downloads gradle-wrapper.jar)
- `gradlew.bat` - Windows Gradle wrapper script
- `gradle/wrapper/gradle-wrapper.properties` - Gradle version configuration
- `gradle.properties` - Optimized build settings for cloud builds

## Codemagic Setup Steps

### 1. Sign Up & Connect Repository

1. Go to [codemagic.io](https://codemagic.io)
2. Sign up with your GitHub/GitLab/Bitbucket account
3. Connect your Cover app repository

### 2. Configure Environment Variables

In Codemagic dashboard, go to **App Settings > Environment variables** and add:

| Variable Name | Value | Required |
|--------------|-------|----------|
| `GOOGLE_SERVICES_JSON` | Paste entire content of `google-services.json` | ✅ Yes |
| `CM_EMAIL` | Your email for build notifications | Optional |

**How to set GOOGLE_SERVICES_JSON:**
```bash
# On your local machine with the google-services.json file:
cat app/google-services.json | pbcopy  # macOS
# OR
cat app/google-services.json | clip     # Windows
# OR
cat app/google-services.json            # Copy manually
```
Then paste into the Codemagic environment variable field.

### 3. Trigger Builds

Builds will automatically trigger on:
- Push to `main`, `develop`, or `feature/*` branches
- Pull requests
- Manual trigger from Codemagic dashboard

### 4. Download APK

After successful build:
1. Go to the build details page
2. Click on **Artifacts** tab
3. Download `app-debug.apk`

## Workflow Features

### Debug Build (`cover-debug-build`)

- ✅ Clean build environment
- ✅ Downloads Gradle wrapper automatically
- ✅ Optimized Gradle settings (4GB heap, parallel builds)
- ✅ Firebase configuration from environment variable
- ✅ Debug APK artifact upload
- ✅ Email notifications on success/failure

### Nightly Build (`cover-nightly`)

- ⏰ Runs daily at 2 AM UTC
- 🔄 Same as debug build but scheduled
- 📧 Email notifications

## Build Optimization Settings

The `gradle.properties` includes:
- **4GB JVM heap** - Prevents OOM on large builds
- **Parallel builds** - Faster compilation
- **Build caching** - Reuses previous build outputs
- **Non-transitive R classes** - Faster builds
- **Disabled unused features** - Aid, Renderscript, Shaders

## Troubleshooting

### "google-services.json not found"
- Ensure `GOOGLE_SERVICES_JSON` environment variable is set in Codemagic
- Check the JSON content is valid (no extra whitespace)

### "Gradle wrapper not found"
- The workflow downloads it automatically
- Check network connectivity in build logs

### "Out of memory" errors
- Already configured for 4GB heap in `gradle.properties`
- Codemagic provides 8GB RAM on standard instances

### "Firebase configuration invalid"
- Verify `google-services.json` content is complete
- Check `package_name` matches your app ID (`com.cover.app`)

## Manual Build Trigger

You can also trigger builds manually via Codemagic CLI:

```bash
# Install codemagic CLI
npm install -g @codemagic/cli

# Login
codemagic login

# Trigger build
codemagic build --workflow cover-debug-build
```

## Build Output

Successful builds produce:
- `app/build/outputs/apk/debug/app-debug.apk`
- Build logs with full stacktrace
- Gradle scan (if enabled)

## Security Notes

- `google-services.json` is stored securely in Codemagic environment variables
- Debug builds use Android default debug keystore
- No sensitive credentials are committed to repository

## Support

- [Codemagic Documentation](https://docs.codemagic.io/)
- [Codemagic Slack Community](https://slack.codemagic.io/)
