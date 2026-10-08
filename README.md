# Learning Dashboard (Android · Kotlin · Jetpack Compose)

Login → Course Dashboard → Course Details with lesson completion, offline-first.
**Demo logins:** `demo@learn.com` or `alex@learn.com` (test states: `empty@`, `error@`, `expired@learn.com`), password `password123` · **Run tests:** `./gradlew testDebugUnitTest` · **APK:** `./gradlew assembleRelease`

The backend is mocked by `MockApiInterceptor`, an OkHttp interceptor that serves `assets/mock/*.json` behind a real Retrofit API.
It adds latency and **fails with an `IOException` when the device is offline**, so the offline path runs the same way it would against a real server.
Point the app at a real backend by removing that one interceptor.

## 1. Architecture
MVVM with a Clean-style layering: `ui` (Compose + ViewModels) → `domain` (models, repository interfaces, `ProgressCalculator`, validation) → `data` (Room, Retrofit, Keystore token store), wired with Hilt.
ViewModels expose one immutable `StateFlow` of sealed UI states (`Loading / Empty / Error / Content`), so every screen state is explicit and easy to test.
The domain layer has no Android dependencies, which keeps the business rules unit-testable and makes data sources swappable.
I didn't add a use-case layer: with this little logic it would only add boilerplate. It's the next step as the business logic grows.

## 2. Offline Support
**Room is the single source of truth.** Screens only observe the database through `Flow`s, and network calls only write into it (`OfflineFirstCourseRepository`).
- A course refresh replaces the cached course list and prefetches every course's lessons, so details also open offline.
- Offline or failing refresh: cached courses stay on screen with an offline banner. The full-screen error appears only when there is no cache.
- Marking a lesson complete writes to Room first (`isPendingSync = 1`), so the UI and progress update instantly. The API call is then attempted, and lessons still pending are retried on the next refresh.
- Completion only moves forward: when server data is merged, a local completion is never overwritten by a stale response (`LessonMerger`).
- Progress is derived from cached lessons (`ProgressCalculator`). It is never stored as a separate value, so it can't drift.
- **Per-user cache:** every row is keyed by `userId`, and queries only read the logged-in user's rows. Logout ends the session but keeps the data, so the same user gets their progress back instantly, offline too, and other accounts never see it.
- Logout first tries to push pending completions. If some can't be sent (offline), the user is warned and can stay or log out; the completions sync on that user's next login.
- A saved session skips login, so cached data is reachable after an offline app restart. The connection returning triggers a refresh automatically.

## 3. Security
Tokens are encrypted with an AES-256-GCM key in the **Android Keystore** (hardware-backed where available), and only the ciphertext is stored (`KeystoreSessionStore`). A 401 from the server ends the session and returns the user to login with a "session expired" message.
In production I'd also: keep a short-lived access token plus a rotating refresh token handled by an OkHttp `Authenticator`; enable certificate pinning; disable backups of auth data (done); encrypt the database with SQLCipher if course data were sensitive; and keep R8 on for release builds (done).

## 4. Scale (1M users, hundreds of courses)
1. **Pagination:** use Paging 3 with a `RemoteMediator` over Room for the course list, and fetch lessons lazily per course instead of prefetching all of them.
2. **Reliable sync:** move pending lesson completions to **WorkManager** (network constraint, exponential backoff, idempotent server endpoint), and add delta sync with `updatedSince`/ETags instead of full reloads.
3. **Modularisation:** split into feature and core Gradle modules for build speed, team ownership and clear boundaries. Add baseline profiles for startup.
4. **Observability:** Crashlytics, structured logging, performance/ANR monitoring, and feature flags with staged rollouts.
5. **Quality gates:** CI with unit, Room/UI instrumentation and screenshot tests, plus migration tests for the exported Room schema (`app/schemas`).

## 5. Second Platform (iOS/macOS)
The same layers map directly: **SwiftUI** views → `@Observable` view models publishing an enum UI state → repository protocols → `URLSession` + `async/await` with `Codable` DTOs and **SwiftData** (or Core Data) as the offline cache.
`NavigationStack` handles navigation, `NWPathMonitor` replaces `ConnectivityManager`, the **Keychain** holds the token, and a `URLProtocol` subclass plays the role of the mock interceptor.
Pending completions sync via `BGTaskScheduler`. Tests use XCTest/Swift Testing with protocol fakes. A shared SwiftUI codebase covers both iOS and macOS.

## Tests
- `DashboardViewModelTest`: cached courses stay visible when offline; error state without a cache; empty state; retry recovers; logout warns about unsynced completions.
- `LessonMergerTest`: a local completion survives a stale server response; the pending flag clears on confirmation.
- `ProgressCalculatorTest`: rounding and zero lessons.
