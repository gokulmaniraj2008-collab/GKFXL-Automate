# GKFXL Automate

A native Android automation app written in Kotlin with Jetpack Compose. GitHub is the source of truth for this project.

## Current starter features

- Light-theme-only Android UI.
- Create daily or one-time time-based reminder rules.
- Enable, disable, and delete saved rules.
- Local persistence using Android SharedPreferences.
- Scheduled reminders delivered as Android notifications.
- WhatsApp deep-link composer: opens a chat with a prefilled message; the user must tap **Send**.
- Permission guidance for notifications and exact alarms.

## Build

Open this repository in Android Studio (Hedgehog or newer), allow Gradle sync, then run the app configuration on an Android device or emulator. To produce an APK, use the Android Studio Build menu and select Build APK(s).

- Android Gradle Plugin: 8.7.3
- Kotlin: 2.0.21
- Compile/target SDK: 35
- Minimum SDK: 26

## Important limitations

- Android may delay alarms due to battery restrictions, Doze, reboot, or manufacturer-specific background policies.
- This starter does not yet restore scheduled alarms after device reboot.
- Exact alarms may require the user to grant access in Android Settings.
- Android 13+ requires notification permission.
- WhatsApp is never sent a message silently; the user reviews and taps Send.
- The current starter schedules reminder actions only. Additional action types and a richer run-history screen can be added in future commits.

## Repository

https://github.com/gokulmaniraj2008-collab/GKFXL-Automate
