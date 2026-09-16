# Smart Todo

Offline-first Android todo app. Tasks are typed in free-form text — including
Roman-script transliterations of non-English languages (e.g. "gym jana hai",
"bazar se doodh lena") — and are automatically classified into categories
without any network round-trip.

## Status

Early scaffold. The domain contract, build configuration and CI pipeline are in
place; data and UI layers are in progress.

## Tech stack

- Kotlin, Jetpack Compose (Material 3)
- MVVM with a pure-Kotlin domain layer (`com.umair.smarttodo.domain`)
- Room for local persistence, Hilt for dependency injection, KSP for codegen
- Kotlin coroutines and `Flow` for reactive data
- minSdk 26, targetSdk 36, compileSdk 36, JDK 17 toolchain
- Dependency versions are centralised in `gradle/libs.versions.toml`

## Build

There is no Android SDK, JDK or Gradle installed on the authoring machine, so
the project is built and verified by GitHub Actions
(`.github/workflows/android.yml`) on every push to `main` and every pull
request. The workflow assembles the debug APK, runs the JVM unit tests and
uploads the APK as a build artifact.

`gradle-wrapper.jar` is intentionally not committed. CI provisions Gradle via
`gradle/actions/setup-gradle` and regenerates the wrapper from
`gradle/wrapper/gradle-wrapper.properties` before invoking `./gradlew`.

To build locally you need JDK 17 and the Android SDK (API 36). With Gradle on
your `PATH`:

```
gradle wrapper --gradle-version 8.13
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Or simply open the project in Android Studio, which supplies its own JDK and
generates the wrapper on import.

## Debug signing key

`app/debug.p12` is a committed debug keystore (alias `smarttodo`, password
`android`), wired into the `debug` signing config in `app/build.gradle.kts`.

It exists because a signing certificate is what Android uses to decide whether
one APK is an update of another. With no keystore in the repo, every developer
machine and every CI run generates its own random debug key, so each build is a
*different app* as far as Android is concerned: installing it over the previous
one fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, and the only way through is
to uninstall first - which deletes the Room database. Pinning the key makes
every build, local or CI, install cleanly over the last one and keep its data.

The tradeoff, stated plainly:

- This is a **debug** key with the conventional password `android`. It protects
  nothing. Anyone with access to this repo can build an APK that Android
  considers the same app as yours.
- That is an accepted tradeoff for a private repo and a sideloaded personal
  build. It would not be acceptable for a published app.
- It must **never** sign anything uploaded to Play. Publishing requires a real
  release keystore kept out of the repo (GitHub Secrets / Play App Signing),
  which is deliberately not set up here.

The certificate is valid for 10000 days, so it will not expire in practice. It
was generated with OpenSSL rather than `keytool` (no JDK on the authoring
machine); the intermediate private-key PEM was created outside the repo and
never committed.
