# Reelock (Android)

Blocks Instagram Reels and YouTube Shorts specifically — the rest of each app
still works normally. The home screen gamifies it: a streak grows a little
plant from seed → sprout → sapling → tree, with a battery-optimization
warning and daily stats (streak, scrolls stopped, minutes saved).

## How it works

An Android Accessibility Service watches the screen while Instagram or
YouTube is open. When it detects a Reels or Shorts screen (by looking for
telltale internal view IDs), it automatically presses "back" to exit it.

## Build it — no Android Studio, no computer setup (recommended)

This repo includes a GitHub Actions workflow that builds the APK on GitHub's
servers for free.

1. Create a free account at [github.com](https://github.com) if you don't
   have one.
2. Create a new repository and upload everything in this folder to it
   (on the repo page: "Add file" → "Upload files", drag the whole
   `ReelsBlocker` folder's *contents* in, then commit).
3. Go to the repo's **Actions** tab. A workflow called "Build APK" should
   run automatically (if not, click it and press "Run workflow").
4. Once it finishes (green check, a minute or two), click into that run and
   download the **app-debug** artifact — it's a zip containing
   `app-debug.apk`.
5. Send that APK to your phone any way you like (email to yourself, Google
   Drive/WhatsApp to yourself, USB cable) and tap it to install.
6. Your phone will ask to allow "install from this source" — allow it for
   this one install.

That's it — no SDK, no IDE, nothing installed on your computer.

## Build it with Android Studio (alternative)

1. Install [Android Studio](https://developer.android.com/studio).
2. Open this folder (`ReelsBlocker/`) as a project — File → Open.
3. Let Gradle sync (first run downloads dependencies, needs internet).
4. Connect your phone via USB (enable Developer Options → USB debugging),
   or use an emulator.
5. Click Run ▶. The app installs as "Reelock".

## Set it up on your phone

1. Open the Reelock app.
2. Tap "Enable in Accessibility Settings".
3. Find **Reelock** in the list (usually under "Installed apps" /
   "Downloaded apps") and turn it on.
4. Accept the permission prompt.
5. Go back to the app — it should say "Status: Active".

Now open Instagram Reels or YouTube Shorts — it should kick you back out
within a second.

## Known limitations (please read)

- **Instagram and YouTube can break this at any time.** Detection relies on
  internal view IDs (e.g. `clips_viewer_view_pager`) that both apps can
  rename in any update. If it stops working, open
  `app/src/main/java/com/example/reelsblocker/ReelsBlockerService.kt` and
  update the `BLOCKED_ID_SUBSTRINGS` list — a layout inspector
  (Android Studio → Layout Inspector, while the Reels/Shorts screen is open)
  will show you the current IDs.
- There's a brief flash of the Reels/Shorts screen before it backs out —
  it can't intercept before rendering.
- TikTok's package name is included in the accessibility config as a
  starting point, but its "For You" feed *is* the whole app, so this
  approach can't isolate it the way it can with Instagram/YouTube.
- This is unrelated to and won't be listed on the Play Store as-is —
  Accessibility Services that alter other apps' behavior face strict
  Play Store review. It's meant for personal use, sideloaded onto your own
  device (Run ▶ from Android Studio, or build an APK via
  Build → Build Bundle(s)/APK(s) → Build APK(s)).
- Requires Android 8.0 (API 26) or higher.

## Extending it

- To fully block the apps at certain hours instead, use `AlarmManager` /
  `UsageStatsManager` to detect foreground app and show a blocking overlay.
- To block Shorts in YouTube's home feed shelf (not just the full-screen
  player), add its shelf resource ID to `BLOCKED_ID_SUBSTRINGS` once you've
  found it via Layout Inspector — it changes often.
