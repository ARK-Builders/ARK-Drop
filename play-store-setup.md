# Google Play Store Setup Guide

This guide walks you through setting up ARK Drop for Google Play Store release.

## Prerequisites

1. **Google Play Console Account**: You need a Google Play Console developer account ($25 one-time fee)
2. **Release Keystore**: A signing key for your app releases
3. **GitHub Repository**: With proper secrets configured

## Step 1: Create Release Keystore

If you don't have a release keystore, create one:

```bash
keytool -genkeypair \
  -alias drop-key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -keystore release-keystore.jks \
  -dname "CN=Your Name, OU=Your Organization, O=Your Company, L=Your City, ST=Your State, C=Your Country"
```

**Important**: Store this keystore file securely. You'll need it for all future releases.

## Step 2: Google Play Console Setup

### Create New App

1. Go to [Google Play Console](https://play.google.com/console)
2. Click "Create app"
3. Fill in app details:
    - **App name**: ARK Drop - Secure File Sharing
    - **Default language**: English (United States)
    - **App or game**: App
    - **Free or paid**: Free

### App Signing

1. Go to "Release" → "Setup" → "App signing"
2. Choose "Use Google Play App Signing" (recommended)
3. Upload your release keystore or let Google generate one

### Service Account for API Access

1. Go to "Setup" → "API access"
2. Click "Create new service account"
3. Follow the link to Google Cloud Console
4. Create a service account with these roles:
    - Service Account User
    - Service Account Token Creator
5. Create and download a JSON key file
6. Back in Play Console, grant access to the service account

## Step 3: Configure GitHub Secrets

Add these secrets to your GitHub repository:

### Required Secrets

```bash
# Keystore secrets
KEYSTORE_BASE64=<base64-encoded-keystore-file>
KEY_ALIAS=drop-key
KEY_PASSWORD=<your-key-password>
KEYSTORE_PASSWORD=<your-keystore-password>

# Play Store API
PLAY_STORE_CREDENTIALS=<service-account-json-content>
```

### Generate KEYSTORE_BASE64

```bash
# On Linux/Mac
base64 -i release-keystore.jks | tr -d '\n'

# On Windows
certutil -encode release-keystore.jks keystore.b64
# Then copy content without headers/footers
```

## Step 4: Prepare App Store Listing

### Store Listing

1. **App name**: ARK Drop - Secure File Sharing
2. **Short description**: Fast, secure file sharing between devices without internet
3. **Full description**: Use the content from `fastlane/metadata/android/en-US/full_description.txt`

### Graphics Assets Needed

Create these assets (you'll need to upload them manually):

- **App icon**: 512 x 512 px (PNG, no transparency)
- **Feature graphic**: 1024 x 500 px (JPG or PNG)
- **Screenshots**: At least 2, up to 8 (16:9 or 9:16 aspect ratio)
- **Phone screenshots**: 320-3840 px on longest side
- **Tablet screenshots** (optional): 320-3840 px on longest side

### Content Rating

1. Go to "Policy" → "App content"
2. Complete the content rating questionnaire
3. ARK Drop should receive an "Everyone" rating

### Privacy Policy

1. Upload the privacy policy from `PRIVACY.md`
2. Host it on your website or GitHub Pages
3. Add the URL to your app listing

## Step 5: Release Process

### Internal Testing (Recommended First)

1. **Create internal testing release**:
   ```bash
   git tag v1.0.0-internal
   git push origin v1.0.0-internal
   ```

2. **Or use workflow dispatch**:
    - Go to GitHub Actions
    - Select "Release to Google Play"
    - Choose "internal" track
    - Run workflow

3. **Add testers**:
    - Go to "Testing" → "Internal testing"
    - Add email addresses of testers
    - Share the testing link

### Production Release

After internal testing:

1. **Create production release**:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

2. **Or use workflow dispatch** with "production" track

3. **Review and publish** in Play Console

## Step 6: Post-Release

### Monitor Release

1. **Check for crashes**: "Quality" → "Android vitals"
2. **Monitor reviews**: "Ratings and reviews"
3. **Track installs**: "Statistics" → "Installs"

### Update Process

For future updates:

1. Update version in code
2. Create new tag: `git tag v1.1.0`
3. Push tag: `git push origin v1.1.0`
4. GitHub Actions will automatically build and upload

## Troubleshooting

### Common Issues

1. **Keystore errors**: Ensure all keystore secrets are correct
2. **API access denied**: Check service account permissions
3. **Upload failed**: Verify app signing configuration
4. **Version conflicts**: Ensure version codes are incrementing

### Debug Steps

1. Check GitHub Actions logs
2. Verify all secrets are set
3. Test keystore locally:
   ```bash
   ./gradlew assembleRelease
   ```

### Support

- GitHub Issues: [Create an issue](https://github.com/ARK-Builders/ARK-Drop/issues)

## Security Best Practices

1. **Never commit keystores** to version control
2. **Use different keystores** for debug and release
3. **Backup your keystore** securely
4. **Rotate service account keys** periodically
5. **Monitor API usage** in Google Cloud Console

## Compliance

Ensure your app complies with:

- Google Play Developer Policy
- Android App Bundle requirements
- Target API level requirements
- Privacy and data handling policies
