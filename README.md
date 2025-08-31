# ARK Drop - Secure File Sharing

ARK Drop is a secure, peer-to-peer file sharing application for Android that allows you to transfer files between devices over the internet.

## Features

- 🔒 **Secure & Private**: End-to-end encryption for all transfers
- 📱 **Easy to Use**: Simple interface with QR code and link sharing
- ⚡ **Fast & Reliable**: High-speed internet transfers
- 🎨 **Customizable**: Profile management with custom avatars
- 📊 **Transfer History**: Track all your file transfers
- 🌐 **Internet-based**: Works anywhere with internet connection

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or later
- JDK 11 or later
- Android SDK API 29 or later

### Building the App

1. Clone the repository:
```bash
git clone https://github.com/your-username/drop-android.git
cd drop-android
```

2. Open the project in Android Studio

3. Build and run:
```bash
./gradlew assembleDebug
```

### Release Build

To create a release build:

```bash
./gradlew assembleRelease
```

## Google Play Store Release

### Setup

1. **Create a release keystore** (if you don't have one):
```bash
keytool -genkeypair -alias drop-key -keyalg RSA -keysize 2048 -validity 10000 -keystore release-keystore.jks
```

2. **Set up GitHub Secrets**:
   - `KEYSTORE_BASE64`: Base64 encoded keystore file
   - `KEY_ALIAS`, `KEY_PASSWORD`, `KEYSTORE_PASSWORD`: Keystore credentials
   - `PLAY_STORE_CREDENTIALS`: Google Play Console service account JSON

3. **Google Play Console Setup**:
   - Create a new app in Google Play Console
   - Set up app signing
   - Create a service account for API access
   - Download the service account JSON file

### Release Process

#### Automated Release (Recommended)

1. **Create a release tag**:
```bash
git tag v1.0.0
git push origin v1.0.0
```

2. **Manual workflow dispatch**:
   - Go to GitHub Actions
   - Select "Release to Google Play"
   - Choose the release track (internal/alpha/beta/production)
   - Run workflow

#### Manual Release

1. **Build release bundle**:
```bash
./gradlew bundleRelease
```

2. **Upload to Play Console**:
```bash
./gradlew publishBundle
```

### Release Tracks

- **Internal**: For internal testing (up to 100 testers)
- **Alpha**: For alpha testing (open or closed)
- **Beta**: For beta testing (open or closed)
- **Production**: For public release

### Version Management

Versions are automatically managed:
- **Version Code**: GitHub run number
- **Version Name**: Git tag (for releases) or dev build number

## How ARK Drop Works

ARK Drop uses peer-to-peer technology to transfer files directly between devices over the internet:

1. **Sender** selects files and starts transfer
2. **System** generates a secure transfer link and QR code
3. **Sender** shares the link or shows QR code to receiver
4. **Receiver** opens link or scans QR code to connect
5. **Files** are transferred directly between devices with encryption

## Sharing Options

ARK Drop provides multiple ways to share transfers:

- **Deep Links**: Share via messaging apps, email, or any text-based communication
- **QR Codes**: Perfect for in-person sharing or when devices are nearby
- **Copy Link**: Quick clipboard copying for easy sharing

## Project Structure

```
app/
├── src/main/
│   ├── java/dev/arkbuilders/drop/app/
│   │   ├── ui/                 # Compose UI components
│   │   ├── data/               # Data layer
│   │   ├── domain/             # Business logic
│   │   └── di/                 # Dependency injection
│   ├── res/                    # Resources
│   └── AndroidManifest.xml
├── build.gradle.kts            # App build configuration
└── proguard-rules.pro          # ProGuard rules

fastlane/
└── metadata/android/en-US/     # Play Store metadata
    ├── title.txt
    ├── short_description.txt
    ├── full_description.txt
    └── changelogs/

.github/workflows/
├── build_apk.yml              # CI build workflow
└── release.yml                # Release workflow
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For support and questions:
- Create an issue on GitHub
- Contact: support@arkbuilders.dev

## Privacy Policy

ARK Drop respects your privacy:
- No data is collected or stored on external servers
- All transfers are direct device-to-device over internet
- Files are encrypted during transfer
- No analytics or tracking

For more details, see our [Privacy Policy](PRIVACY.md).
