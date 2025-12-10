# 🎱 Billiards-2D

Repository ini adalah implementasi tugas kelompok untuk mata kuliah **Desain dan Pemrograman Berbasis Objek (OOP)**. Proyek ini adalah simulasi permainan billiard 8-Ball yang mendukung multiplayer lokal maupun online.

![Java](https://img.shields.io/badge/Java-17+-orange?style=flat-square&logo=openjdk)
![JavaFX](https://img.shields.io/badge/JavaFX-21-blue?style=flat-square)
![Gradle](https://img.shields.io/badge/Gradle-8.x-green?style=flat-square&logo=gradle)
![Status](https://img.shields.io/badge/Status-In%20Development-yellow?style=flat-square)
![Multiplayer](https://img.shields.io/badge/Multiplayer-TCP%20Ready-success?style=flat-square)

---

## 🚀 Fitur Utama

* **Multiplayer Online:** Main jarak jauh antar komputer (Host & Client) menggunakan koneksi TCP.
* **Local PvP:** Main berdua di satu komputer secara bergantian.
* **Practice Mode:** Mode latihan sendiri tanpa giliran lawan (Solo).
* **Realistic Physics:** Simulasi tumbukan bola, pantulan bantalan, dan gesekan meja yang akurat.
* **UI Modern:** Tampilan Dark Mode dengan Menu Hamburger yang responsif.

---

## 🛠️ Teknologi

| Komponen | Spesifikasi |
|----------|-------------|
| **Bahasa** | Java 17+ |
| **Framework UI** | JavaFX 21 |
| **Build Tool** | Gradle 8.x (Kotlin DSL) |
| **Networking** | Java Sockets (TCP Port 5000) |

---

## ☕ Persiapan (Wajib Install)

Pastikan komputer sudah terinstall:

1.  **Java JDK 17** atau lebih baru.
    * Cek di terminal: `java -version`
2.  **Git** (untuk clone project).

*(Library JavaFX dan dependensi lain akan didownload otomatis oleh Gradle saat pertama kali dijalankan).*

---

## ▶️ Cara Menjalankan Game

Buka terminal/CMD di folder project, lalu jalankan perintah:

**Linux / macOS:**
```bash
./gradlew run
```

**Windows:**
```
gradlew.bat run
```

---

## 🌐 Panduan Main Online (Jarak Jauh)

Game ini menggunakan Port 5000. Agar bisa terhubung antar internet yang berbeda (beda WiFi/Rumah), kita menggunakan tool gratis bernama `bore`.

### Langkah 1: Host (Pemain 1)

1. Buka game, klik menu ☰ -> Online Multiplayer -> Host Game.
    * Status game akan menjadi "Waiting...".
2. Buka Terminal baru, lalu jalankan perintah tunneling menggunakan `bore`:

#### Untuk Linux/macOS:

**Opsi 1: Install via Package Manager (Paling Mudah)**
```bash
# Arch Linux
sudo pacman -S bore

# Setelah install, langsung jalankan:
bore local 5000 --to bore.pub
```

**Opsi 2: Download Manual**
```bash
# Download binary
curl -LO https://github.com/ekzhang/bore/releases/download/v0.5.1/bore-v0.5.1-x86_64-unknown-linux-musl.tar.gz

# Extract file
tar -xzf bore-v0.5.1-x86_64-unknown-linux-musl.tar.gz

# Jalankan
./bore local 5000 --to bore.pub
```

#### Untuk Windows:

**Langkah 1: Download Bore**
1. Buka browser, kunjungi: https://github.com/ekzhang/bore/releases
2. Scroll ke bawah, cari file: **`bore-v0.5.1-x86_64-pc-windows-msvc.zip`**
3. Download file tersebut
4. Extract file `.zip` ke folder yang mudah diakses (contoh: `C:\bore`)

**Langkah 2: Jalankan Bore**
1. Buka **Command Prompt (CMD)** atau **PowerShell**
2. Pindah ke folder tempat bore.exe berada:
   ```cmd
   cd C:\bore
   ```
3. Jalankan perintah:
   ```cmd
   bore.exe local 5000 --to bore.pub
   ```

**Alternatif: Tanpa Pindah Folder**
Jika tidak ingin ribet pindah folder, bisa langsung ketik path lengkapnya:
```cmd
C:\bore\bore.exe local 5000 --to bore.pub
```

3. Terminal akan menampilkan alamat unik, contoh: `bore.pub:38291`.
4. Kirim alamat tersebut ke temanmu (Client).
5. Jangan tutup terminal ini selama bermain!

### Langkah 2: Client (Pemain 2)

1. Buka game.
2. Klik menu ☰ -> Online Multiplayer -> Join Game.
3. Masukkan alamat yang diberikan Host (contoh: `bore.pub:38291`).
4. Klik OK.

---

## 🎮 Kontrol Permainan

* **Membidik:** Gerakkan mouse mengelilingi bola putih.
* **Mengatur Power:** Klik kiri tahan, lalu tarik mouse ke belakang (seperti menarik stik asli).
* **Menembak:** Lepaskan klik kiri.
* **Ball-in-Hand:** Jika terjadi pelanggaran (Foul), pemain bisa memindahkan bola putih dengan cara klik & drag ke posisi yang diinginkan.

---

## ⚠️ Catatan Penting

1. **Firewall:** Jika teman tidak bisa connect, pastikan Firewall di komputer Host mengizinkan koneksi Java.
2. **Port 5000:** Pastikan tidak ada aplikasi lain yang menggunakan port 5000 sebelum menjalankan game.
3. **Tunneling:** Alamat `bore.pub` akan berubah setiap kali Host me-restart terminal. Pastikan selalu kirim alamat baru jika main ulang.