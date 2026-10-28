# Komikta — Android Comic Reader

Aplikasi Android native untuk membaca manhwa, manga, dan manhua.  
Dibangun dengan pendekatan **Backend First**.

## Teknologi

- Kotlin + Jetpack Compose (UI nanti)
- Architecture: MVVM + Clean-ish layers
- Networking: Ktor Client + Kotlinx Serialization
- DI: Hilt
- Local: Room (history & bookmark) + DataStore
- Image: Coil
- Min SDK: 26 | Target SDK: 35

## Struktur Proyek

```
app/src/main/java/com/komikta/
├── core/
│   ├── network/          # ApiConfig
│   ├── database/         # Room Database
│   └── common/           # Result, utilities
├── data/
│   ├── remote/           # DTOs, API, Mappers
│   ├── local/            # Entities, DAOs
│   └── repository/       # Repository implementations
├── domain/
│   ├── model/            # Domain models
│   ├── repository/       # Repository interfaces
│   └── usecase/          # Use cases
├── feature/              # Feature modules (UI later)
│   ├── home/
│   ├── search/
│   ├── detail/
│   ├── reader/
│   ├── bookmark/
│   └── history/
└── di/                   # Hilt modules
```

## API

Base URL: `https://web-production-b2a1d.up.railway.app`

Sources: `lumos`, `manta`, `shinigami`

Endpoint yang sudah diintegrasikan:
- GET `/api/sources`
- GET `/api/{source}/latest?page=`
- GET `/api/{source}/popular`
- GET `/api/{source}/browse?...`
- GET `/api/{source}/search?q=`
- GET `/api/{source}/genres`
- GET `/api/{source}/comic/{slug}`
- GET `/api/{source}/chapter/{comicSlug}/{chapterSlug}`

## Cara Build APK via GitHub Actions (Cloud)

1. Buat repository baru di GitHub.
2. Upload seluruh isi folder proyek ini.
3. Pastikan file `gradlew` dan `gradle/wrapper` ada (atau generate dengan `gradle wrapper`).
4. Buka tab **Actions** → pilih workflow **Build APK** → **Run workflow**.
5. Setelah selesai, download artifact:
   - `komikta-debug-apk`
   - `komikta-release-apk` (unsigned)

### Generate Gradle Wrapper (jika belum ada)

```bash
gradle wrapper --gradle-version 8.9
```

## Status Saat Ini

| Tahap | Status |
|-------|--------|
| 1. Audit API | ✅ Selesai |
| 2. Fondasi proyek | ✅ Selesai |
| 3. Integrasi backend | ✅ Selesai |
| 4. Database & cache lokal | ✅ Skeleton siap |
| 5. Pengujian | ⏳ Manual / unit test berikutnya |
| 6. UI/UX | ⏳ Menunggu instruksi |

## Catatan

- UI sengaja masih placeholder (`MainActivity` menampilkan teks "Komikta Backend Ready").
- Tidak ada endpoint yang dikarang — semua berasal dari audit live API.
- Bookmark & Reading History disimpan lokal (Room).
- Image loading siap memakai Coil + proxy path dari API jika diperlukan.

---

**Langkah selanjutnya**: Setelah Anda konfirmasi, saya akan menambahkan unit test untuk repository + API parsing, lalu menyiapkan state holder / ViewModel untuk UI.
