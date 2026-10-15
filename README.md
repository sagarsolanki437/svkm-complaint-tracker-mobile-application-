# SVKM Complaint Tracker

SVKM Complaint Tracker is a native Android application for recording and following up on student complaints. Students submit complaints and check their status, wardens review and resolve pending entries, and faculty can view and search the complaint register. Complaint and staff activity data are stored locally on the Android device using SQLite.

## Features

### Student

- Enter student name, SAP ID, phone number, branch, and year.
- Choose a complaint category and describe the issue.
- Submit complaints, which are saved with a timestamp and a pending status.
- Look up complaints using a SAP ID and see the status and any warden response.

### Warden

- Sign in to the warden area using the demonstration password `warden1234` and a warden name.
- Browse and search unresolved complaints.
- Open a complaint to review the student and complaint details.
- Add an action or response and mark the complaint resolved.
- View the warden's resolved complaint history and recorded login time.
- Receive a local notification when a complaint is resolved. Android notification permission may be requested on supported Android versions.

### Faculty

- Sign in to the read-only supervision area using the demonstration password `faculty1234` and a faculty name.
- Search and inspect current complaints, including their status and assigned warden.
- View the latest recorded warden dashboard login time.

### Resolved complaint retention

Resolved complaints remain in the active complaint list for 48 hours. The app schedules cleanup and also removes expired resolved entries when complaint screens are loaded. The warden history table retains a copy of resolved complaint details after removal from the active list.

## Technology

- Native Android application written in Java.
- Android Views and XML layouts for the interface.
- SQLite through `SQLiteOpenHelper` for on-device persistence.
- Gradle Kotlin DSL for project and app configuration.
- AndroidX Activity, AppCompat, ConstraintLayout, and Material Components dependencies.

## Requirements

- Android Studio with Android SDK Platform 36 (including minor API level 1 as configured by this project).
- JDK 11 compatibility for Java source and target compilation.
- An Android device or emulator running Android 7.0 (API 24) or later.

## Open and run

1. Open this repository folder in Android Studio.
2. Allow Android Studio to sync the Gradle project and install any requested SDK components.
3. Select an Android emulator or connect a device running API 24 or later.
4. Run the `app` configuration.

The Gradle wrapper is included. From the repository root, a debug APK can also be assembled with:

```powershell
.\gradlew.bat assembleDebug
```

The generated debug APK is normally located at `app/build/outputs/apk/debug/app-debug.apk`.

## Project layout

```text
.
├── app/
│   ├── src/main/
│   │   ├── java/com/sagar/svkmcomplainttracker/
│   │   │   ├── MainActivity.java   # Screens, navigation, validation, and workflows
│   │   │   └── Database1.java      # SQLite schema and complaint operations
│   │   ├── res/                    # Layouts, themes, strings, and app graphics
│   │   └── AndroidManifest.xml     # App entry point and notification/cleanup declarations
│   └── build.gradle.kts
├── gradle/                         # Wrapper and dependency version catalog
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew / gradlew.bat
```

## Data and demonstration credentials

Complaint records, warden sessions, and warden history are saved in the app's private SQLite database on the device. The project does not configure a remote server or cloud synchronization, so data is local to each device and is not automatically shared between devices.

The warden and faculty passwords are fixed demonstration values embedded in the app (`warden1234` and `faculty1234`). They are suitable only for a classroom demonstration, not for production authentication. A production deployment should use secure server-side authentication and access controls.

## Database upgrade note

The current SQLite schema version is 7. The upgrade path from database versions earlier than 7 recreates the complaint and warden tables, which can remove data stored by an older installed version. Back up any important device data before upgrading an existing installation.

## Permissions

The manifest declares `POST_NOTIFICATIONS` to support resolution notifications on Android versions that require runtime notification permission. Complaint information is otherwise kept in the app's local database.

## Version

- Application ID: `com.sagar.svkmcomplainttracker`
- Version name: `1.0`
- Version code: `1`
- Minimum Android API: `24`
- Target Android API: `36`
