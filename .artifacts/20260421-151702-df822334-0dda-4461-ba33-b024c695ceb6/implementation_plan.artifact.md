# Fix Test Crash and Improve Robustness

The user reported a `Process crashed` error during instrumentation tests. Based on the logs and the current implementation, the crash is likely due to a `SecurityException` when querying all packages on Android 11+ (API 30+) or a memory/performance issue when loading icons for all apps in the main thread.

## User Review Required

> [!NOTE]
> The `QUERY_ALL_PACKAGES` permission is a sensitive permission. For a real app on the Play Store, you would need to justify its use. For learning and local testing, it is fine.

## Proposed Changes

### App Logic Improvements

I will wrap the package querying logic in a `try-catch` block and move the icon loading to a background thread to prevent "Application Not Responding" (ANR) or crashes during initialization. I'll also use `AsyncImage` or similar if available, or just ensure the list is loaded efficiently.

#### [MainActivity.kt](file:///C:/Users/Allen/AndroidStudioProjects/AppTimeNotify/app/src/main/java/com/example/apptimenotify/MainActivity.kt)

- Wrap `pm.getInstalledApplications` in a `try-catch` block.
- Use `produceState` or `LaunchedEffect` to load the app list asynchronously.
- Improve error handling in case the app list cannot be retrieved.

### Test Robustness

#### [AppSearchTest.kt](file:///C:/Users/Allen/AndroidStudioProjects/AppTimeNotify/app/src/androidTest/java/com/example/apptimenotify/AppSearchTest.kt)

- Add a fallback mechanism in case no apps starting with 'a' are found.
- Add more logging/assertions to pinpoint where it fails.

## Verification Plan

### Automated Tests
- Run the instrumented test again:
  `./gradlew :app:connectedDebugAndroidTest`

### Manual Verification
- Deploy the app to the emulator and verify it doesn't crash on startup.
- Verify the search functionality works manually.
- Check `logcat` for any remaining exceptions.
