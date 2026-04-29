# AppTimeNotify

AppTimeNotify is an Android application that helps users monitor and limit their time spent on specific applications. 

## Features

- **App Selection**: Browse and search through installed applications.
- **Time Limits**: Set a daily time limit (hours and minutes) for a selected app.
- **Real-time Tracking**: Background service monitors usage using `UsageStatsManager`.
- **Notifications**: Receive a notification when the user-defined limit is reached.

## Permissions

To function correctly, the app requires the following permissions:

- **Usage Access**: Required to monitor app usage. Users will be prompted to enable this in the system settings on the first launch.
- **Foreground Service**: Needed for the app to track usage in the background.
- **Post Notifications**: Required to send alerts when time limits are reached (Android 13+).

## Setup & Installation

1. Clone the repository.
2. Open the project in Android Studio.
3. Build and run the app on an Android device or emulator.
4. Grant the "Usage Access" permission when prompted.

## Verification

### Automated Tests
Run the UI tests using Gradle:
```bash
./gradlew connectedDebugAndroidTest
```

### Manual Verification
1. Launch the app.
2. Search for an app (e.g., "YouTube") and select it.
3. Enter a small limit (e.g., 0 hours, 1 minute).
4. Click "Confirm".
5. Use the selected app for the specified duration.
6. Verify that a notification appears when the limit is exceeded.
