# GKFXL Automate

Native Android automation app using Kotlin + Jetpack Compose.

## Existing app features
- Light-theme-only UI, local reminders, notification scheduling, and WhatsApp message preparation.
- Optional one-shot WhatsApp Accessibility Send Assist (requires Android Accessibility permission).

## Gemini + Supabase cloud integration foundation

This commit adds the backend schema, authenticated Gemini Edge Function, Android REST/Auth helper, and Gradle configuration. The cloud APIs are not yet wired into the existing Compose screens or local reminder lifecycle.

1. In Supabase **Project Settings → API**, copy the Project URL and publishable/anon key. The anon/publishable key is intended for client apps; never use a `service_role` or secret key in Android.
2. Add these to your user-level `~/.gradle/gradle.properties` (or another untracked local Gradle properties file):
   ```properties
   SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co
   SUPABASE_ANON_KEY=YOUR_PUBLIC_ANON_KEY
   ```
   Do not commit real credentials.
3. Run `supabase/migrations/202609260001_cloud_automation.sql` in the Supabase SQL Editor. Tables have Row Level Security policies scoped to `auth.uid()`.
4. Deploy `supabase/functions/gemini-assistant/index.ts` as the `gemini-assistant` Edge Function.
5. Set the Gemini key as a server-side function secret, for example `supabase secrets set GEMINI_API_KEY=...`. Never put the Gemini key in Kotlin, Gradle properties, GitHub, or the APK.
6. Enable Email auth in Supabase and configure email confirmation as desired.

### CloudBackend API
- `signUp(email, password)`
- `signIn(email, password)`
- `assistant(prompt, accessToken)`
- `syncRules(accessToken, userId, rulesJson)`
- `log(accessToken, userId, eventType, details)`

Calls are synchronous and must run on a background thread. Session persistence, sign-out, UI integration, pull/merge sync, and automatic event logging still need to be implemented before this is a complete user-facing integration. Test against a development Supabase project first.

## Build
Open in Android Studio, sync Gradle, then build the debug APK. GitHub Actions builds the debug APK on pushes to `main`.
