# Komikta performance

- Glassmorphism dan navigasi mengambang dihapus; bottom bar kini flat, solid, tanpa animasi dan tanpa ripple.
- Transisi antar tab dimatikan, transisi ke detail/reader hanya fade 120-140 ms.
- Baseline profile (`app/src/main/baseline-prof.txt`) dipasang oleh ProfileInstaller sehingga kode aplikasi, Compose, Coil, OkHttp dan Ktor dikompilasi AOT; ini mengurangi jank scroll dan startup lambat pada APK sideload.
- Home menyimpan snapshot terakhir ke disk dan menampilkannya seketika saat aplikasi dibuka, lalu menyegarkan data di belakang layar.
- Shimmer statis (tanpa animasi tak berhingga).
- Kartu komik tanpa ikon vector, tanpa Surface, tanpa animasi; metadata ditampilkan sebagai teks.
- Hero tanpa auto-slide dan hanya satu lapisan scrim.
- Explore memakai BasicTextField dan chip sederhana (tanpa OutlinedTextField/FilterChip).
- Detail: baris chapter tanpa graphicsLayer alpha dan tanpa ikon vector.
- Reader: rasio halaman disimpan per item (tidak ada state map global yang menginvalidasi semua halaman), kontrol atas/bawah tanpa AnimatedVisibility.
