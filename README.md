# Komikta Android

## UI dan performa
- Tema gelap dengan aksen violet (`Ui.Accent`) dan font Poppins, tanpa glassmorphism dan tanpa navigasi mengambang.
- Transisi navigasi berupa fade singkat; loading memakai skeleton shimmer hanya saat belum ada data sama sekali.
- Cache dua lapis (`ResponseCache`): memori LRU + Room (`cache_entries`, maksimal 400 entri, usia maksimal 14 hari). Daftar 2 menit, pencarian 5 menit, detail 10 menit, chapter 6 jam, genre 12 jam.
- Stale-while-revalidate: Home, Explore, pencarian, detail, komentar, like, jumlah komentar per chapter, dan profil menampilkan cache seketika (juga setelah aplikasi dimatikan), lalu menyegarkan di latar belakang. UI hanya diperbarui jika datanya berubah.
- Request yang sama berjalan bersamaan digabung jadi satu; request tetap selesai walau layar ditutup.
- Optimistic UI: komentar, balasan, edit, hapus, like komentar, like komik, username, foto profil, dan visibilitas riwayat langsung berubah dan dipulihkan jika server menolak. Ketukan like beruntun digabung sehingga server hanya menerima status akhir.
- Foto profil baru dimuat ke cache gambar sebelum menggantikan pratinjau lokal, sehingga tidak ada kedipan.
- Progres baca dikirim ke server dengan debounce dan retry, bukan setiap perubahan.
- Startup memanaskan koneksi dan data Home sebelum UI tampil; chapter sebelumnya dan berikutnya diprefetch.
- OkHttp: connection pool, retry satu kali untuk GET (error jaringan, 502/503/504), dispatcher lebih longgar. Coil memakai memory cache dan disk cache.
- Daftar Compose memakai stable keys dan contentType.


## Optimasi final (interaksi instan)
- Explore: tidak lagi mengosongkan list saat pull-to-refresh; skeleton hanya jika benar-benar kosong.
- Reader: buka chapter dari prefetch/stack tanpa request; loading hanya jika belum ada konten; chapter memakai stale-while-revalidate (`observeChapter`).
- Chapter content punya `observeChapter` di repository (cache dulu, refresh background, TTL 6 jam).
- Pola yang sudah ada tetap: ResponseCache + Room, request dedup, optimistic UI komentar/like/avatar/username, ProfileCache, debounce progress, prefetch chapter, startup warm-up.

## Build
Buka folder ini melalui Android Studio dan jalankan Gradle Sync, lalu Build APK. Arsip ini tidak menyertakan `gradle-wrapper.jar`; jika Android Studio tidak dapat menjalankan wrapper, gunakan Gradle wrapper yang dibuat Android Studio atau tambahkan file wrapper resmi yang sesuai dengan Gradle 8.9.

## Komunitas (login Google, komentar)
1. Di Google Cloud Console buat OAuth client **Web** (dipakai juga di server sebagai `GOOGLE_CLIENT_ID`) dan OAuth client **Android** untuk package `com.komikta` dan `com.komikta.debug` dengan SHA-1 keystore yang dipakai (keystore `app/komikta-debug.jks`: `19:4D:B1:1D:47:89:70:75:C3:F8:52:E3:F9:5A:64:00:05:27:C1:8B`).
2. Isi `GOOGLE_WEB_CLIENT_ID=<web client id>` di `gradle.properties` (atau `-PGOOGLE_WEB_CLIENT_ID=...`). Kosong = tombol login menampilkan pesan belum dikonfigurasi.
3. Server harus punya `MONGODB_URI`, `GOOGLE_CLIENT_ID`, `AUTH_SECRET` (lihat README comic-api).
Komentar ada di Detail (baris "Komentar") dan di akhir tiap chapter di Reader. Token sesi disimpan di SharedPreferences yang dikecualikan dari backup.

## Rilis
- Versi 1.1.0 (versionCode 2). Signing rilis dibaca dari env/properti `KOMIKTA_STORE_FILE`, `KOMIKTA_STORE_PASSWORD`, `KOMIKTA_KEY_ALIAS`, `KOMIKTA_KEY_PASSWORD`. Kalau kosong, build rilis masih memakai keystore debug (tidak bisa diunggah ke Play Store, buat keystore sendiri dan simpan di luar repo).
- Profile: Hapus akun (wajib Play Store), tautan Ketentuan dan Kebijakan Privasi (halaman `/terms` dan `/privacy` dari server), dan layar Moderasi untuk MODERATOR/OWNER (hapus komentar atau abaikan laporan).

## Profil & komentar (update)

- **Tema monokrom** untuk profil dan komentar: teks putih, latar abu-hitam, ikon putih (palet di `ui/theme/Mono.kt`). Satu-satunya warna lain adalah warna role: Owner biru, Moderator hijau, Premium emas, Member abu netral.
- **Background/banner profil dihapus total**: tidak ada lagi banner, ikon kamera banner, menu *Ganti/Hapus banner*, maupun endpoint banner di server. Header kini bersih: foto profil bercincin putih, nama, @username, kapsul role + level, dan kartu statistik (chapter dibaca, total komentar, bulan bergabung).
- Profil sendiri dan profil orang lain memakai satu tampilan yang sama; teks kosong menyesuaikan ("kamu" vs "pengguna ini").
- Tab **All / Comments / Likes** dirapikan: tab berikon putih, tiap aktivitas jadi kartu (sampul, judul, chapter, waktu, jumlah chapter dibaca, tombol *Lanjut*; komentar tampil dalam kotak kutipan).
- **Aktivitas privat dihapus**: riwayat dan likes selalu publik. Menu *Riwayat & Likes: publik/privat* dan layar "Aktivitas bersifat privat" sudah hilang (server: endpoint `history-visibility` dihapus, tab `all`/`likes` tidak lagi memberi `403`).
- **Komentar**: tiap komentar berupa kartu; username besar (17sp, tebal), di bawahnya badge role dan level (Lv + bintang putih), lalu isi komentar putih, aksi suka/balas/opsi berikon putih. Bagian komentar di akhir chapter memakai gaya yang sama.
- OWNER: di profil pengguna lain muncul tombol **Kelola** (role, XP, hapus foto, blokir).
