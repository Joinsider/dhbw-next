# AUR Package Guide

This document explains how to install and maintain the DHBW Horb Studenten App via the Arch User Repository (AUR).

## Installation for Arch Linux Users

### Using an AUR Helper (Recommended)

If you use an AUR helper like `yay`, `paru`, or `pamac`:

```bash
yay -S dhbw-next
```

Or with `paru`:

```bash
paru -S dhbw-next
```

### Manual Installation

1. Clone the AUR package repository:
   ```bash
   git clone https://aur.archlinux.org/dhbw-next.git
   cd dhbw-next
   ```

2. Build and install:
   ```bash
   makepkg -si
   ```

## Running the Application

After installation, you can run the application by:

- Launching it from your application menu (it will appear as "DHBW Horb Studenten App")
- Running `dhbw-next` from the terminal

## Updating

To update to the latest version:

```bash
yay -Syu dhbw-next
```

Or manually:
```bash
cd dhbw-next
git pull
makepkg -si
```

## For Maintainers

### Prerequisites

To publish updates to AUR, you need:

1. An AUR account (register at https://aur.archlinux.org)
2. SSH key added to your AUR account
3. The following GitHub secrets configured in the repository:
   - `AUR_USERNAME`: Your AUR username
   - `AUR_EMAIL`: Your AUR email
   - `AUR_SSH_PRIVATE_KEY`: Your SSH private key for AUR

### Automatic Publishing

The package is automatically published to AUR when a new release is created on GitHub. The workflow:

1. Triggers on GitHub releases
2. Downloads the release tarball
3. Calculates SHA256 checksum
4. Updates PKGBUILD and .SRCINFO
5. Publishes to AUR

### Manual Publishing

To manually trigger a publish:

1. Go to Actions → Publish to AUR
2. Click "Run workflow"
3. Enter the release tag (e.g., `v1.0.4`)
4. Click "Run workflow"

### Testing the Package Locally

Before publishing, you can test the PKGBUILD locally on an Arch Linux system:

```bash
# From the repository root
makepkg -f
```

This will build the package without installing it. Check for any errors in the build process.

### Updating the Package

When releasing a new version:

1. Update `versionCode` and `versionName` in `composeApp/build.gradle.kts`
2. Create a git tag with the version (e.g., `v1.0.5`)
3. Push the tag: `git push origin v1.0.5`
4. Create a GitHub release
5. The AUR package will be automatically updated

### Manual PKGBUILD Updates

If you need to manually update the PKGBUILD:

1. Edit the `PKGBUILD` file
2. Update `pkgver` and `pkgrel` as needed
3. Generate new `.SRCINFO`:
   ```bash
   makepkg --printsrcinfo > .SRCINFO
   ```
4. Commit and push to AUR:
   ```bash
   git add PKGBUILD .SRCINFO
   git commit -m "Update to version X.Y.Z"
   git push
   ```

## Troubleshooting

### Build Fails

If the build fails, check:

1. You have the required dependencies: `jdk11-openjdk` and `gradle`
2. You have enough disk space
3. The release tarball is accessible

### Application Won't Start

If installed but won't start:

1. Check Java is installed: `java -version`
2. Try running from terminal to see error messages: `dhbw-next`
3. Check file permissions in `/opt/dhbw-next/`

## Resources

- AUR Package: https://aur.archlinux.org/packages/dhbw-next
- GitHub Repository: https://github.com/Joinsider/dhbw-next
- AUR Guidelines: https://wiki.archlinux.org/title/AUR_submission_guidelines
