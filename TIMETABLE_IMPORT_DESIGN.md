# Pangia Timetable Import Design

## Purpose

This document defines the recommended approach for adding timetable upload and sync to Pangia.

The goal is to let students import their class timetable, attach timetable sessions to courses, and use those sessions for reminders and future schedule views.

## Recommendation Summary

Pangia should start with **personal timetable import**, not a shared university-wide timetable catalog.

Recommended first release:

- Each student uploads their own timetable file
- Pangia validates and previews the file
- Pangia creates or updates the user's courses
- Pangia creates timetable session rows linked to those courses
- Pangia uses timetable sessions for reminders

Recommended input format for v1:

- CSV

Recommended later phases:

- Shared institutional templates
- Excel support
- PDF parsing and OCR

## Why The Current Model Is Not Enough

Today, Pangia stores only one weekly class slot per course:

- `classDayOfWeek`
- `classStartMinuteOfDay`

That is too limited for real academic timetables because a single course may have:

- multiple lectures per week
- lecture + tutorial + lab
- different sections or streams
- venue changes
- irregular week patterns

Because of that, a timetable feature should not continue to treat a course and a recurring class session as the same thing.

## Recommended Product Scope

### Phase 1: Personal Timetable Upload

User outcome:

- A student uploads a timetable file and Pangia converts it into courses and class sessions

Included:

- CSV upload
- validation
- preview screen
- create/update courses
- create timetable sessions
- reminder scheduling from timetable sessions

Not included in phase 1:

- OCR
- PDF parsing
- shared university timetable repository
- timetable conflict resolution across multiple students

### Phase 2: Shared Timetable Templates

User outcome:

- Students can choose a known program/year/semester timetable template instead of uploading a file manually

Included:

- curated templates by institution
- program/year/semester/stream filtering
- template-to-course mapping

### Phase 3: Smart Parsing

User outcome:

- Students can import from PDF, image, or spreadsheet with less manual formatting

Included:

- XLSX parsing
- PDF table extraction
- OCR fallback for scanned timetables

## Recommended Data Model

### New Entity: TimetableEntry

Add a new table/entity for recurring class sessions.

Suggested fields:

- `id`
- `remote_id`
- `owner_user_id`
- `course_id`
- `term`
- `class_type`
- `section`
- `day_of_week`
- `start_minute_of_day`
- `end_minute_of_day`
- `venue`
- `lecturer`
- `week_pattern`
- `source`
- `created_at`
- `updated_at`

### Course Changes

Keep `courses` as the academic course container, but enrich it.

Suggested additional fields:

- `course_code`
- `term`
- `section` or `stream`

Suggested direction:

- deprecate reminder scheduling from `courses.classDayOfWeek`
- deprecate reminder scheduling from `courses.classStartMinuteOfDay`
- move class scheduling responsibility to `timetable_entries`

### Why This Separation Matters

- `courses` are identity and organization
- `timetable_entries` are actual scheduled sessions
- `assignments` and `exams` stay linked to `courses`

That model scales cleanly for:

- multiple sessions per course
- multiple sections
- future calendar views
- timetable conflict detection

## Recommended Upload Format

### Format Choice

Use CSV first.

Why:

- easy to create in Excel and Google Sheets
- easy to export and share
- easy to validate
- easy to parse reliably on Android and backend
- much more predictable than PDF/image import

### CSV Structure

One row should represent one class meeting.

Recommended columns:

- `term`
- `course_code`
- `course_name`
- `class_type`
- `section`
- `day_of_week`
- `start_time`
- `end_time`
- `venue`
- `lecturer`
- `week_pattern`

### Required Fields For v1

- `course_code`
- `course_name`
- `day_of_week`
- `start_time`
- `end_time`

### Optional Fields For v1

- `term`
- `class_type`
- `section`
- `venue`
- `lecturer`
- `week_pattern`

### Allowed Day Values

Recommended accepted inputs:

- `MON`, `TUE`, `WED`, `THU`, `FRI`, `SAT`, `SUN`

Optional parser flexibility:

- `MONDAY`, `TUESDAY`, etc.
- `1` to `7`

### Allowed Time Format

Recommended format:

- `HH:mm`

Examples:

- `08:00`
- `14:30`

## Sample CSV

See [timetable-upload-template.csv](C:/student-copilot/samples/timetable-upload-template.csv).

## Import Flow

### Proposed User Flow

1. User opens a new `Import Timetable` action.
2. User selects a CSV file from storage.
3. Pangia validates the header and rows.
4. Pangia shows a preview:
   - recognized courses
   - detected sessions
   - validation warnings
5. User confirms import.
6. Pangia creates or updates courses.
7. Pangia creates timetable session rows.
8. Pangia reschedules reminders.
9. Pangia shows an import summary.

### Suggested Import Summary

- courses created
- courses matched
- timetable sessions created
- skipped rows
- warnings

## Course Matching Logic

### Recommended Matching Key

For v1, match courses using:

- `course_code`
- `section` if present
- `term` if present

If no match is found:

- create a new course

If one clear match is found:

- link the timetable rows to the existing course

If multiple possible matches are found:

- show a merge/match choice in preview

## Sync Strategy

### Local

- Import into Room first
- Show preview and errors before commit

### Remote

For signed-in users:

- sync `courses`
- sync `timetable_entries`

For guest users:

- keep import local only

### Guest To Signed-In Upgrade

If guest data later moves into a signed-in account on the same device:

- timetable entries should migrate together with courses

## Reminder Strategy

Current Pangia reminders are built around one class slot per course.

That should change to:

- schedule one recurring reminder per timetable entry

Recommended reminder input:

- `course_id`
- `class_type`
- `day_of_week`
- `start_minute_of_day`
- `venue`

Recommended notification message:

- `CSC101 Lecture starts at 08:00 in LT2`

## Validation Rules

### File-Level Validation

- file must be CSV
- header must contain required columns
- file must contain at least one valid row

### Row-Level Validation

- `course_code` must not be blank
- `course_name` must not be blank
- `day_of_week` must map to a valid day
- `start_time` must be valid
- `end_time` must be valid
- `end_time` must be later than `start_time`

### Warning-Level Validation

- duplicated rows
- suspicious overlapping sessions
- unknown class type value
- missing optional fields

## UoN-Specific Considerations

For UoN and similar institutions, timetable rows may include:

- program
- stage/year
- semester
- group/stream
- lecturer
- venue

Recommended v1 behavior:

- ignore extra columns if present
- parse only the supported subset

Recommended v2 behavior:

- preserve more institutional metadata

## UX Recommendations

### New Screen Ideas

- `Import Timetable`
- `Import Preview`
- `Import Result`

### Display Recommendations

On a future course details screen, show:

- all linked timetable sessions
- day/time
- class type
- venue

### Error Messaging

Keep errors specific:

- `Row 4: start_time is invalid`
- `Row 7: end_time must be after start_time`
- `Missing required column: course_code`

## Suggested Backend Changes

### Supabase

Add table:

- `timetable_entries`

Suggested relationships:

- `timetable_entries.course_id -> courses.id`
- `timetable_entries.owner_user_id -> auth.users.id`

Suggested indexes:

- `owner_user_id`
- `course_id`
- `day_of_week`

Suggested RLS:

- same pattern as existing tables
- user can only read/write their own timetable entries

## Suggested Room Changes

Add:

- `TimetableEntryEntity`
- `TimetableEntryDao`
- repository support
- migrations

Update:

- reminder scheduler
- course details presentation
- sync repository

## Implementation Plan

### Step 1

Data model refactor:

- add `course_code`, `term`, `section` support
- add `timetable_entries`

### Step 2

Sync and local persistence:

- Room entity and DAO
- Supabase schema and sync

### Step 3

Reminder migration:

- schedule class reminders from timetable entries

### Step 4

CSV import pipeline:

- file picker
- parser
- validator
- preview
- import commit

### Step 5

UI polish:

- timetable list in course details
- import history or summary

## Acceptance Criteria For Phase 1

- User can import a valid CSV file
- Pangia creates courses when needed
- Pangia creates multiple class sessions for a single course
- Imported timetable sessions sync for signed-in users
- Imported timetable sessions remain local for guest users
- Class reminders are generated from imported sessions
- Invalid rows are reported clearly
- Import preview shows what will be created or updated

## Risks

### Risk: messy user timetable files

Mitigation:

- strict CSV template
- preview and validation
- row-by-row error reporting

### Risk: duplicate course creation

Mitigation:

- use `course_code + section + term` matching
- preview before import

### Risk: reminder duplication

Mitigation:

- use unique work names per timetable entry
- clear and reschedule on import

### Risk: overbuilding too early

Mitigation:

- start with CSV only
- defer OCR and PDF parsing

## Recommended Next Build Step

The best next engineering step is:

1. add `TimetableEntryEntity` and Supabase `timetable_entries`
2. add `course_code`, `term`, and `section` to `courses`
3. refactor reminders to use timetable entries

After that, Pangia will be structurally ready for CSV timetable import.
