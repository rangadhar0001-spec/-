# ଓଡ଼ିଆ ଶିକ୍ଷା ସାଥୀ — Android starter project

ଏହା Kotlin + Jetpack Compose ଉପରେ ଆଧାରିତ ଏକ ପ୍ରାରମ୍ଭିକ Android Studio ପ୍ରୋଜେକ୍ଟ। ଏଥିରେ ଓଡ଼ିଆ ହୋମ୍ ଡ୍ୟାସବୋର୍ଡ, NEP 2020 5+3+3+4 ଶିକ୍ଷା ପର୍ଯ୍ୟାୟ, ପଞ୍ଚପଦୀ ଲେସନ୍ ପ୍ଲାନ୍ ନମୁନା, ଡେମୋ ସମୁଦାୟ ଫିଡ୍ ଏବଂ ପ୍ରୋଫାଇଲ୍ ସ୍କ୍ରିନ୍ ରହିଛି।

## Build କରିବା ପାଇଁ
1. Android Studio (JDK 17 ସହ) ଇନଷ୍ଟଲ୍ କରନ୍ତୁ।
2. ZIP ଫାଇଲ୍‌କୁ Extract କରନ୍ତୁ।
3. Android Studio → Open → `OdiaSikshyaSathi` ଫୋଲ୍ଡର୍ ଖୋଲନ୍ତୁ।
4. Gradle sync ସମ୍ପୂର୍ଣ୍ଣ ହେବାକୁ ଦିଅନ୍ତୁ।
5. Emulator କିମ୍ବା USB debugging ସକ୍ରିୟ Android ଫୋନ୍ ଚୟନ କରି Run କରନ୍ତୁ।
6. APK ପାଇଁ: Build → Build Bundle(s) / APK(s) → Build APK(s)।

ସାଧାରଣ debug APK ପଥ: `app/build/outputs/apk/debug/app-debug.apk`

## ଗୁରୁତ୍ୱପୂର୍ଣ୍ଣ
- ଏହା ଏକ starter/demo project; ଏହା ସହିତ ପୂର୍ବରୁ ତିଆରି APK ନାହିଁ।
- ଲଗଇନ୍, Firebase, cloud database, AI API, ପ୍ରକୃତ ପୋଷ୍ଟିଂ, push notifications, moderation ଏବଂ PDF export ଏଯାବତ୍ implement ହୋଇନାହିଁ।
- ଲେସନ୍ ପ୍ଲାନ୍ ବର୍ତ୍ତମାନ ଡେମୋ ଉଦାହରଣ ଦେଖାଏ; AI ମଡେଲ୍ ସହ ଯୋଡ଼ାଯାଇନାହିଁ।
- ପଞ୍ଚପଦୀର ପଦ ନାମ/ବ୍ୟାଖ୍ୟାକୁ ଆପଣଙ୍କ ସଂସ୍ଥାର ଅଧିକୃତ ମଡେଲ୍ ଅନୁଯାୟୀ ଯାଞ୍ଚ ଓ ସଂଶୋଧନ କରନ୍ତୁ।
- ପ୍ରକାଶ ପୂର୍ବରୁ Android SDK, Gradle dependency compatibility ଏବଂ Odia font rendering ପରୀକ୍ଷା କରନ୍ତୁ।

## ମୁଖ୍ୟ ଫୋଲ୍ଡର୍
- `app/src/main/java/.../MainActivity.kt` — UI ଏବଂ demo navigation
- `app/src/main/AndroidManifest.xml` — Android app entry point
- `app/build.gradle.kts` — Android/Compose dependencies
