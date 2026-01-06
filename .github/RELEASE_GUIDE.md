# GitHub Actions Guide

This project uses GitHub Actions for automatic building and releasing.

## Workflows

### 1. Build Workflow (build.yml)

**Triggers:**
- Push to `main`, `master`, or `develop` branches
- Pull requests to these branches

**What it does:**
- Builds on Linux, Windows, and macOS
- Runs tests
- Uploads JAR artifacts

### 2. Release Workflow (release.yml)

**Triggers:**
- Push tags starting with `v` (e.g., `v1.0.0`, `v2.1.3`)

**What it does:**
- Builds native executables for all platforms
- Creates GitHub Release
- Uploads all artifacts automatically

## How to Create a Release

### Step 1: Commit your changes

```bash
git add .
git commit -m "Release version 1.0.0"
git push origin main
```

### Step 2: Create and push a tag

```bash
# Create a tag
git tag v1.0.0

# Push the tag
git push origin v1.0.0
```

### Step 3: Wait for GitHub Actions

The workflow will automatically:
1. Build for Linux (Ubuntu runner)
2. Build for Windows (Windows runner)
3. Build for macOS (macOS runner)
4. Create a GitHub Release
5. Upload all packages to the release

### Step 4: Check the release

Go to: `https://github.com/YOUR_USERNAME/sim800l-manager/releases`

You'll see:
- ✅ Linux: `SIM800L-Manager-linux-x64.tar.gz`
- ✅ Windows: `SIM800L-Manager-windows-x64.zip`
- ✅ macOS: `SIM800L-Manager-macos-x64.dmg`

## Version Numbering

Use semantic versioning:
- `v1.0.0` - Major.Minor.Patch
- `v1.0.1` - Bug fix
- `v1.1.0` - New feature
- `v2.0.0` - Breaking changes

## Examples

### Release version 1.0.0

```bash
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

### Release version 1.0.1 (bug fix)

```bash
git tag -a v1.0.1 -m "Fix serial port connection bug"
git push origin v1.0.1
```

### Delete a tag (if needed)

```bash
# Delete local tag
git tag -d v1.0.0

# Delete remote tag
git push origin :refs/tags/v1.0.0
```

## Workflow Status

Check workflow status at:
`https://github.com/YOUR_USERNAME/sim800l-manager/actions`

## Troubleshooting

### Workflow fails

1. Check the Actions tab for error logs
2. Common issues:
   - Missing dependencies
   - Test failures
   - Build errors

### Release not created

1. Make sure tag starts with `v`
2. Check GITHUB_TOKEN permissions
3. Verify tag was pushed: `git push origin v1.0.0`

### Artifacts not uploaded

1. Check build logs
2. Verify file paths in workflow
3. Ensure jpackage succeeded

## Build Times

Approximate build times:
- Linux: ~5 minutes
- Windows: ~7 minutes
- macOS: ~8 minutes
- Total: ~20 minutes

## Costs

GitHub Actions is free for:
- Public repositories: Unlimited minutes
- Private repositories: 2,000 minutes/month (free tier)

## Manual Release (Alternative)

If you prefer manual releases:

1. Build locally for each platform
2. Go to GitHub > Releases > Create Release
3. Upload files manually
4. Write release notes

But automated is better! 🚀

## Tips

1. **Test before tagging**: Make sure code works
2. **Write release notes**: Update CHANGELOG.md
3. **Version carefully**: Don't skip versions
4. **Check workflows**: Review Actions tab regularly

## Example Release Process

```bash
# 1. Update version in code
# 2. Update CHANGELOG.md
# 3. Commit changes
git add .
git commit -m "Prepare release v1.0.0"
git push origin main

# 4. Create and push tag
git tag -a v1.0.0 -m "Release v1.0.0 - Initial release"
git push origin v1.0.0

# 5. Wait ~20 minutes
# 6. Check: https://github.com/YOUR_USERNAME/sim800l-manager/releases
# 7. Done! ✅
```

---

Happy releasing! 🎉
