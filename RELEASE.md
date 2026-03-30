# Pangia Release Checklist

Current status as of 2026-03-30:
- The app now targets Android 15 / API 35.
- The build uses Android Gradle Plugin 8.6.0 with Gradle 8.7.
- Local release signing is configured through ignored files:
  - `keystore.properties`
  - `release/upload-keystore.jks`
- Verified release artifacts:
  - `app/build/outputs/bundle/release/app-release.aab`
  - `app/build/outputs/apk/release/app-release.apk`

Verified locally:
1. `.\gradlew.bat testDebugUnitTest --no-daemon --console=plain`
2. `.\gradlew.bat assembleDebug --no-daemon --console=plain`
3. `.\gradlew.bat assembleDebugAndroidTest --no-daemon --console=plain`
4. `.\gradlew.bat assembleRelease --no-daemon --console=plain`
5. `.\gradlew.bat bundleRelease --no-daemon --console=plain`

Release assets to back up now:
1. `keystore.properties`
2. `release/upload-keystore.jks`

Manual steps before Play submission:
1. Install the release APK on a real device and smoke test the main flows.
2. Upload `app/build/outputs/bundle/release/app-release.aab` to Play Console.
3. Complete the Play listing: app name, descriptions, screenshots, icon, feature graphic, contact details, and privacy answers.
4. Fill in the Data safety section based on what the app stores and transmits.
5. If your Play Console account is a new personal account, complete closed testing before requesting production access.

Known limits:
- Instrumentation coverage is compiled and ready, but it was not executed because no emulator or device was connected through `adb`.
- Builds on this Windows machine may show a Kotlin daemon fallback warning; the build still completes successfully.
