# Pangia Product Requirements Document

## Document Overview

- Product name: Pangia
- Product type: Android student planning and academic organization app
- Current app version: 1.0.4
- Document status: Working PRD
- Primary platform: Android
- Current package ID: `com.example.studentcopilot`

## 1. Executive Summary

Pangia is a mobile app that helps students organize their academic lives in one place. It gives users a lightweight system to manage courses, assignments, and exams, see upcoming work at a glance, and receive reminders before deadlines and classes.

The product is designed for students who need a simple personal academic organizer without the complexity of a full learning management system. Pangia supports both signed-in use with cloud sync and guest use with local-only storage, making it accessible for quick adoption while still allowing account-based continuity.

## 2. Problem Statement

Students often manage academic responsibilities across scattered tools such as notebooks, messaging apps, calendars, LMS portals, and mental reminders. This leads to missed deadlines, poor visibility into workload, and fragmented planning behavior.

The core problems Pangia addresses are:

- Students do not have a single clear view of upcoming assignments and exams.
- Important deadlines are easy to forget without reminders.
- Many students want a lightweight planner, not a full productivity suite.
- Some users want to try the app before creating an account.
- Users who do create accounts expect their data to persist and sync reliably.

## 3. Vision

Pangia should become the simplest reliable academic planner for students who want to stay on top of coursework, deadlines, and class schedules from their phone.

## 4. Product Goals

### Primary Goals

- Give students one place to track courses, assignments, and exams.
- Reduce missed deadlines through timely reminders.
- Make the app useful immediately, even without account creation.
- Support account-based sync for users who want persistence across devices.
- Keep the experience simple enough to be learned in minutes.

### Secondary Goals

- Provide a clear dashboard that summarizes academic workload.
- Support gradual adoption by allowing guest mode first and account creation later.
- Create a stable v1 foundation for future additions such as richer timetable support and analytics.

## 5. Non-Goals

The following are not part of the current Pangia v1 scope:

- Full timetable generation across all courses and semesters
- Lecturer dashboards or classroom management tools
- Collaborative planning between multiple users
- LMS integrations such as Moodle, Canvas, or Google Classroom
- Public social features
- iOS, web, or desktop clients
- Automatic background app updates via app store infrastructure
- Google sign-in or other social sign-in methods

## 6. Target Users

### Primary Persona: Undergraduate Student

- Uses a smartphone daily
- Needs help remembering assignments, exams, and classes
- Wants a lightweight, focused tool
- May or may not want to create an account immediately

### Secondary Persona: New User Exploring the App

- Wants to try the app before trusting it with account information
- Needs immediate value with low setup friction
- Benefits from guest mode

### Secondary Persona: Returning Multi-Device User

- Wants to access the same academic data across devices
- Expects sign-in, persistence, and cloud sync

## 7. Jobs To Be Done

- When I start a semester, I want to add my courses so I can organize everything around them.
- When I receive an assignment, I want to log it quickly so I do not forget it.
- When exam dates are announced, I want to store them in one place so I can prepare ahead.
- When I open the app, I want to instantly see what is coming up next.
- When a deadline or class is near, I want the app to remind me.
- When I do not want to sign up yet, I want to use the app as a guest.
- When I later create an account, I want my existing guest data to move with me if possible.

## 8. Current Product Scope

### 8.1 Access and Identity

Pangia currently supports:

- Email/password sign up
- Email/password sign in
- Session restore on relaunch
- Sign out
- Guest mode

Guest mode allows a user to enter the app without creating an account. Guest data remains on the device only. If a guest later signs into a real account on the same device and that account has no local rows yet, Pangia migrates the guest data into the signed-in profile before cloud sync.

### 8.2 Dashboard

The dashboard provides:

- Count of upcoming assignments
- Count of upcoming exams
- Nearest upcoming deadline
- Signed-in or guest-state messaging
- Sync status messaging for cloud users
- Retry sync action when sync fails
- Sign out or leave guest mode action

### 8.3 Courses

Users can:

- Add courses
- Edit courses
- Delete courses
- Attach an optional weekly class reminder to a course
- Set class day of week
- Set class start time

Deleting a course also removes related assignments and exams.

### 8.4 Assignments

Users can:

- Add assignments
- Associate each assignment with a course
- Set due date
- Mark assignments as completed
- Delete assignments

### 8.5 Exams

Users can:

- Add exams
- Associate each exam with a course
- Set exam date
- Set exam type
- Delete exams

Users must have at least one course before adding exams.

### 8.6 Reminders

Pangia supports local reminders for:

- Assignments due today
- Exams scheduled for today
- Weekly class reminders for courses with class day/time set

Class reminders are currently configured for 30 minutes before class start.

### 8.7 Data and Sync

The app uses:

- Room/SQLite for on-device local storage
- Supabase for authentication and cloud sync when signed in

Behavior by access mode:

- Guest users: local-only data, no cloud sync
- Signed-in users: local cache backed by Supabase sync

### 8.8 Update Delivery

Pangia contains an app-update check framework that can read a remote JSON config and prompt the user to download a newer APK in the browser. This mechanism exists in code but is not active unless update URLs are configured at build time.

## 9. Product Principles

- Simple first: core academic planning should never feel bloated.
- Low-friction entry: users should be able to start immediately.
- Local reliability: the app must remain useful even with network issues.
- Clear ownership: every assignment and exam should belong to a course.
- Helpful reminders: reminders should be useful, not noisy.
- Honest state: guest mode, sync status, and failures should be visible to the user.

## 10. Functional Requirements

### 10.1 Authentication and Access

1. The app must allow users to sign up using email and password.
2. The app must allow existing users to sign in using email and password.
3. The app must allow users to continue as guest without account creation.
4. The app must restore the last active session on app relaunch.
5. The app must support sign out.
6. The app must display whether the user is signed in or using guest mode.
7. The app must preserve guest data locally until the guest leaves the app or signs in.
8. If a guest signs into an account with no local data on that device, Pangia should migrate guest data into that account's local profile before sync.

### 10.2 Dashboard

1. The dashboard must show counts of upcoming assignments and exams.
2. The dashboard must show the nearest upcoming assignment or exam date.
3. The dashboard must update over time as the current day changes.
4. For signed-in users, the dashboard must show sync progress or sync failure state.
5. For guest users, the dashboard must show that data is stored only on-device.

### 10.3 Courses

1. Users must be able to create a course with a name.
2. Users must be able to edit course details.
3. Users must be able to delete a course.
4. Course deletion must display a confirmation because related data is also removed.
5. Users must be able to optionally enable a weekly class reminder.
6. If class reminder is enabled, the user must provide class day and start time.

### 10.4 Assignments

1. Users must be able to create assignments tied to an existing course.
2. Users must be able to set a due date.
3. Users must be able to mark assignments complete or incomplete.
4. Users must be able to delete assignments.
5. Completed assignments should be visually differentiated from incomplete assignments.

### 10.5 Exams

1. Users must be able to create exams tied to an existing course.
2. Users must be able to set an exam date.
3. Users must be able to provide an exam type.
4. Users must be able to delete exams.
5. If no courses exist, the exam flow must direct the user to create a course first.

### 10.6 Reminders

1. The app must schedule reminders for incomplete assignments that are due today.
2. The app must schedule reminders for exams scheduled for today.
3. The app must schedule recurring weekly class reminders for courses with a class schedule.
4. The app must cancel and reschedule reminders when relevant academic data changes.
5. The app must request notification permission on supported Android versions.

### 10.7 Cloud Sync

1. Signed-in users must sync their courses, assignments, and exams with Supabase.
2. The app must keep a local Room cache for signed-in users.
3. The app must gracefully show sync failures rather than crashing.
4. The app must allow users to retry sync from the dashboard.
5. Guest mode must not attempt to use cloud sync.

### 10.8 Update Prompting

1. If update URLs are configured and a higher version is available, the app should prompt the user to update.
2. The update action should open a browser or download link rather than silently installing updates.

## 11. User Experience Requirements

### 11.1 Navigation

- The app must use bottom navigation for core product areas:
  - Dashboard
  - Courses
  - Assignments
  - Exams

### 11.2 Empty States

- Courses screen should guide the user to add a first course.
- Assignments screen should guide the user to add the first assignment.
- Exams screen should either guide course creation or exam creation depending on state.
- Dashboard should remain informative even with no academic data.

### 11.3 Error States

- Auth errors must be shown in-context on the auth screen.
- Sync errors must be visible on the dashboard.
- CRUD failures should be shown on the relevant screen.

### 11.4 Performance Expectations

- The app should launch into a usable state quickly.
- Navigation between main tabs should feel immediate.
- Adding, editing, and deleting local data should feel responsive.

## 12. Non-Functional Requirements

### 12.1 Platform

- Android only
- Minimum SDK: 26
- Target SDK: 35

### 12.2 Reliability

- The app must not depend on constant connectivity for core use.
- Local storage must remain functional without network access.
- Signed-in users should degrade gracefully when Supabase is unavailable.

### 12.3 Security and Privacy

- The Android client must use only the Supabase publishable key.
- No service-role key may be embedded in the app.
- User data must be separated per owner profile.
- Guest data must remain local unless intentionally migrated into a signed-in account.

### 12.4 Maintainability

- Database schema changes must be migration-based, not destructive.
- Core flows should be covered by at least a baseline of unit and instrumentation tests.

## 13. Data Model Summary

### Core Entities

- Course
  - local ID
  - remote ID
  - owner user ID
  - name
  - optional class day of week
  - optional class start minute of day

- Assignment
  - local ID
  - remote ID
  - owner user ID
  - course ID
  - title
  - due date
  - completion state

- Exam
  - local ID
  - remote ID
  - owner user ID
  - course ID
  - title
  - date
  - type

## 14. Technical Context

Current stack:

- Kotlin
- Jetpack Compose
- Navigation Compose
- Android ViewModel + coroutines
- Room database
- WorkManager for reminders
- Supabase for auth and data sync

Release distribution currently supports:

- Signed release APK generation
- Optional browser-based custom update prompt if configured
- Manual APK sharing flows

## 15. Success Metrics

### North Star Outcome

Students consistently know what academic work is coming next and miss fewer deadlines.

### Suggested v1 Metrics

- Weekly active users
- Percentage of users who add at least 3 courses within first session
- Percentage of users who add at least 1 assignment or exam within first session
- Reminder permission acceptance rate
- Guest-to-account conversion rate
- 7-day retention
- Crash-free session rate
- Sync success rate for signed-in users

## 16. Release Readiness Criteria

Pangia v1 should be considered ready when:

- Users can complete auth or guest entry without blockers
- CRUD works for courses, assignments, and exams
- Dashboard data is accurate
- Reminders schedule correctly
- Sync works for signed-in users
- Release APK builds cleanly
- Core flows have at least baseline verification
- The app is stable on a real Android device

## 17. Risks and Mitigations

### Risk: Users rely on guest mode and later lose device-local data

Mitigation:

- Clearly message that guest data stays on-device
- Encourage account creation for cross-device persistence

### Risk: Cloud sync issues reduce trust

Mitigation:

- Keep local Room cache as fallback
- Show sync errors clearly
- Provide retry flow

### Risk: Reminder fatigue

Mitigation:

- Keep reminders limited to deadline-day and class-start logic
- Avoid overly frequent reminder schedules

### Risk: Accidental destructive actions

Mitigation:

- Confirm course deletion
- Continue improving destructive-action UX where needed

### Risk: App identity and distribution are not fully polished

Mitigation:

- Finalize permanent application ID before broader public store launch
- Standardize release process and listing assets

## 18. Dependencies

### Product Dependencies

- Android notification permissions
- Supabase project configuration
- Supabase SQL schema deployment
- Stable Android release signing setup

### Operational Dependencies

- APK hosting or Play Store workflow for distribution
- Basic QA on physical Android devices

## 19. Future Roadmap

### Near-Term Opportunities

- Password reset flow
- Better onboarding for first-time users
- Account settings screen
- Search and filtering
- Editing and richer metadata for assignments and exams

### Mid-Term Opportunities

- Semester and academic period organization
- Calendar and agenda view
- Richer timetable support
- Backup/export features
- Better analytics for workload planning

### Long-Term Opportunities

- LMS import or integration
- Web companion app
- Lecturer-facing planning or classroom views
- AI-assisted study planning and prioritization

## 20. Open Questions

- Should guest mode data be explicitly exportable before sign-in?
- Should reminder timing be customizable by the user?
- Should assignments and exams support notes or attachments?
- Should Pangia support semester grouping and archived terms?
- When the product reaches wider release, should the app/package identity be renamed from the current internal package ID?

## 21. Recommended Product Positioning

Suggested positioning statement:

Pangia is a simple academic planner that helps students manage courses, assignments, exams, and reminders from one Android app, with the flexibility to start as a guest and upgrade to a synced account when ready.
