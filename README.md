# grizz

A day counter for Android. Add the dates you care about, see how many days away
they are, and keep the next one on your home screen.

- **Just the number.** Each event is a coloured card with a big number on it.
  Dates that have passed count up instead, marked `ago`.
- **Your order.** Long-press a card and drag it — the list stays exactly how you
  arrange it.
- **Home-screen widget.** Shows whichever event is next (or the most recent one,
  if they've all passed). Tap it to open the app.
- **On device only.** Events live in a single JSON file in the app's private
  storage. No account, no network. Export/import from the ⋮ menu backs it up.

## Getting the app on your phone

Every push builds a debug APK in GitHub Actions:

1. Open the repo's **Actions** tab and pick the latest **Build APK** run.
2. Download the `grizz-debug-apk` artifact.
3. Unzip it on your phone and open `app-debug.apk` — Android will ask you to
   allow installs from that app the first time.

## Building locally

Open the project in Android Studio (Ladybug or newer) and run it, or:

```
./gradlew assembleDebug
```

Requires JDK 17 and the Android SDK (compileSdk 35). Minimum device: Android 8.0.

## Layout

```
app/src/main/java/com/grizz/countdown/
├── MainActivity.kt          entry point
├── data/
│   ├── CountdownEvent.kt    the model, plus day arithmetic
│   └── EventStore.kt        JSON-file persistence, shared with the widget
├── ui/
│   ├── CountdownApp.kt      list and editor screens
│   ├── DragDropState.kt     long-press reordering for the list
│   ├── EventsViewModel.kt   state, export/import
│   ├── Palette.kt           card colours
│   └── theme/Theme.kt       app theme, contrast helper
└── widget/
    ├── CountdownWidget.kt   Glance widget
    └── CountdownWidgetReceiver.kt
```
