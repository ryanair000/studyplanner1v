# Supabase Setup

Pangia now uses Supabase for authentication and cloud sync, with Room as the on-device cache.

## What Is Live

- Email/password sign up
- Email/password sign in
- Guest mode with local-only data
- Session restore on app launch
- Sign out
- Per-user local data separation on the same device
- Cloud sync for courses, assignments, and exams

## Data Model

- Supabase stores the canonical courses, assignments, and exams.
- Room keeps a local cached copy for the signed-in user on that device.
- Existing pre-auth local data is claimed by the first signed-in user and uploaded on the next successful sync.

## Required Local Config

Create `supabase.properties` in the project root:

```properties
url=https://your-project-ref.supabase.co
publishableKey=sb_publishable_xxxxxxxxxxxxxxxxx
```

There is a template at [supabase.properties.example](/c:/student-copilot/supabase.properties.example).

## Required Supabase SQL

Run [schema.sql](/c:/student-copilot/supabase/schema.sql) in the Supabase SQL Editor before testing cloud sync. The app will show a sync error until those tables and RLS policies exist.

## Security Note

Do not put a Supabase `service_role` key into the Android app. If a service-role key was exposed during setup or testing, rotate it in Supabase before shipping.

## Supabase Dashboard Settings

- Authentication provider: Email
- Confirm email: optional, depending on whether you want verification before first sign-in

If email confirmation is enabled, Pangia will show a confirmation message after sign-up and wait for the user to verify before signing in.

## Build Verification

Use:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug testDebugUnitTest assembleRelease
```

## Current Behavior

- Add actions save to Supabase first when possible, and can still fall back to local cache if the backend is not ready yet.
- Updates and deletes for already-synced rows require the Supabase tables to be live.
- If cloud sync fails, Pangia shows the reason on the dashboard and offers a retry.
- Guest mode skips Supabase completely and keeps data on the device under a local guest profile.
- If the guest profile has local data and the same device later signs into a real account for the first time, Pangia migrates that guest data into the signed-in account before syncing.
