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

## Profil (update)

- Kapsul peran: hanya kata peran (tanpa "Komikta"); Owner/Moderator/Premium punya pendar warna yang lewat berkala.
- Statistik "menit membaca" diganti **chapter dibaca** (angka dari server, tidak bisa diubah dari aplikasi).
- Banner profil gaya TikTok: ketuk banner / ikon kamera, atau menu gerigi → *Ganti banner* / *Hapus banner*. Gambar dipotong tengah ke rasio 2:1 dan dikompres sebelum diunggah.
- Daftar tab **All** dirancang ulang: sampul, judul, chapter terakhir, waktu + jumlah chapter dibaca, tombol *Lanjut*.
- OWNER: di profil pengguna lain muncul tombol **Kelola** (role, XP, hapus banner/foto, blokir).
