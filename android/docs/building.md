# Building CallShield

## Prerequisites

- Android Studio installed (provides the JDK)
- `android/key.properties` present (see below)
- `android/upload-keystore.jks` present

If `java` is not on your PATH, prefix all `./gradlew` commands with:
```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

---

## Debug Build

Installs directly on a connected device or emulator. No signing required.

```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

Install immediately:
```bash
./gradlew installDebug
```

---

## Release Build

Signed AAB for Play Store upload. Requires `key.properties` and the upload keystore.

```bash
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

### key.properties setup

Create `android/key.properties` (gitignored):
```
storePassword=YOUR_PASSWORD
keyPassword=YOUR_PASSWORD
keyAlias=callshield-upload
storeFile=../upload-keystore.jks
```

Copy the keystore:
```bash
cp ~/Documents/CallShield-secrets/upload-keystore.jks android/upload-keystore.jks
```

---

## Other Tasks

```bash
./gradlew lintDebug            # Lint checks
./gradlew testDebugUnitTest    # Unit tests
./gradlew clean                # Clean build outputs
```
