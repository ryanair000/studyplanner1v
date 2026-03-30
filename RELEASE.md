# Student Copilot Release Checklist

Current status as of 2026-03-30:
- Debug build, unit tests, and Android test APK compilation are working locally.
- Google Play release is still blocked because the app targets API 34. New Play submissions currently require API 35 or higher.

Before publishing:
1. Install Android SDK Platform 35 (or a newer stable platform) in Android Studio SDK Manager.
2. Update `compileSdk` and `targetSdk` in `app/build.gradle.kts` to 35 or higher.
3. Create an upload keystore.
4. Copy `keystore.properties.example` to `keystore.properties` and fill in the real values.
5. Build a signed release bundle with `.\gradlew.bat bundleRelease`.
6. Upload the `.aab` file to Play Console.

If your Play Console account is a new personal account:
- Run a closed test with at least 12 opted-in testers for 14 continuous days before applying for production access.

Suggested first-release checks:
- Confirm the course, assignment, and delete flows on a real device.
- Decide whether to ship the Exams tab now or hide it until it is fully configured.
- Prepare Play listing assets: title, short description, full description, icon, screenshots, and privacy details.
