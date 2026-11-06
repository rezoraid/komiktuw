# Android performance second pass

- Reader chapter loading now cancels superseded requests and ignores late results for chapters that are no longer selected.
- Existing reader content stays visible while a target chapter is being fetched, avoiding an unnecessary full-screen loading transition.
- Detail chapter-count and like-state collectors are single-instance per ViewModel method call; repeated calls cancel their previous collector.

Build verification is still required in an Android SDK + Gradle environment.

## Network retry correction

Removed the synchronous retry interceptor that called `Thread.sleep()` on OkHttp dispatcher threads. This prevented sleeping network threads from being occupied during transient failures. OkHttp's built-in `retryOnConnectionFailure(true)` remains enabled; user-facing retry/backoff should be coroutine-based at the repository/use-case layer if a specific idempotent GET requires it.

## Penyesuaian dengan backend v6

- **Komentar tanpa ganda:** setiap kiriman komentar/balasan membawa `clientId` (UUID) yang sama di semua percobaan. Jika respons hilang (timeout/jaringan putus), aplikasi mengirim ulang sekali dengan `clientId` itu dan server mengembalikan komentar yang sudah ada (tanpa komentar ganda, tanpa XP ganda). Pencarian komentar (`findPosted`) tetap jadi cadangan terakhir.
- **Foto profil berversi:** server kini memberi `picture` berbentuk `.../avatar?v=<waktu ganti>`. `AvatarBust` memakai URL itu apa adanya, sehingga cache gambar server/Coil tetap efektif dan foto baru langsung punya alamat baru.
- **Cache HTTP:** respons publik membawa `stale-while-revalidate`; GET personal memakai `private, no-cache` + ETag, sehingga cache OkHttp 50 MB benar-benar dipakai (304 tanpa body).
