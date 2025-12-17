# 🎱 Billiards-2D 

Implementasi permainan billiard 2D menggunakan JavaFX dengan simulasi fisika realistis dan antarmuka retro-arcade. Proyek ini dibuat sebagai tugas mata kuliah **Desain dan Pemrograman Berbasis Objek (OOP)** dengan menerapkan prinsip-prinsip OOP seperti inheritance, polymorphism, encapsulation, dan separation of concerns.

## Fitur Utama

- **Dua Mode Permainan**: 8-Ball (kompetitif) dan Arcade (time-attack)
- **Simulasi Fisika Realistis**: Collision detection, momentum transfer, dan friction
- **Antarmuka Retro**: Pixel art, custom fonts, dan efek visual arcade
- **High Score System**: Persistensi skor menggunakan Java Preferences API
- **Dokumentasi Lengkap**: Javadoc berbahasa Indonesia untuk semua public API

## Teknologi

| Komponen | Teknologi | Versi |
|----------|-----------|-------|
| **Bahasa** | Java | 17+ |
| **GUI Framework** | JavaFX | 21 |
| **Build Tool** | Gradle (Kotlin DSL) | 8.14 |
| **IDE** | IntelliJ IDEA | Community Edition |

## Struktur Proyek

```
Billiards-2D/
├── src/main/java/com/billiards2d/
│   ├── core/
│   │   ├── BilliardApp.java          # Main application & JavaFX entry point
│   │   ├── GameObject.java           # Interface untuk game entities
│   │   └── GameConstants.java        # Konstanta konfigurasi global
│   ├── entities/
│   │   ├── balls/
│   │   │   ├── Ball.java             # Abstract base class untuk bola
│   │   │   ├── BallType.java         # Enum tipe bola (Solid/Stripe/8-Ball)
│   │   │   ├── CueBall.java          # Bola putih (cue ball)
│   │   │   └── ObjectBall.java       # Bola target (1-15)
│   │   ├── CueStick.java             # Logika stik biliar
│   │   └── Table.java                # Meja biliar & pocket detection
│   ├── game/
│   │   ├── GameController.java       # Game loop orchestrator
│   │   ├── GameRules.java            # Aturan 8-Ball & Arcade
│   │   └── PhysicsEngine.java        # Simulasi fisika & collision
│   ├── input/
│   │   └── InputHandler.java         # Mouse/keyboard input handler
│   ├── ui/
│   │   ├── SceneManager.java         # Manager scene (Menu/Game/GameOver)
│   │   ├── GameUIRenderer.java       # Renderer utama game canvas
│   │   ├── HUDRenderer.java          # Heads-up display (score, timer, dll)
│   │   └── FloatingText.java         # Efek teks floating untuk feedback
│   └── util/
│       └── Vector2D.java             # Utilitas matematika vektor 2D
├── src/main/resources/assets/        # Sprite, font, dan gambar
├── build.gradle.kts                  # Konfigurasi build & dependensi
└── gradlew / gradlew.bat             # Gradle wrapper

```

## Arsitektur & Design Pattern

Proyek ini menerapkan beberapa prinsip OOP dan design pattern:

- **Separation of Concerns**: Package terpisah untuk core, entities, game logic, input, dan UI
- **Inheritance & Polymorphism**: Hierarchy `Ball` → `CueBall`/`ObjectBall`, interface `GameObject`
- **Encapsulation**: Konstanta terpusat di `GameConstants`, state management di `GameController`
- **Single Responsibility**: Setiap class memiliki tanggung jawab yang jelas dan terbatas
- **MVC-like Pattern**: Pemisahan antara model (entities), controller (game logic), dan view (UI renderers)

## Prerequisites

Sebelum menjalankan proyek, pastikan telah terinstall:

1. **Java JDK 17 atau lebih baru**
   - Download dari [Eclipse Temurin](https://adoptium.net/) atau [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
   - Verifikasi instalasi: `java -version`

2. **Git** (untuk clone repository)

> Gradle dan JavaFX akan diunduh otomatis oleh Gradle Wrapper saat pertama kali build.

## Cara Menjalankan

### Menggunakan IntelliJ IDEA

1. Clone repository:
   ```bash
   git clone https://github.com/Nand-o/Billiards-2D.git
   ```

2. Buka IntelliJ IDEA dan pilih **Open**, lalu pilih folder `Billiards-2D`

3. Tunggu IntelliJ mengunduh dependencies (terlihat di progress bar pojok kanan bawah)

4. Jalankan aplikasi melalui Gradle panel:
   - Klik tab **Gradle** di sisi kanan
   - Navigasi: **Billiards-2D → Tasks → application → run**
   - Double-click **run**

### Menggunakan Terminal

```bash
# Clone repository
git clone https://github.com/Nand-o/Billiards-2D.git
cd Billiards-2D

# Jalankan aplikasi (Windows)
gradlew.bat run

# Jalankan aplikasi (Linux/macOS)
./gradlew run
```

### Generate Javadoc

```bash
# Windows
gradlew.bat javadoc

# Linux/macOS
./gradlew javadoc
```

Dokumentasi akan tersedia di: `build/docs/javadoc/index.html`

## Troubleshooting

**Build gagal / JavaFX error:**
- Pastikan menggunakan Gradle task `run`, jangan run langsung dari `BilliardApp.java`
- Gradle akan otomatis menghandle JavaFX module path

**Gradle sync error:**
- Periksa koneksi internet (Gradle perlu download dependencies)
- Coba: **File → Invalidate Caches → Invalidate and Restart** di IntelliJ

**High DPI display issues:**
- Set VM options: `-Dglass.gtk.uiScale=1.0` atau sesuaikan scaling

## Kontributor

Proyek ini dikembangkan oleh Kelompok 1 sebagai tugas mata kuliah OOP.

Nama Anggota:
| Nama Lengkap        | GitHub                        |
|--------------------|-------------------------------|
| Ernando Febrian     | [Ernando Febrian](https://github.com/Nand-o) |
| Faris Maulana      | [bai](https://github.com/farismlna) |
| Candra Afriansyah        | [Candra Afriansyah](https://github.com/CanLikez) |
| Muhammad Yasyfi Alhafizh       | [Muhammad Yasyfi Alhafizh](https://github.com/noireveil) |

## Lisensi

Proyek ini dibuat untuk keperluan edukasi.
