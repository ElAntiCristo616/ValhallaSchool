# Stash

A personal, offline Android app in the style of Deepstash: 1,200 bite-sized ideas from 120 books, with Duolingo-style learning on top. No login, no backend, no tracking.

## Features

- **Home**: full-screen vertical swipe cards. A card counts as read once it stays on screen for about 1.2 s. Read ideas never show up in the feed again. Filter by topic.
- **Explore**: all 120 books with progress bars, topic filters, and instant search across books and ideas. Each book page has a progress bar, "Continue" (unread ideas only), and a quiz.
- **Learn**:
  - Daily XP goal, streaks, and levels.
  - A path of 360 lessons: two 5-idea lessons plus a book quiz per book.
  - Spaced-repetition reviews.
  - Four question types: pick the idea, which book, true/false, and fill in the blank.
  - Wrong answers come back at the end of the session.
  - Combos, sounds, haptics, and confetti.
- **Stash**: saved ideas, plus your full reading history grouped by day.
- **Profile**: weekly XP chart, progress per topic, 16 achievements, daily goal, dark/light theme, sounds, and JSON backup export/import.

## Tech

Plain Java with Android Views built in code (no XML layouts, no WebView). The only dependency is `androidx.recyclerview`. The release APK is about 220 KB. Min SDK 26 (Android 8), target SDK 35.

## Build

Requirements: JDK 17 (path set in `gradle.properties`) and the Android SDK (`sdk.dir` in `local.properties`, which is not committed).

```sh
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
./gradlew assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
```

The release build is signed with the local debug key, which is fine for sideloading onto your own phone. A prebuilt APK is attached to the GitHub Releases.

## Content

The content lives in `app/src/main/assets/{cats,books,ideas}.tsv`. It is generated from the source PDF, which is not in the repo:

```sh
python3 tools/pdf_to_assets.py deepstash-content.pdf   # needs poppler's pdftotext
```

The mapping of books to topics is in `tools/pdf_to_assets.py`.
