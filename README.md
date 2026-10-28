# Komikta Android

## UI dan performa
- Tema monokrom hitam-putih dengan font Poppins.
- Halaman Explore memuat satu halaman awal terlebih dahulu; halaman berikutnya dimuat saat mendekati akhir daftar.
- Grid Explore memakai placeholder shimmer dengan ukuran kartu yang sama seperti konten, agar tata letak stabil selama loading.
- Daftar Compose memakai stable keys untuk mengurangi perubahan item yang tidak perlu.
- Cache memori repository: daftar 60 detik, detail 5 menit, chapter 10 menit; jumlah entry dibatasi agar penggunaan memori tetap terkendali.
- Coil memakai memory cache dan disk cache untuk mempercepat gambar saat dibuka kembali.

## Build
Buka folder ini melalui Android Studio dan jalankan Gradle Sync, lalu Build APK. Arsip ini tidak menyertakan `gradle-wrapper.jar`; jika Android Studio tidak dapat menjalankan wrapper, gunakan Gradle wrapper yang dibuat Android Studio atau tambahkan file wrapper resmi yang sesuai dengan Gradle 8.9.
