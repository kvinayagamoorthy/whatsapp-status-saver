# Keepsake

Keepsake shows the WhatsApp statuses you have already viewed on your phone, lets you look
at them full screen, and saves the ones you want to keep into your phone's gallery.

- No account, no ads, no analytics. The app has no internet permission at all.
- It only reads the statuses folder you grant it, and never changes or deletes anything
  in it.
- Saved copies go to `Pictures/Keepsake` (images) and `Movies/Keepsake` (videos), so
  they show up in any gallery app.

Keepsake is an independent app. It is not affiliated with, endorsed by, or sponsored by
WhatsApp or Meta. Statuses belong to the people who posted them: save and share them only
with their permission.

## Requirements

- Android 8.0 or newer.
- WhatsApp or WhatsApp Business, with statuses you have already opened (WhatsApp keeps
  them for about a day).

## Installing the APK

Keepsake is not on the Play Store. It is installed from an APK file (for example
`keepsake-0.1.0.apk`).

1. Copy the APK to the phone, for example into its Downloads folder.
2. Open the phone's Files app, go to Downloads and tap the APK.
3. The first time, Android says: "For your security, your phone currently isn't allowed
   to install unknown apps from this source." Tap **Settings**, turn on **Allow from this
   source**, then go back.
4. Tap the APK again if the question does not come back by itself. Android asks "Do you
   want to install this app?". Tap **Install**, then **Open**.

The wording differs a little between phone makers. The switch only allows the app that
opened the APK (opened from Files' Downloads, that is "Download Manager"). If Google Play
Protect offers to scan the app, you can let it.

You can turn **Allow from this source** off again afterwards; Keepsake keeps working.

## First start

Keepsake asks for the folder where WhatsApp keeps statuses:

1. Tap **Grant access** next to WhatsApp (or WhatsApp Business).
2. The folder picker opens at the statuses folder. Tap **Use this folder**, then
   **Allow**. (On Android 8 and 9 the button is **Select**, and there is no second step.)
3. Tap **Continue**.

You can change or remove this access later under Settings (the gear on the main screen).

## Updating

Open the newer APK the same way. Android asks "Do you want to update this app?"; tap
**Update**. Keepsake keeps its folder access and settings.

An update only installs over an APK signed with the same key. If Android says "App not
installed as package conflicts with an existing package.", the file was signed with a
different key; uninstall Keepsake first (this removes its folder access, which you then
grant again).

## Uninstalling

Uninstall it like any other app. Copies you saved stay in your gallery.

## Building from source

Needs JDK 17 or newer and the Android SDK (`sdk.dir` in `local.properties` or
`ANDROID_HOME`).

```bash
./gradlew assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
```

The release build is signed when a `keystore.properties` file exists in the project
root (it is git-ignored, and so is the keystore):

```properties
storeFile=/path/outside/the/repo/keepsake-release.jks
storePassword=...
keyAlias=keepsake
keyPassword=...
```

Without it, the build produces `app-release-unsigned.apk`. Every update must be signed
with the same keystore, so keep it and its passwords backed up.

## License

Apache License 2.0, see [LICENSE](LICENSE).
