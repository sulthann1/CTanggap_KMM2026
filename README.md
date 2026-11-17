# C-Tanggap (kerangka MVP Android) - versi 2: 12 layar Figma

Aplikasi deteksi indikasi benturan untuk pengendara (lingkup: kecelakaan lalu lintas). Mengirim SMS berisi lokasi ke kontak
darurat, lalu eskalasi bertahap ke fasilitas kesehatan, polisi, dan 112 (opt-in, default mode simulasi).

> Status: kerangka kode, BELUM dikompilasi di sini (tidak ada Android SDK). Hanya sintaks Kotlin yang diperiksa dengan
> kotlinc (tidak ada error sintaks). Buka di Android Studio, sinkronkan Gradle, dan perbaiki error kecil yang muncul
> (versi dependensi di `build.gradle.kts` hanyalah contoh).

## Pemetaan 12 layar Figma ke kode
| No | Layar Figma | Composable / berkas |
|---|---|---|
| 1 | Splash | `SplashScreen` (ui/OnboardingScreens.kt) |
| 2 | Izin dan onboarding | `PermissionScreen` (ui/OnboardingScreens.kt), status izin real-time |
| 3 | Beranda aktif | `HomeScreen`, kondisi `running = true` (ui/HomeScreen.kt) |
| 4 | Beranda nonaktif | `HomeScreen`, kondisi `running = false`, tombol SOS tahan 2 detik |
| 5 | Hitungan mundur (pop-up) | `CountdownContent` di `AlertActivity` (ui/AlertActivity.kt) |
| 6 | Status eskalasi | `StatusContent` di `AlertActivity`, data dari `IncidentState` |
| 7 | Kontak darurat | `ContactsScreen` (ui/ContactsScreens.kt) |
| 8 | Tambah kontak | `AddContactScreen` (ui/ContactsScreens.kt) |
| 9 | Pengaturan | `SettingsScreen` (ui/SettingsScreen.kt) |
| 10 | Dialog persetujuan eskalasi | `OptInDialog` (muncul saat saklar eskalasi dihidupkan) |
| 11 | Riwayat | `HistoryScreen`, ringkasan, perjalanan, insiden (ui/HistoryScreens.kt) |
| 12 | Detail insiden | `IncidentDetailScreen`, log pengiriman per tahap |

Layar SMS di ponsel kontak (Figma 09) bukan bagian aplikasi; itu isi pesan yang dibuat `AlertManager.contactMessage()`.

## Yang berubah dari versi 1
- Alur awal: Splash -> Izin/onboarding -> aplikasi utama (`onboardingDone` disimpan di `AppSettings`).
- `AlertActivity` menggantikan `CountdownActivity`: satu Activity dengan dua tampilan (hitungan mundur dan status), mengikuti
  `IncidentState.phase`.
- `IncidentState` (baru): fase insiden dan status tahap 1-4 yang dibaca UI; `AlertManager` memperbaruinya.
- Insiden dibuat sejak hitungan mundur dimulai dan punya status: HITUNG_MUNDUR, TERKIRIM, DITANGANI (kontak membalas YA),
  DIBATALKAN. Riwayat bisa menampilkan insiden yang dibatalkan.
- Entitas `Trip` (baru): perjalanan dan jarak, dicatat oleh `DrivingService` selama mode berkendara.
- Tombol "Kirim Bantuan Segera" (`ACTION_SEND_NOW`) melewati sisa hitungan mundur.
- Pengaturan: nama, info medis, wilayah uji, hitungan mundur, sensitivitas, eskalasi (opt-in dengan dialog), mode simulasi,
  nomor uji, rekam sensor.
- Peta pada layar hitungan mundur diganti teks koordinat (Maps SDK belum dipakai; pengembangan lanjutan).
- Toggle "Hubungi 112 otomatis" di desain Kontak dipindah ke Pengaturan > Eskalasi (satu tempat, dengan dialog opt-in).
  Sesuaikan desain Figma bila ingin konsisten.

## Menjalankan
1. Android Studio (Ladybug atau lebih baru), Open folder `CTanggap`, tunggu Gradle sync.
2. Jalankan di PONSEL ASLI (emulator tidak punya SMS dan sensor yang realistis).
3. Ikuti layar Izin: izin runtime, lokasi latar belakang, lalu matikan optimasi baterai.
4. Tab **Kontak**: tambah minimal 1 kontak (nomor uji milik tim).
5. Tab **Atur**: isi nama dan nomor uji; biarkan **Mode simulasi aktif**.
6. **Beranda**: tahan tombol SOS 2 detik untuk menguji alur tanpa benturan: hitungan mundur -> SMS tahap 1 -> tahap 2 ->
   tahap 3/4 (jika opt-in). Balas SMS dari kontak dengan "YA" untuk menguji penghentian eskalasi.

## Struktur
| Paket | Isi |
|---|---|
| `detection/` | `CrashDetector` (logika murni, ada unit test), `SensorLogger` |
| `service/` | `DrivingService` (foreground service, perjalanan, timer), `Notifier`, `AlarmPlayer` |
| `alert/` | `AlertManager`, `IncidentState`, `SmsReplyReceiver`, `ResponseState`, `PhoneUtil` |
| `location/` | `LocationProvider` |
| `facility/` | `FacilityRepository` (Overpass API + `assets/facilities_fallback.json`) |
| `data/` | Room (kontak, insiden, log, perjalanan), `AppSettings` |
| `ui/` | semua layar Compose, `AlertActivity`, tema (palet Figma), komponen |

## Pengujian
- Unit test: `./gradlew testDebugUnitTest` (5 skenario `CrashDetectorTest`).
- Kalibrasi: aktifkan "Rekam data sensor", lakukan skenario aman, ambil CSV di `Android/data/id.ctanggap.app/files/`.
- JANGAN menguji dengan nomor instansi sungguhan. Biarkan mode simulasi aktif.

## Yang perlu dikerjakan
1. Isi `assets/facilities_fallback.json` dengan faskes dan polisi wilayah uji (nomor diverifikasi lewat telepon).
2. Kalibrasi `impactG` dan `stillStdMaxG` dari data sensor nyata.
3. Ganti font ke Inter (tambah ke res/font) dan samakan bilah navigasi bawah dengan desain (lingkaran krem menonjol).
4. Ujikan di minimal 2 merek ponsel; periksa layanan tidak dimatikan sistem.
5. Distribusi sebagai APK (izin SMS dibatasi Google Play).

## Batasan yang diketahui
- Balasan "YA" hanya terdeteksi jika izin RECEIVE_SMS diberikan dan nomor pengirim cocok dengan kontak.
- Panggilan 112 hanya membuka sambungan; rincian dikirim lewat SMS.
- Android 14+ membatasi full-screen intent; izinkan manual di pengaturan aplikasi bila layar hitungan mundur tidak muncul.
- Deteksi bersifat indikatif; bukan alat diagnosis atau jaminan keselamatan.
