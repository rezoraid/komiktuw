# Endpoint server untuk Panel Admin

Semua di bawah `/api/community/admin/*`, wajib `Authorization: Bearer <token>` dan role `OWNER` (selain itu balas 403). Format respons sama seperti endpoint lain: `{ "ok": true, "data": ... }`.

## Sudah ada
- `GET /admin/users/:id`, `POST /admin/users/:id/xp`, `PUT /admin/users/:id/role`, `PUT /admin/users/:id/ban`, `POST /admin/users/:id/reset-media`
- `GET /admin/reports`, `POST /admin/reports/:id/resolve`
- `DELETE /comments/:id` (harus mengizinkan OWNER menghapus komentar siapa pun)

## Perlu ditambah di server
### GET /admin/stats
```json
{ "users": 0, "newUsers7d": 0, "active24h": 0, "banned": 0, "owners": 0, "moderators": 0, "premiums": 0, "comments": 0, "comments24h": 0, "openReports": 0, "chaptersRead": 0 }
```
`active24h` = `lastLoginAt` dalam 24 jam, `newUsers7d` = `joinedAt` dalam 7 hari, `comments` hanya yang belum dihapus.

### GET /admin/users?q=&role=&banned=1&page=1
`q` mencari username / name / email (case-insensitive). `role` = OWNER | MODERATOR | PREMIUM | USER. `banned=1` hanya akun diblokir. 30 item per halaman, urut `joinedAt` terbaru.
```json
{ "items": [ { "id": "", "username": "", "name": "", "picture": "", "role": "USER", "level": 1, "banned": false, "lastLoginAt": 0 } ], "hasMore": false, "page": 1, "total": 0 }
```

### GET /admin/comments?q=&reported=1&page=1
`q` mencari isi komentar / username. `reported=1` hanya yang `reportCount > 0`. Termasuk komentar yang sudah dihapus. 30 item per halaman, urut terbaru.
```json
{ "items": [ { "id": "", "body": "", "deleted": false, "targetKey": "", "createdAt": 0, "likeCount": 0, "replyCount": 0, "reportCount": 0, "user": { "id": "", "username": "", "role": "USER" } } ], "hasMore": false, "page": 1, "total": 0 }
```
