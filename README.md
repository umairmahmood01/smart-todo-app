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
