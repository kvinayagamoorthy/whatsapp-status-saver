# Product Spec — Keepsake (Android status saver)

Status: Draft v1 (2026-10-13)

## 1. Problem

WhatsApp statuses (photos and videos posted by contacts) disappear after 24 hours,
and WhatsApp has no "save" button for other people's statuses. When a user views a
status, WhatsApp caches the media file in a hidden `.Statuses` folder on the phone.
Users want a simple way to see those cached files and keep the ones they like in
their normal phone gallery.

## 2. Users

A single person on their own Android phone who has WhatsApp and/or WhatsApp Business
installed and has viewed some statuses. Not technical. Wants: open app, see statuses,
tap save, done.

## 3. Goals

- G1. Show all currently cached status images and videos, newest first.
- G2. Preview any item full screen (zoomable image, playable video).
- G3. Save an item to the phone gallery in one tap, where any gallery app can see it.
- G4. Make it obvious which items are already saved.
- G5. Feel native: Material 3, follows system light/dark theme.
- G6. Respect privacy: no network access, no tracking, minimal permissions.

## 4. Non-goals (out of scope)

- Downloading statuses from servers or any network access.
- Posting, reposting, or editing statuses; any interaction with WhatsApp itself.
- A general-purpose gallery or file manager.
- Accounts, cloud backup, sync, ads, analytics, crash reporting services.
- Scheduled or automatic background saving.
- iOS, tablets-specific layouts, Wear OS, TV.
- Deleting files from WhatsApp's folders.

## 5. Functional requirements

### FR1 — First-run folder access
- On first launch, explain in one short screen why access is needed, then open the
  system folder picker pre-pointed at the WhatsApp statuses folder.
- The user taps "Use this folder"; the app keeps that permission across restarts.
- If the user picks the wrong folder, show a clear message and let them retry.
- Support WhatsApp and WhatsApp Business. If both are installed, the user can grant
  both; the list merges them.

### FR2 — Status list
- Grid of thumbnails, newest first (by file modified time).
- Two tabs: **Images** and **Videos**. Video tiles show a play badge and duration
  when cheaply available.
- Pull to refresh. Also refresh automatically when the app returns to foreground.
- Empty state: explain that statuses appear after viewing them in WhatsApp.
- Ignore `.nomedia` and any non image/video files.

### FR3 — Viewer
- Tap a tile to open full-screen viewer.
- Swipe left/right to move between items of the same tab.
- Images: pinch to zoom, double-tap to zoom.
- Videos: play/pause, seek bar, mute toggle; pauses when leaving the screen.
- Actions in the viewer: **Save** and **Share**.

### FR4 — Save to gallery
- Saves a copy into `Pictures/Keepsake` (images) or `Movies/Keepsake` (videos).
- Visible immediately in the phone's gallery apps.
- Saving the same item twice does not create a duplicate; show "Already saved".
- Show a short confirmation (snackbar) on success, clear message on failure.
- Long-press in the grid enters multi-select; a "Save selected" action saves all.

### FR5 — Saved indicator
- Tiles and viewer show a check mark for items already saved by this app.

### FR6 — Share
- Share an item to other apps via the system share sheet.

### FR7 — Theme
- Default: follow system light/dark. Dynamic color on Android 12+.
- Settings screen offers System (default) / Light / Dark.

### FR8 — Access management
- Settings shows which folders are granted and lets the user re-pick or revoke.
- If the persisted permission is lost (e.g. WhatsApp reinstalled), return to FR1 flow.

## 6. Non-functional requirements

- NFR1. minSdk 26 (Android 8.0); target latest stable SDK.
- NFR2. Cold start to visible grid under 1 s with ~200 statuses on a mid-range phone.
- NFR3. Scrolling the grid stays smooth (thumbnails decoded off the main thread, cached).
- NFR4. Permissions: no `INTERNET`. `WRITE_EXTERNAL_STORAGE` only on API ≤ 28.
- NFR5. APK size small (target < 10 MB release).
- NFR6. Accessibility: content descriptions on actions, touch targets ≥ 48 dp,
  works with TalkBack and large font sizes.
- NFR7. Works in portrait and landscape without losing state.

## 7. Legal and store notes

- Do not use "WhatsApp" in the app name, icon, or launcher label. Description may say
  "works with WhatsApp" with a non-affiliation disclaimer.
- Saving other people's media is the user's responsibility; show a one-line note in
  the about section.

## 8. Open questions

- OQ1. ~~Distribution channel~~ Resolved: sideloaded APK for now; Play Store maybe later (D-008).
- OQ2. ~~App display name and icon~~ Resolved: "Keepsake", bookmark-in-a-ring icon (D-009).
