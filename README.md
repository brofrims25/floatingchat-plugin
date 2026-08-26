# FloatingChat

Plugin **Minecraft Java (Paper/Spigot)** untuk menampilkan chat pemain sebagai
teks melayang (*floating chat*) di dekat karakter pemain — bisa dilihat oleh
pemain lain, dan kompatibel dengan client **Bedrock** lewat **Geyser**.

Cocok untuk server **1.21.x** (termasuk 1.21.11 dan versi 1.21 lain yang lebih
baru), dan jalan di server yang memakai **Java 17, Java 21, maupun versi Java
yang lebih baru**.

## ✨ Fitur

- Chat melayang di **atas kepala**, **samping** (kiri/kanan), **depan**, atau
  **belakang** karakter pemain — semua bisa diatur lewat `config.yml`.
- Atur **ketinggian**, **jarak**, dan **skala ukuran teks**.
- Atur **durasi tampil** (berapa detik chat melayang sebelum hilang).
- **Word wrap otomatis**: kalau chat kepanjangan, tidak akan terus memanjang
  ke kanan — otomatis pindah baris baru.
- **Perataan teks** bisa diatur: rata **kiri**, **tengah**, atau **kanan**
  (baris baru mengikuti perataan ini).
- **Sensor kata** otomatis, daftar kata bisa diedit bebas di `config.yml`,
  termasuk deteksi trik bypass seperti `a-n-j-i-n-g`.
- Kompatibel dengan pemain **Bedrock** (via Geyser/Floodgate).
- Semua fitur di atas **100% bisa diatur lewat `config.yml`** tanpa perlu
  mengedit kode/compile ulang — cukup `/floatingchat reload`.

## 📂 Isi folder ini

```
FloatingChat/
├── pom.xml                        <- konfigurasi build Maven
├── src/main/java/...              <- kode sumber plugin
├── src/main/resources/plugin.yml  <- info plugin untuk server
├── src/main/resources/config.yml  <- SEMUA pengaturan yang bisa diedit
└── .github/workflows/build.yml    <- build otomatis (GitHub Actions)
```

Kamu **tidak perlu mengerti atau mengedit kode Java sama sekali**. Yang perlu
diedit hanya `config.yml` (opsional, sudah berisi pengaturan default yang
masuk akal).

## 🚀 Cara upload ke GitHub & dapatkan file .jar (tanpa install apa pun)

1. Buat repository baru di GitHub (misalnya bernama `FloatingChat`), pilih
   **Private** atau **Public** sesuai keinginanmu. Jangan centang "Add a
   README" saat membuatnya (biar tidak konflik).
2. Di halaman repo yang baru dibuat, klik **"Add file" → "Upload files"**.
3. **Upload folder ini apa adanya** (semua file & folder termasuk yang
   tersembunyi seperti `.github`) — drag & drop seluruh isi folder
   `FloatingChat` ke halaman upload GitHub tersebut.
   - Kalau upload lewat browser tidak mau membawa folder `.github` (folder
     tersembunyi), gunakan cara alternatif: install **GitHub Desktop**
     (aplikasi, tanpa perlu command line/coding), lalu "Add local repository"
     pilih folder ini, lalu klik **Publish repository**. Ini otomatis membawa
     semua file termasuk `.github`.
4. Klik **"Commit changes"**.
5. Buka tab **"Actions"** di repository-mu. GitHub otomatis mulai meng-compile
   plugin (prosesnya sekitar 1–2 menit).
6. Setelah selesai (tanda centang hijau ✅), klik run tersebut, lalu di bagian
   bawah halaman ada **"Artifacts"** → unduh **FloatingChat-plugin.zip**.
   Di dalam zip itu ada file **`FloatingChat-1.0.0.jar`** — itulah plugin jadi
   yang tinggal kamu taruh di folder `plugins/` server Minecraft-mu.

### Supaya dapat file .jar sebagai "Release" resmi (link download tetap)

1. Di halaman repo, klik **"Releases" → "Create a new release"**.
2. Di kolom "Tag", ketik misalnya `v1.0.0`, lalu klik **"Create new tag"**.
3. Klik **"Publish release"**.
4. Tunggu ± 1-2 menit, GitHub Actions otomatis membangun ulang dan
   **melampirkan file `.jar` langsung ke halaman Release ini** — jadi kamu
   (atau siapapun) bisa unduh langsung dari sana kapan saja tanpa harus
   membuka tab Actions.

## ⚙️ Cara pasang di server

1. Taruh file `FloatingChat-1.0.0.jar` ke folder `plugins/` server
   Paper/Spigot-mu.
2. Restart atau start server.
3. Plugin otomatis membuat file `plugins/FloatingChat/config.yml`.
4. Edit `config.yml` sesuai selera (lihat komentar di dalamnya, semua sudah
   dijelaskan dalam Bahasa Indonesia).
5. Jalankan `/floatingchat reload` di server (atau restart) supaya perubahan
   config diterapkan.

## 🔧 Perintah & izin

| Perintah                  | Keterangan                          | Izin (permission)         |
|---------------------------|--------------------------------------|----------------------------|
| `/floatingchat reload`    | Muat ulang config.yml tanpa restart | `floatingchat.admin` (default: op) |

| Izin                         | Keterangan                                      | Default |
|------------------------------|--------------------------------------------------|---------|
| `floatingchat.use`           | Chat pemain ditampilkan sebagai floating chat    | true (semua pemain) |
| `floatingchat.bypasscensor`  | Chat pemain tidak disensor                       | op |
| `floatingchat.admin`         | Boleh menjalankan `/floatingchat reload`         | op |

## ❗ Catatan tentang versi

- Nama versi Minecraft seperti "1.21.11" itu benar-benar ada (Mojang memang
  memakai penomoran 1.21.x hingga dua digit di belakang koma untuk update
  kecil). Plugin ini di-*build* menggunakan Paper API 1.21.4 dan memakai
  `api-version: '1.21'`, sehingga Paper akan menganggapnya kompatibel dengan
  **semua** versi server 1.21.x (termasuk 1.21.11 dan yang lebih baru).
- Kalau di kemudian hari server sudah pindah ke Minecraft 1.22 ke atas dan ada
  fitur baru yang tidak jalan, cukup beri tahu aku lagi, nanti dibantu update.
- Untuk dukungan Bedrock: pastikan server memakai **Geyser** (dan Floodgate
  kalau mau pemain Bedrock login tanpa akun Java). Plugin ini memakai entity
  `TextDisplay` bawaan Minecraft yang secara native diteruskan Geyser ke
  client Bedrock sebagai teks 3D — tidak perlu plugin tambahan lain.
