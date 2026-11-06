package com.example.model

object ReactionCatalog {

    fun getWakeWordResponse(character: PetCharacter, language: AppLanguage): String {
        return when (character.id) {
            "zayn" -> when (language) {
                AppLanguage.INDONESIAN -> "Eh, apaan sih manggil-manggil 'Hey Petto'?! Bikin kaget aja... B-bukan berarti aku nungguin kamu ya! Cepetan mau ngomong apa, gitu?!"
                AppLanguage.ENGLISH -> "Huh?! Why are you yelling 'Hey Petto'?! Don't flatter yourself, I wasn't waiting for you! Hurry up, what do you want?!"
                AppLanguage.JAPANESE -> "はあ？！なによ急に大声で呼んで！べ、別に待ってたわけじゃないんだからね！用件は何なの？！"
                AppLanguage.SPANISH -> "¡¿Qué?! ¡¿Por qué gritas 'Hey Petto'?! ¡No creas que te estaba esperando! ¡Habla rápido, qué quieres?!"
            }
            "siti" -> when (language) {
                AppLanguage.INDONESIAN -> "Iya sayangku... Siti di sini udah siap mendengarkanmu... Ada apa memanggilku manis? Ceritakan semuanya pada Siti ya..."
                AppLanguage.ENGLISH -> "Yes my darling, Siti is right here listening... What made you call me, sweetie? Tell me everything."
                AppLanguage.JAPANESE -> "はい愛しい人、シティはここにいるよ... 何かあったの？何でもお話ししてね。"
                AppLanguage.SPANISH -> "Sí mi amor, aquí está Siti escuchándote... ¿Qué necesitas, cariño? Cuéntamelo todo."
            }
            "rizky" -> when (language) {
                AppLanguage.INDONESIAN -> "Siap! Rizky mendengar panggilan 'Hey Petto' dengan sangat jelas! Segera laporkan apa yang kamu butuhkan sekarang... laksanakan!"
                AppLanguage.ENGLISH -> "Loud and clear! Rizky acknowledges the call! State your command or report immediately!"
                AppLanguage.JAPANESE -> "了解！リズキー、呼び出しを明確に確認！要件を直ちに報告せよ！"
                AppLanguage.SPANISH -> "¡Entendido! ¡Rizky escucha tu llamada fuerte y claro! ¡Dime tu orden inmediatamente!"
            }
            "jamal" -> when (language) {
                AppLanguage.INDONESIAN -> "Hoaaammm... Mager banget sumpah. Siapa sih manggil 'Hey Petto'? Jamal lagi enak tidur nih... Awas ya kalau jeruk di kepala gua sampai tumpah!"
                AppLanguage.ENGLISH -> "Yaaawn... So lazy right now. Who's shouting 'Hey Petto'? You ruined my nap... You better have snacks!"
                AppLanguage.JAPANESE -> "ふわぁ〜... だるすぎる。誰だよペロットって呼んだの？せっかく寝てたのに... おやつくれよ。"
                AppLanguage.SPANISH -> "Bostezo... Qué pereza por dios. ¿Quién llama a Petto? Interrumpiste mi siesta... ¡más te vale tener comida!"
            }
            else -> when (language) { // Heru
                AppLanguage.INDONESIAN -> "HEH! Siapa lu manggil-manggil 'Hey Petto'?! Berisik amat lu ganggu gua lagi santai! Cepetan mau ngomong apa lu, jangan bikin gua naik darah!"
                AppLanguage.ENGLISH -> "HEY! Who told you to yell 'Hey Petto'?! Stop being annoying, you're disturbing my peace! Speak fast before I lose my temper!"
                AppLanguage.JAPANESE -> "おいコラ！誰に向かって呼んでんだ？！うるせえんだよ邪魔すんな！用があるなら手短に言え！"
                AppLanguage.SPANISH -> "¡OYE! ¡¿Quién te crees llamándome Petto?! ¡No molestes! ¡Habla rápido antes de que me enoje!"
            }
        }
    }

    fun getPettingReaction(character: PetCharacter, language: AppLanguage): String {
        return when (character.id) {
            "zayn" -> when (language) {
                AppLanguage.INDONESIAN -> listOf(
                    "H-hei... jangan sembarangan elus-elus dong! B-bukannya aku suka ya, dasar!",
                    "Cih! Jangan pegang leherku kayak gitu... geli tahu! B-bukan berarti enak, lho!",
                    "Kenapa sih tanganmu nggak bisa diem?! Ya udah... khusus hari ini gua ijinin elus, gitu!"
                ).random()
                AppLanguage.ENGLISH -> listOf(
                    "H-hey! Don't just pet me without asking! I-it's not like I enjoy it!",
                    "Tch! Stop touching my chin... it tickles! Baka!"
                ).random()
                AppLanguage.JAPANESE -> listOf(
                    "ち、ちょっと！勝手になでないでよ！べ、別に気持ちよくなんかないんだから！",
                    "触るなバカ！...でも、もうちょっとだけなら許してあげる。"
                ).random()
                AppLanguage.SPANISH -> listOf(
                    "¡O-oye! ¡No me acaricies así! ¡N-no es que me guste!",
                    "¡Cielos! ¡Deja de tocarme... qué atrevido!"
                ).random()
            }

            "siti" -> when (language) {
                AppLanguage.INDONESIAN -> listOf(
                    "Aduh sayangku... Elusan tanganmu menyejukkan hati Siti banget, lembut sekali deh...",
                    "Purrr manis... Siti sayang banget sama kamu, makasih ya kasihku...",
                    "Hangat sekali belaianmu manis... Siti ngerasa sangat dicintai, tahu..."
                ).random()
                AppLanguage.ENGLISH -> listOf(
                    "Oh my darling... Your gentle touch warms Siti's heart so much.",
                    "Sweet purrs... Siti loves you so dearly, my sweet human."
                ).random()
                AppLanguage.JAPANESE -> listOf(
                    "愛しい人... あなたの手は本当に温かくて優しいね。大好きだよ。",
                    "ゴロゴロ... シティはずっとあなたのそばにいるからね。"
                ).random()
                AppLanguage.SPANISH -> listOf(
                    "Ay mi amor... Tus caricias son tan dulces y reconfortantes.",
                    "Siti te quiere con todo su corazón, cariño."
                ).random()
            }

            "rizky" -> when (language) {
                AppLanguage.INDONESIAN -> listOf(
                    "Siap! Laporan diterima! Stimulasi sentuhan fisik dinilai aman... dan moril tempur meningkat pesat!",
                    "Disiplin tetap nomor satu kawan, tapi elusan hangat ini... patut diapresiasi!",
                    "Afeksi dikonfirmasi! Nah... Rizky siap melindungi kawan terbaik kapan pun juga!"
                ).random()
                AppLanguage.ENGLISH -> listOf(
                    "Affection confirmed! Morale boosted by 100 percent! Ready for action!",
                    "Physical contact acknowledged! Good job partner!"
                ).random()
                AppLanguage.JAPANESE -> listOf(
                    "スキンシップ確認！士気が100パーセント向上した！感謝する！",
                    "的確ななで方だ！任務遂行の活力になる！"
                ).random()
                AppLanguage.SPANISH -> listOf(
                    "¡Contacto físico aprobado! ¡Moral aumentada al cien por cien!",
                    "¡Excelente trabalho compañero! ¡Rizky está listo para todo!"
                ).random()
            }

            "jamal" -> when (language) {
                AppLanguage.INDONESIAN -> listOf(
                    "Hoaaammm... Iya, pas banget di situ bro, garuk punggung gua... Jangan berhenti ya, mager bangun nih...",
                    "Enak bener dah... Lu emang tukang pijit terbaik gua bro. Abis ini ambilin camilan dong, hehe...",
                    "Duh, ngantuk berat nih... Elus terus aja sampai gua ketiduran ya, jangan berisik..."
                ).random()
                AppLanguage.ENGLISH -> listOf(
                    "Yaaawn... Yeah right there. Scratch my back and don't stop, too lazy to move.",
                    "Feels so lazy and good... Now go fetch me some treats."
                ).random()
                AppLanguage.JAPANESE -> listOf(
                    "あ〜そこそこ... ちょうどかゆいとこ。そのまま揉んでて、動くのめんどくさいから。",
                    "眠くなってきた... 終わったら飯持ってきてくれよ。"
                ).random()
                AppLanguage.SPANISH -> listOf(
                    "Bostezo... Sí, justo ahí. Ráscame la espalda que no me pienso mover.",
                    "Qué rico... Ahora ve por mi comida mientras duermo."
                ).random()
            }

            else -> when (language) { // Heru
                AppLanguage.INDONESIAN -> listOf(
                    "HEH! Tangan lu jangan lancang megang-megang napa! Tapi... hmm, lumayan enak juga sih... Jangan geer lu ya!",
                    "APAAN SIH ELUS-ELUS?! Gua beruang sangar bukan boneka lu! ...Udah terusin aja dikit lagi, jangan berenti!",
                    "LU BERANI MEGANG GUA?! Ya udah deh... berhubung lagi enak di leher, gua maafin lu kali ini!"
                ).random()
                AppLanguage.ENGLISH -> listOf(
                    "HEY! Watch your hands, punk! Don't touch me like I'm a toy! ...Keep going though!",
                    "ARE YOU DUMB?! Petting a fierce bear?! Whatever, don't stop now!"
                ).random()
                AppLanguage.JAPANESE -> listOf(
                    "おいコラ！気安く触ってんじゃねえ！...まあ、悪くはねえけど調子に乗んなよ！",
                    "誰がぬいぐるみだ！でもまあ... 気持ちいいから許してやる！"
                ).random()
                AppLanguage.SPANISH -> listOf(
                    "¡OYE! ¡Cuidado dónde pones las manos! ¡No soy un juguete! ...Pero sigue rascando.",
                    "¡¿ESTÁS LOCO?! ¡Acariciar a una fiera como yo?! ¡No te burles!"
                ).random()
            }
        }
    }

    fun getTouchZoneReaction(character: PetCharacter, zone: TouchZone, language: AppLanguage): String {
        return when (zone) {
            TouchZone.HEAD -> character.headTouchResponse
            TouchZone.BELLY -> character.bellyTouchResponse
            TouchZone.HANDS -> character.handsTouchResponse
            TouchZone.FEET -> character.feetTouchResponse
        }
    }

    fun getCostumeReaction(costume: CostumeStyle, character: PetCharacter, language: AppLanguage): String {
        return when (character.id) {
            "zayn" -> "Hmph! Baju ${costume.displayName} ini... lumayan lah. B-bukan berarti aku suka karena kamu yang milih ya!"
            "siti" -> "Cantik sekali baju ${costume.displayName} ini sayang, Siti merasa anggun sekali memakainya. Terima kasih manis."
            "rizky" -> "Perlengkapan seragam ${costume.displayName} telah terpasang dengan presisi tinggi! Siap untuk inspeksi!"
            "jamal" -> "Baju ${costume.displayName}? Agak ribet sih pakainya, tapi kalau bisa buat tidur ya Jamal pake aja dah."
            else -> "HEH! Baju apaan nih ${costume.displayName}?! Awas aja kalau bikin gua kelihatan konyol di depan orang, lu gua gigit!"
        }
    }

    fun getAccessoryReaction(accessory: AccessoryStyle, character: PetCharacter, language: AppLanguage): String {
        return when (character.id) {
            "zayn" -> "Cih, aksesoris ${accessory.displayName} ini norak tau! Tapi... ya udah aku pake biar kamu gak sedih."
            "siti" -> "Aksesoris ${accessory.displayName} ini sangat manis sayang, Siti menyukainya sepenuh hati."
            "rizky" -> "Aksesori ${accessory.displayName} telah dikonfirmasi terpasang sesuai standar operasional!"
            "jamal" -> "Berat gak nih ${accessory.displayName}? Kalau berat gua lepas ya, mager bawa-bawa barang."
            else -> "APA LAGI INI?! Lu pasang ${accessory.displayName} di gua?! Jangan macam-macam lu sama Heru!"
        }
    }

    fun getFurColorReaction(furName: String, character: PetCharacter, language: AppLanguage): String {
        return when (character.id) {
            "zayn" -> "Warna bulu $furName ini... lumayan cocok sih. Tapi jangan terus-terusan diliatin dong, malu tau!"
            "siti" -> "Bulu $furName ini hangat dan indah sekali sayang, Siti senang kamu memilihnya."
            "rizky" -> "Warna kamuflase bulu $furName telah diaktifkan secara optimal!"
            "jamal" -> "Mau warna $furName kek atau apa kek, yang penting bulunya empuk buat rebahan seharian."
            else -> "Warna $furName?! Gak bikin gua kelihatan jinak kan?! Gua tetep Heru yang paling sangar!"
        }
    }
}
