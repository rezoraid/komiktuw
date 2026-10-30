# Komikta Android

## UI dan performa
- Tema monokrom hitam-putih dengan font Poppins, tanpa glassmorphism dan tanpa navigasi mengambang.
- Navigasi tab tanpa animasi transisi; loading memakai skeleton statis.
- Explore memuat satu halaman awal; halaman berikutnya dimuat saat mendekati akhir daftar.
- Daftar Compose memakai stable keys dan contentType.
- Cache memori repository: daftar 60 detik, detail 5 menit, chapter 10 menit.
- Coil memakai memory cache dan disk cache.

## Build
Buka folder ini melalui Android Studio dan jalankan Gradle Sync, lalu Build APK. Arsip ini tidak menyertakan `gradle-wrapper.jar`; jika Android Studio tidak dapat menjalankan wrapper, gunakan Gradle wrapper yang dibuat Android Studio atau tambahkan file wrapper resmi yang sesuai dengan Gradle 8.9.
