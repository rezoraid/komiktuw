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

## Tanpa loading: Detail, Reader, Balasan

- **Detail:** sampul, judul, status, dan rating tampil seketika dari daftar yang sebelumnya dilihat (`ComicPreviewCache`, memori), lalu chapter menyusul. Bila daftar membawa chapter terbaru, chapter itu sudah bisa diketuk; sisanya placeholder shimmer. Tidak ada layar loading penuh.
- **Chapter di-prefetch:** begitu daftar chapter ada, data chapter "Lanjut/Mulai baca" dan chapter terbaru dimuat ke cache, sehingga Reader terbuka dari cache.
- **Reader:** judul bilah atas langsung "Chapter N" (dari judul, daftar chapter, atau slug), tidak pernah "Reader". Spinner diganti placeholder halaman shimmer.
- **Balasan:** halaman pertama balasan untuk komentar yang punya balasan (maks 8) dimuat di latar belakang saat daftar komentar tampil, jadi ketukan "N balasan" langsung menampilkan isinya. Server juga satu round-trip database lebih cepat pada endpoint balasan.

## Prefetch agresif (terasa instan)

- **Home:** begitu daftar tampil, detail 8 komik teratas dimuat ke cache di belakang layar (server sudah menyimpannya hangat). Mengetuk komik membuka detail + chapter lengkap dari cache.
- **Detail:** selain data chapter, 3 halaman pertama chapter "Lanjut/Mulai baca" ikut diunduh ke cache gambar sehingga Reader langsung menampilkan gambar.
- **Server:** pemanas cache menjaga Home + detail/chapter teratas selalu hangat (lihat OPTIMIZATION.md di backend).
