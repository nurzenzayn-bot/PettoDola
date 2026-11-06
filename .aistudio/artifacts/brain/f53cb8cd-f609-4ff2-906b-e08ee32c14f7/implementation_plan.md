# Rencana Implementasi: Upgrade Penuh `gemini-3.8-live` Voice Conversations & Multi-Turn Gemini Chatbot

## 1. Real-Time Voice Conversations dengan `gemini-3.8-live` (Live API)

### A. Klien WebSocket Bidirectional Live API (`GeminiLiveSessionClient.kt` - *File Baru*)
- Mengimplementasikan koneksi *real-time* WebSocket ke endpoint **Gemini Live API (`BidiGenerateContent`)** menggunakan model utama **`gemini-3.8-live`** (dengan fallback otomatis ke `gemini-2.0-flash-live-001` / `gemini-2.0-flash-exp` jika dibutuhkan):
  - **Setup Pesan Awal (`setup`)**: Mengirimkan `model: "models/gemini-3.8-live"`, `generationConfig` dengan `responseModalities: ["AUDIO"]`, konfigurasi suara karakter (`Puck` untuk Zayn, `Aoede` untuk Siti, `Fenrir` untuk Rizky, `Charon` untuk Jamal, `Orus` untuk Heru), serta `systemInstruction` lengkap beserta memori jangka panjang pengguna.
  - **Streaming Audio Masuk (`realtimeInput`)**: Mengirimkan potongan PCM 16kHz 16-bit mono (`audio/pcm;rate=16000`) secara *real-time* dari mikrofon pengguna saat sesi Live / Video Call berlangsung, plus dukungan pengiriman *frame* kamera (`image/jpeg`) saat *Video Call*.
  - **Streaming Audio Keluar (`serverContent`)**: Menerima *chunk* audio PCM 24kHz (`audio/pcm;rate=24000`) dan transkrip teks secara *streaming* dari `gemini-3.8-live`, lalu memutarnya tanpa jeda menggunakan `AudioTrack` berlatensi rendah sambil menggerakkan animasi mulut (*lip-sync*) Petto 3D secara langsung.
  - **Dukungan Interupsi (*Barge-In*)**: Jika pengguna mulai berbicara saat Petto sedang menjawab, `AudioTrack` langsung dihentikan seketika agar percakapan terasa alami seperti telepon sungguhan.

### B. Integrasi di Ruangan 3D & Video Call (`AvatarViewModel.kt` & `FaceToFaceCallView.kt`)
- Menambahkan indikator status koneksi **`⚡ Gemini 3.8 Live API Aktif`** pada *Gemini Live Pill* di ruangan utama dan di layar **Video Call**.
- Jika pengguna belum memasukkan kunci API untuk WebSocket langsung, sistem otomatis beralih mulus ke jalur `Firebase.ai` tanpa memutus percakapan.

---

## 2. Multi-Turn Gemini Chatbot dengan 3 Tingkatan Model (`gemini-4-argon`, `gemini-3.8-flash`, `gemini-3.1-flash-lite`)

### A. Sistem Pemilihan Model Otomatis + Manual (`GeminiModelTier`)
Menambahkan enum `GeminiModelTier` dengan 4 pilihan yang dapat dipilih pengguna atau diatur ke **Auto (Pintar)**:
1. **🤖 Auto (Rekomendasi)**: Secara cerdas memilih model berdasarkan kompleksitas pesan:
   - Pesan singkat / sapaan / obrolan kilat (< 6 kata) → **`gemini-3.1-flash-lite`** (Respons super cepat).
   - Obrolan sehari-hari, curhat, cerita, & diskusi umum → **`gemini-3.8-flash`** (Seimbang & ekspresif).
   - Pertanyaan kompleks, analisis mendalam, logika/matematika, koding, atau penjelasan panjang → **`gemini-4-argon`** (Penalaran tingkat tinggi).
2. **🧠 Kompleks (`gemini-4-argon`)**: Dipilih manual untuk tugas berat & analisis mendalam.
3. **⚡ Umum (`gemini-3.8-flash`)**: Dipilih manual untuk percakapan multi-turn sehari-hari.
4. **🚀 Kilat (`gemini-3.1-flash-lite`)**: Dipilih manual untuk respons instan berlatensi rendah.

### B. Antarmuka Multi-Turn Chatbot Interaktif (`ChatHistoryDialog.kt` & `AvatarRoomScreen.kt`)
- Menyempurnakan jendela obrolan (`ChatHistoryDialog.kt`) menjadi **Multi-Turn Gemini Chatbot Sheet** yang lengkap:
  - Menampilkan **daftar pesan yang dapat digulir (*scrollable thread*)** dengan auto-scroll ke pesan terbaru.
  - Menampilkan **lencana model** pada setiap balasan Petto (mis. `⚡ gemini-3.8-flash`, `🧠 gemini-4-argon`, atau `🚀 gemini-3.1-flash-lite`) serta peran karakter aktif (`system_instruction`).
  - Menyediakan **baris pemilih model (`Auto`, `Argon`, `Flash`, `Flash-Lite`)** di bagian atas ruang obrolan.
  - Menyediakan **kolom input teks multi-turn, chip saran topik cepat, tombol mikrofon suara, dan tombol kirim** langsung di dalam jendela obrolan (sehingga pengguna bisa mengobrol panjang seperti aplikasi chat modern tanpa harus menutup jendela riwayat).

---

## 3. File yang Akan Dibuat / Diperbarui
1. **`app/src/main/java/com/example/ai/GeminiLiveSessionClient.kt`** *(File Baru)*:
   - Klien WebSocket `BidiGenerateContent` untuk `gemini-3.8-live` dengan `AudioRecord` (16kHz PCM in) dan `AudioTrack` (24kHz PCM streaming out).
2. **`app/src/main/java/com/example/model/PetCharacter.kt`** (atau `ChatMessage`):
   - Menambahkan enum `GeminiModelTier` (`AUTO`, `ARGON_COMPLEX`, `FLASH_GENERAL`, `FLASH_LITE_FAST`) dan properti `modelUsed: String` pada `ChatMessage`.
3. **`app/src/main/java/com/example/ai/GeminiChatService.kt`**:
   - Mengimplementasikan *smart model router* (`resolveModelForPrompt`) dan dukungan penuh untuk `gemini-4-argon`, `gemini-3.8-flash`, dan `gemini-3.1-flash-lite` baik melalui `Firebase.ai` maupun Gemini REST API dengan konteks multi-turn penuh.
4. **`app/src/main/java/com/example/ui/components/ChatHistoryDialog.kt`**:
   - Mengubah dialog riwayat pasif menjadi **Multi-Turn Gemini Chatbot** interaktif dengan pemilih model (`Auto` / `gemini-4-argon` / `gemini-3.8-flash` / `gemini-3.1-flash-lite`), *scrollable thread*, indikator *typing*, dan bilah kirim pesan langsung.
5. **`app/src/main/java/com/example/ui/AvatarViewModel.kt`** & **`AvatarRoomScreen.kt`**:
   - Menghubungkan `GeminiLiveSessionClient` (`gemini-3.8-live`) ke sesi suara / *Video Call* serta menghubungkan state pemilihan model `GeminiModelTier` ke antarmuka Chatbot.
