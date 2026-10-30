# Komikta performance changes

## Changes made

- Reduced reader image prefetch from 15 upcoming pages to 4 and from three concurrent workers to one. This limits background network/decode work competing with scroll rendering.
- Removed the eager in-memory warm-up that queued up to six page image decodes while the reader was moving. The reader now relies on the lazy list and Coil cache for visible/near-visible pages, while keeping a small disk prefetch.
- Removed the reader's per-image loading/fade animation state. Each page now updates only its own aspect ratio when image dimensions become available.
- Replaced the shared observable ratio map with a regular remembered map, preventing every ratio update from invalidating all visible page composables.
- Removed spring scaling/pressed-state animation from every comic card, reducing per-item animation work during Explore/Home scrolling.
- Disabled cover crossfade animations to avoid animating every image as grid items enter the viewport.
- Reduced global image HTTP concurrency from 24 requests (8 per host) to 12 (4 per host), leaving more CPU/network headroom for visible content.

## Verification note

The provided project ZIP does not contain `gradle/wrapper/gradle-wrapper.jar`, and no system Gradle executable is available in the environment. Therefore a Kotlin/Gradle build could not be run here. Open the project in Android Studio or restore the wrapper JAR and run `./gradlew :app:compileDebugKotlin` before producing the next APK.
