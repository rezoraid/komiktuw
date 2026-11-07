# Kontrak server: katalog mandiri Komikta

APK ini memakai kontrak di bawah. **Server (comic-api, arsip `komikta-backend-katalog`) sudah mengimplementasikannya**
(`src/community/catalog.js`), jadi views tampil (`viewCount`), tab **Komik** di Panel Admin berfungsi, dan unggah gambar jalan.
Kalau server yang ter-deploy masih versi lama: views disembunyikan (bukan "-") dan tab Komik menampilkan pesan error dari server.

Semua respons memakai format yang sama: `{ "ok": true, "data": ... }`. Endpoint admin: header `Authorization: Bearer <token>` dan role `OWNER` (selain itu 403).

## 1. Sumber
Hanya `manta`, `shinigami`, `lumos`. Id sumber lain ditolak (404).

## 2. Views dari database Komikta
APK **mengabaikan** field `views` dari scraper. Server mengirim field baru `viewCount` (bilangan bulat, tanpa desimal):

- Item daftar komik (`latest`, `popular`, `browse`, `search`): `viewCount`
- Detail komik: `viewCount`, dan tiap item `chapters[]` punya `viewCount`
- `viewCount` 0 atau tidak ada = tidak ditampilkan di app

### Catat aktivitas
- `POST /api/community/views/comic` body `{ "source": "", "slug": "" }`
- `POST /api/community/views/chapter` body `{ "source": "", "comic": "", "chapter": "" }`
- Header `Authorization` opsional (anonim boleh), header `X-Device-Id` dikirim APK di semua request.
- Respons: `{ "ok": true, "data": { "viewCount": 1234 } }` (total terbaru komik; untuk chapter boleh kosong).
- Anti-spam di server: kunci dedupe `userId` jika login, kalau tidak `deviceId`, per target, jendela 6 jam. Di dalam jendela: balas total saat ini tanpa menambah.
- APK juga menahan pengiriman yang sama selama 30 menit dan tidak menunggu hasilnya.

Skema MongoDB yang disarankan:
```js
// comic_stats    : { _id: "manta|slug", views: Number }           // $inc atomik
// chapter_stats  : { _id: "manta|slug|chapterSlug", views: Number }
// view_dedupe    : { _id: "c|manta|slug|<userId|deviceId>", at: Date }   // TTL index 6 jam (expireAfterSeconds: 21600)
```
Alur: `insertOne` ke `view_dedupe` (duplicate key = sudah dihitung) lalu `findOneAndUpdate({_id}, {$inc:{views:1}}, {upsert:true, returnDocument:"after"})`.
Saat membangun respons daftar: ambil semua stats dengan satu query `_id: {$in: [...]}` (jangan satu per satu).

## 3. Data komik yang bisa diedit OWNER
Koleksi `comic_overrides`: `{ _id: "manta|slug", title, cover, synopsis, genres[], type, status, author, artist, updatedAt }`.
Field yang ada di override menggantikan data scraper di **semua** respons (daftar, pencarian, detail). Field kosong = pakai data scraper.
`type` hanya `manhwa | manhua | manga`; `status` hanya `ongoing | completed | hiatus | cancelled`.
Setelah menyimpan: buang cache server untuk komik itu dan semua daftar (supaya ETag berubah), karena APK memuat ulang dengan revalidasi ETag.
Override juga harus ikut dihitung saat filter `type`/genre di `browse`.

### Endpoint (di bawah `/api/community/admin`)
| Method | Path | Isi |
|---|---|---|
| GET | `/comics/:source/:slug` | Data gabungan (override di atas scraper) + `edited` + daftar chapter |
| PUT | `/comics/:source/:slug` | Simpan override. Body: `title, cover, synopsis, genres[], type, status, author, artist` |
| DELETE | `/comics/:source/:slug/override` | Hapus override, balas data asli |

Bentuk `data` untuk GET dan PUT dan DELETE:
```json
{ "source": "manta", "slug": "x", "title": "", "cover": "", "synopsis": "", "genres": ["Action"],
  "type": "manhwa", "status": "ongoing", "author": "", "artist": "", "viewCount": 0, "edited": false,
  "chapters": [ { "slug": "", "number": "12", "title": "", "viewCount": 0, "edited": false } ] }
```
`cover` boleh path relatif (`/media/<id>`); APK menjadikannya URL penuh.

## 4. Gambar chapter (panel)
Koleksi `chapter_overrides`: `{ _id: "manta|comicSlug|chapterSlug", pages: [url, ...], updatedAt }`.
Kalau ada, endpoint publik `GET /api/:source/chapter/:comic/:chapter` mengembalikan `pages` ini (urutan sama) menggantikan hasil scraper;
URL yang butuh referer tetap lewat image proxy seperti sekarang, URL `/media/...` dikirim apa adanya.

| Method | Path | Isi |
|---|---|---|
| GET | `/comics/:source/:slug/chapters/:chapter/pages` | `{ "pages": [url...], "edited": false }` (override jika ada, kalau tidak hasil scraper) |
| PUT | `/comics/:source/:slug/chapters/:chapter/pages` | Body `{ "pages": [url...] }` = daftar lengkap berurutan. Tolak daftar kosong (400) |
| DELETE | `/comics/:source/:slug/chapters/:chapter/pages` | Hapus override, balas `{ pages, edited:false }` |
| POST | `/admin/media` | `multipart/form-data`, field `file` (gambar). Balas `{ "url": "/media/<id>" }` |

`POST /api/community/admin/media` (khusus OWNER; sesuai yang dipanggil APK): terima `image/jpeg|png|webp|gif`, maksimal 10 MB, cek magic bytes (jangan percaya `Content-Type`).
Railway tidak punya disk permanen, jadi file disimpan di koleksi MongoDB `media` (maks 10 MB per file, di bawah batas dokumen 16 MB) dan disajikan publik lewat `GET /media/:id` (di luar `/api`)
dengan `Cache-Control: public, max-age=31536000, immutable` dan `ETag`. Setelah PUT pages: buang cache chapter itu di server.

## 5. Yang sudah dikerjakan di APK
- Views hanya dari `viewCount`; dikirim lewat `ViewTracker` (Detail dibuka = 1 view komik, chapter dibaca = 1 view chapter).
- Setelah OWNER menyimpan, cache APK dibuang dan Home/Explore/Detail yang sedang hidup memuat ulang otomatis (`CatalogBus`).
