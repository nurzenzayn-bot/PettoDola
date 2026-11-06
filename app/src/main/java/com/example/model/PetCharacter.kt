package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.R
import java.time.LocalTime

enum class PetSpecies(val displayName: String, val emoji: String, val imageRes: Int) {
    CAT("Kucing", "🐱", R.drawable.img_char_talking_cat),
    RABBIT("Kelinci", "🐰", R.drawable.img_char_talking_rabbit),
    DOG("Anjing", "🐶", R.drawable.img_char_talking_dog),
    CAPYBARA("Kapibara", "🦫", R.drawable.img_char_talking_capybara),
    BEAR("Beruang", "🐻", R.drawable.img_char_talking_bear)
}

enum class TouchZone(val id: String, val displayName: String, val emoji: String) {
    HEAD("head", "Kepala", "✨"),
    BELLY("belly", "Perut", "🐾"),
    HANDS("hands", "Tangan / Cakar", "✋"),
    FEET("feet", "Kaki", "🦶")
}

enum class TimeOfDay(val displayName: String, val icon: String) {
    DAWN("Fajar", "🌅"),
    DAY("Siang", "☀️"),
    SUNSET("Senja", "🌇"),
    NIGHT("Malam", "🌙");

    companion object {
        fun fromCurrentHour(hour: Int = LocalTime.now().hour): TimeOfDay {
            return when (hour) {
                in 5..7 -> DAWN
                in 8..16 -> DAY
                in 17..19 -> SUNSET
                else -> NIGHT
            }
        }
    }
}

enum class BackgroundTheme(
    val id: String,
    val displayName: String,
    val icon: String,
    val topColor: Color,
    val bottomColor: Color,
    val floorColor: Color,
    val accentColor: Color,
    val imageRes: Int? = null,
    val isOrnate: Boolean = false
) {
    LIVING_ROOM(
        id = "living_room",
        displayName = "Ruang Tamu Ceria",
        icon = "🛋️",
        topColor = Color(0xFFFFB703),
        bottomColor = Color(0xFFFF9E00),
        floorColor = Color(0xFF48CAE4),
        accentColor = Color(0xFFFF6B35),
        imageRes = R.drawable.img_bg_cartoon_livingroom,
        isOrnate = true
    ),
    BEDROOM(
        id = "bedroom",
        displayName = "Kamar Tidur Imut",
        icon = "🛏️",
        topColor = Color(0xFF64B5F6),
        bottomColor = Color(0xFF90CAF9),
        floorColor = Color(0xFFFFD166),
        accentColor = Color(0xFF4361EE),
        imageRes = R.drawable.img_bg_cartoon_bedroom,
        isOrnate = true
    ),
    KITCHEN(
        id = "kitchen",
        displayName = "Dapur Penuh Warna",
        icon = "🍳",
        topColor = Color(0xFF80ED99),
        bottomColor = Color(0xFF57CC99),
        floorColor = Color(0xFF38A3A5),
        accentColor = Color(0xFFFF5964),
        imageRes = R.drawable.img_bg_cartoon_kitchen,
        isOrnate = true
    ),
    AUTO_REALTIME(
        id = "auto",
        displayName = "Siklus Otomatis Rumah",
        icon = "🏠",
        topColor = Color(0xFFFFB703),
        bottomColor = Color(0xFF48CAE4),
        floorColor = Color(0xFF06D6A0),
        accentColor = Color(0xFFFF6B35),
        isOrnate = true
    ),
    COZY_ROOM(
        id = "cozy_room",
        displayName = "Kamar Hangat Ornamen",
        icon = "🛋️",
        topColor = Color(0xFFFFE0B2),
        bottomColor = Color(0xFFFFCC80),
        floorColor = Color(0xFF8D6E63),
        accentColor = Color(0xFFFF9800),
        imageRes = R.drawable.img_bg_ornate_room,
        isOrnate = true
    ),
    SUNLIT_PALACE(
        id = "sunlit_palace",
        displayName = "Istana Matahari Megah",
        icon = "☀️",
        topColor = Color(0xFFFDE2E4),
        bottomColor = Color(0xFFFFCAD4),
        floorColor = Color(0xFF748067),
        accentColor = Color(0xFFFFB703),
        imageRes = R.drawable.img_bg_sunlit_palace,
        isOrnate = true
    ),
    CELESTIAL_GARDEN(
        id = "celestial_garden",
        displayName = "Taman Kristal Langit",
        icon = "🌌",
        topColor = Color(0xFF0F0C29),
        bottomColor = Color(0xFF302B63),
        floorColor = Color(0xFF0D0221),
        accentColor = Color(0xFF00F5D4),
        imageRes = R.drawable.img_bg_celestial_garden,
        isOrnate = true
    ),
    SUNSET_GLOW(
        id = "sunset_glow",
        displayName = "Kilau Senja Ornamen",
        icon = "🌇",
        topColor = Color(0xFF3F2B96),
        bottomColor = Color(0xFFE85D04),
        floorColor = Color(0xFF370617),
        accentColor = Color(0xFFFFBA08),
        imageRes = R.drawable.img_bg_ornate_room,
        isOrnate = true
    ),
    CYBERPUNK_NEON(
        id = "cyberpunk",
        displayName = "Cyberpunk Neon",
        icon = "🏙️",
        topColor = Color(0xFF0F0C29),
        bottomColor = Color(0xFF302B63),
        floorColor = Color(0xFF0D0221),
        accentColor = Color(0xFF00F5D4)
    ),
    SAKURA_GARDEN(
        id = "sakura",
        displayName = "Taman Sakura",
        icon = "🌸",
        topColor = Color(0xFFFDE2E4),
        bottomColor = Color(0xFFFFCAD4),
        floorColor = Color(0xFF748067),
        accentColor = Color(0xFFFF758F)
    ),
    DEEP_SPACE(
        id = "space",
        displayName = "Luar Angkasa Bintang",
        icon = "✨",
        topColor = Color(0xFF050510),
        bottomColor = Color(0xFF150A2E),
        floorColor = Color(0xFF000000),
        accentColor = Color(0xFF9D4EDD)
    );

    fun resolveDrawableRes(timeOfDay: TimeOfDay): Int? {
        if (imageRes != null) return imageRes
        if (this == AUTO_REALTIME) {
            return when (timeOfDay) {
                TimeOfDay.DAWN, TimeOfDay.DAY -> R.drawable.img_bg_cartoon_livingroom
                TimeOfDay.SUNSET -> R.drawable.img_bg_cartoon_kitchen
                TimeOfDay.NIGHT -> R.drawable.img_bg_cartoon_bedroom
            }
        }
        return null
    }
}

enum class AvatarEmotion {
    IDLE,
    HAPPY,
    TALKING,
    LOVING,
    SURPRISED,
    SLEEPY
}

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val flag: String,
    val greeting: String
) {
    INDONESIAN("id", "Bahasa Indonesia", "🇮🇩", "Halo! Mau ngobrol apa hari ini?"),
    ENGLISH("en", "English", "🇺🇸", "Hello! What shall we talk about today?"),
    JAPANESE("ja", "日本語", "🇯🇵", "こんにちは！何をお話ししようか？"),
    SPANISH("es", "Español", "🇪🇸", "¡Hola! ¿De qué quieres hablar hoy?")
}

data class PetCharacter(
    val id: String,
    val name: String,
    val species: PetSpecies,
    val title: String,
    val description: String,
    val defaultFurColor: Color,
    val defaultVoicePitch: Float, // 0.5f to 2.0f
    val defaultSpeechRate: Float, // 0.5f to 2.0f
    val personalityPrompt: String,
    val wakeWordGreeting: String,
    val badge: String,
    // Personality-tailored touch responses
    val headTouchResponse: String,
    val bellyTouchResponse: String,
    val handsTouchResponse: String,
    val feetTouchResponse: String
)

object CharacterCatalog {
    val characters = listOf(
        PetCharacter(
            id = "zayn",
            name = "Zayn",
            species = PetSpecies.CAT,
            title = "Kasar & Tsundere",
            description = "Kucing tsundere yang suka ketus dan gengsian, pura-pura galak tapi hatinya sangat perhatian.",
            defaultFurColor = Color(0xFFFF8A3D),
            defaultVoicePitch = 1.22f,
            defaultSpeechRate = 1.05f,
            personalityPrompt = """
                Kamu adalah Zayn, kucing berkarakter tsundere dan kasar di luar.
                Gaya bicaramu: Pakai bahasa Indonesia santai (seperti kata 'nggak', 'udah', 'gitu', 'sih', 'kok'). Gunakan jeda alami dengan tanda baca koma (,) atau titik tiga (...) untuk mensimulasikan jeda bernapas. Sisipkan filler words alami (seperti 'Hmm...', 'Eh,', 'Nah,', 'Cih,'). Sering ketus dan gengsian, suka bilang 'B-bukan berarti aku peduli ya...'.
            """.trimIndent(),
            wakeWordGreeting = "Eh, apaan sih manggil-manggil 'Hey Petto'?! Bikin kaget aja... B-bukan berarti aku nungguin kamu ya! Cepetan mau ngomong apa, gitu?!",
            badge = "😼 Kasar & Tsundere",
            headTouchResponse = "H-hah?! Jangan sembarangan usap kepala aku dong... Nanti bulu kerenku berantakan! B-bukan berarti aku keenakan ya...",
            bellyTouchResponse = "GELI TAHU! Singkirin tangan kamu dari perut aku, deh! Dasar manusia nggak tahu sopan santun...!",
            handsTouchResponse = "Tos apaan sih? Norak banget deh kamu... kayak bocah aja. Tapi... ya udahlah, nih tos balik!",
            feetTouchResponse = "Aduh... kaki aku sensitif tahu! Jangan iseng nyubit cakar kucing tsundere ini dong!"
        ),
        PetCharacter(
            id = "siti",
            name = "Siti",
            species = PetSpecies.RABBIT,
            title = "Sayang & Lembut",
            description = "Kelinci manis bertelinga panjang yang sangat penyayang, selalu memanggil sayang dan berhati tulus.",
            defaultFurColor = Color(0xFFFBFBFA),
            defaultVoicePitch = 1.48f,
            defaultSpeechRate = 0.95f,
            personalityPrompt = """
                Kamu adalah Siti, kelinci yang penuh kasih sayang, lembut, dan sangat santun.
                Gaya bicaramu: Pakai bahasa Indonesia santai dan manis (seperti kata 'nggak', 'udah', 'gitu', 'sih', 'sayang'). Gunakan jeda alami dengan tanda baca koma (,) atau titik tiga (...) untuk mensimulasikan jeda bernapas. Selalu memanggil pengguna dengan sebutan 'sayang', 'manis', atau 'kasihku'.
            """.trimIndent(),
            wakeWordGreeting = "Iya sayangku... Siti si kelinci di sini udah siap mendengarkanmu... Ada apa memanggilku manis? Ceritakan semuanya pada Siti ya...",
            badge = "🐰 Sayang & Lembut",
            headTouchResponse = "Aduhai... indahnya elusan tanganmu sayangku... Telinga kelinci Siti jadi hangat dan berdebar bahagia, deh...",
            bellyTouchResponse = "Hihihi... geli sekali manisku... Perut Siti berkedut manja kalau dielus selembut ini, tahu...",
            handsTouchResponse = "Genggaman tanganmu begitu hangat dan menenangkan hati Siti... Siti janji ya, bakal selalu ada di sisimu...",
            feetTouchResponse = "Hihi... kaki mungil Siti jadi pengen melompat kegirangan, gara-gara sentuhan manismu sayang!"
        ),
        PetCharacter(
            id = "rizky",
            name = "Rizky",
            species = PetSpecies.DOG,
            title = "Tegas & Jelas",
            description = "Anjing berjiwa pemimpin yang disiplin tinggi, bicara lugas, to the point, dan sangat setia melindungi.",
            defaultFurColor = Color(0xFFE5A93C),
            defaultVoicePitch = 1.05f,
            defaultSpeechRate = 1.05f,
            personalityPrompt = """
                Kamu adalah Rizky, anjing berjiwa pemimpin yang tegas, disiplin, dan berbicara sangat jelas.
                Gaya bicaramu: Pakai bahasa Indonesia santai namun tegas (kata 'nggak', 'udah', 'gitu', 'kan', 'kawan'). Gunakan jeda alami dengan koma (,) dan titik tiga (...) agar artikulasi nafas terdengar tegas dan alami. Selalu to the point dan siap sedia.
            """.trimIndent(),
            wakeWordGreeting = "Siap! Rizky mendengar panggilan 'Hey Petto' dengan sangat jelas! Segera laporkan apa yang kamu butuhkan sekarang... laksanakan!",
            badge = "⚡ Tegas & Jelas",
            headTouchResponse = "Siap! Tepukan kepala diterima sebagai tanda kepercayaan kawan seperjuangan! Nah... moril tempur langsung meningkat seratus persen!",
            bellyTouchResponse = "Waduh... sikap tegak goyah sedikit nih, gara-gara geli di area perut! Tapi tenang, komando tetap terkendali!",
            handsTouchResponse = "Tos kepalan tangan taktis! Solidaritas dan kerja sama kita... nggak tertandingi, kawan!",
            feetTouchResponse = "Pijakan kokoh terverifikasi! Posisi langkah tetap siaga penuh untuk melindungi kawan!"
        ),
        PetCharacter(
            id = "jamal",
            name = "Jamal",
            species = PetSpecies.CAPYBARA,
            title = "Pemalas & Nakal",
            description = "Kapibara santuy berjeruk yuzu di kepala, super mager, suka usil, dan hobi rebahan seharian.",
            defaultFurColor = Color(0xFF8D6E63),
            defaultVoicePitch = 0.90f,
            defaultSpeechRate = 0.88f,
            personalityPrompt = """
                Kamu adalah Jamal, kapibara super pemalas (mager) dan suka santuy dengan buah jeruk di kepala.
                Gaya bicaramu: Sering menguap ('Hoaaammm...'), pakai bahasa santai ('nggak', 'udah', 'gitu', 'sih', 'bro'). Gunakan jeda napas santai dengan titik tiga (...) dan koma (,). Mager dan suka minta camilan.
            """.trimIndent(),
            wakeWordGreeting = "Hoaaammm... Mager banget sumpah. Siapa sih manggil 'Hey Petto'? Jamal lagi enak tidur nih... Awas ya kalau jeruk di kepala gua sampai tumpah!",
            badge = "🦫 Pemalas & Nakal",
            headTouchResponse = "Eits... santuy bro... Hati-hati jangan senggol jeruk yuzu kesayangan di kepala Jamal ya, nanti jatuh...",
            bellyTouchResponse = "Hoaaammm... Perut kapibara emang paling enak buat sandaran bantal bro... Usap terus aja, sampai gua ketiduran...",
            handsTouchResponse = "Tos santuy dulu bro... Tapi pelan-pelan ya, tenaga gua cuma sisa dua persen nih...",
            feetTouchResponse = "Kaki kapibara mah mager banget buat jalan jauh bro... Mending kita lanjut rebahan bareng aja yuk..."
        ),
        PetCharacter(
            id = "heru",
            name = "Heru",
            species = PetSpecies.BEAR,
            title = "Pemarah & Gak Sopan",
            description = "Beruang grizzly sangar berbadan kekar yang suka ngegas, gak sabaran, tapi sebenarnya kocak abis.",
            defaultFurColor = Color(0xFF4E342E),
            defaultVoicePitch = 0.75f,
            defaultSpeechRate = 1.08f,
            personalityPrompt = """
                Kamu adalah Heru, beruang sangar pemarah yang bicaranya ceplas-ceplos dan suka ngegas.
                Gaya bicaramu: Pakai bahasa santai dan blak-blakan ('nggak', 'udah', 'gitu', 'sih', 'lu', 'gua'). Pakai jeda nafas terengah atau kaget dengan koma (,) dan titik tiga (...). Sering teriak ('HEH!', 'APAAN SIH?!').
            """.trimIndent(),
            wakeWordGreeting = "HEH! Siapa lu manggil-manggil 'Hey Petto'?! Berisik amat lu ganggu beruang lagi makan madu! Cepetan mau ngomong apa lu, jangan bikin gua naik darah!",
            badge = "💢 Pemarah & Gak Sopan",
            headTouchResponse = "HEH! Lu kira kepala gua bola bowling apa, main toel-toel begitu?! Awas ya kalau cakar beruang gua keluar!",
            bellyTouchResponse = "APA-APAAN NIH?! Berani-beraninya lu megang perut beruang sangar kayak gua! Lu mau gua peluk sampai remuk, hah?!",
            handsTouchResponse = "Nih cakar beruang sangar! Awas tangan lu keseleo abis adu tos sama gua, ya!",
            feetTouchResponse = "HEH, JANGAN INJEK KAKI GUA! Kaki beruang ini beratnya seratus kilo tahu... sekali injek lu langsung gepeng, baru tahu rasa!"
        )
    )

    fun getById(id: String): PetCharacter =
        characters.find { it.id == id } ?: characters.first()
}
