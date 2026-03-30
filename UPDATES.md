# Pangia Updater

Pangia now supports a simple custom updater:

1. The app checks a hosted JSON file once per app launch.
2. If the hosted `versionCode` is higher than the installed one, Pangia shows an update dialog.
3. Tapping `Update now` opens the hosted APK link in the browser.
4. The user downloads and installs the APK manually.

This is not a silent background update system.

## Hosted JSON format

Host a small JSON file like this:

```json
{
  "versionCode": 2,
  "versionName": "1.0.1",
  "downloadUrl": "https://your-domain-or-host/Pangia-v1.0.1.apk",
  "releaseNotes": "Bug fixes and small improvements."
}
```

An example copy is available at `release/latest.example.json`.

## Project configuration

Set these fields in `app/build.gradle.kts` before building:

- `UPDATE_CONFIG_URL`
- `UPDATE_FALLBACK_DOWNLOAD_URL`

Example:

```kotlin
buildConfigField("String", "UPDATE_CONFIG_URL", "\"https://your-domain-or-host/latest.json\"")
buildConfigField("String", "UPDATE_FALLBACK_DOWNLOAD_URL", "\"https://your-domain-or-host/Pangia-v1.0.1.apk\"")
```

## Release workflow

For each update:

1. Increase `versionCode` and `versionName`.
2. Build a new signed APK.
3. Upload the APK online.
4. Update the hosted `latest.json`.
5. Rebuild Pangia if you changed the fallback URL.

## Important rules

- Keep the same `applicationId`.
- Keep using the same signing key.
- Every update must use a higher `versionCode`.
