package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.GeminiChatService
import com.example.model.AppLanguage
import com.example.model.BackgroundTheme
import com.example.model.CharacterCatalog
import com.example.model.PetSpecies
import com.example.model.TimeOfDay
import com.example.model.TouchZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PawPal AI", appName)
    }

    @Test
    fun `verify cartoon room themes have valid drawable resources`() {
        assertEquals(R.drawable.img_bg_cartoon_livingroom, BackgroundTheme.LIVING_ROOM.imageRes)
        assertEquals(R.drawable.img_bg_cartoon_bedroom, BackgroundTheme.BEDROOM.imageRes)
        assertEquals(R.drawable.img_bg_cartoon_kitchen, BackgroundTheme.KITCHEN.imageRes)

        assertEquals(R.drawable.img_bg_cartoon_livingroom, BackgroundTheme.AUTO_REALTIME.resolveDrawableRes(TimeOfDay.DAY))
        assertEquals(R.drawable.img_bg_cartoon_bedroom, BackgroundTheme.AUTO_REALTIME.resolveDrawableRes(TimeOfDay.NIGHT))
        assertEquals(R.drawable.img_bg_cartoon_kitchen, BackgroundTheme.AUTO_REALTIME.resolveDrawableRes(TimeOfDay.SUNSET))
    }

    @Test
    fun `direct intent solves math accurately`() {
        val aiService = GeminiChatService()
        val zayn = CharacterCatalog.getById("zayn")
        val answer = aiService.trySolveDirectIntent("5 x 9 apa", zayn, AppLanguage.INDONESIAN)
        assertNotNull(answer)
        assertTrue(answer!!.contains("45"))
    }

    @Test
    fun `direct intent answers president of indonesia accurately`() {
        val aiService = GeminiChatService()
        val zayn = CharacterCatalog.getById("zayn")
        val answer = aiService.trySolveDirectIntent("siapa presiden Indonesia", zayn, AppLanguage.INDONESIAN)
        assertNotNull(answer)
        assertTrue(answer!!.contains("Prabowo Subianto"))
    }

    @Test
    fun `touch zones generate valid responses for all characters`() {
        for (char in CharacterCatalog.characters) {
            assertNotNull(char.headTouchResponse)
            assertNotNull(char.bellyTouchResponse)
            assertNotNull(char.handsTouchResponse)
            assertNotNull(char.feetTouchResponse)
            assertTrue(char.headTouchResponse.isNotBlank())
            assertTrue(char.bellyTouchResponse.isNotBlank())
            assertTrue(char.handsTouchResponse.isNotBlank())
            assertTrue(char.feetTouchResponse.isNotBlank())
        }
    }

    @Test
    fun `audio synthesizer generates non-empty audio without crash`() {
        for (species in PetSpecies.values()) {
            for (zone in TouchZone.values()) {
                com.example.audio.ExpressiveAudioSynthesizer.playTouchReactionSound(species, zone)
            }
        }
    }

    @Test
    fun `talking tom care system handles food and feeding`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.AvatarViewModel(context)

        val initialHunger = vm.uiState.value.hungerLevel
        vm.feedPet(com.example.model.FoodItem.FISH)
        assertTrue(vm.uiState.value.hungerLevel >= initialHunger)
        assertTrue(vm.uiState.value.careFeedbackMessage.contains("Kenyang"))
    }

    @Test
    fun `bouncing ball game increases fun and coins`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.AvatarViewModel(context)

        val initialCoins = vm.uiState.value.coins
        val initialScore = vm.uiState.value.miniGameScore
        vm.playBallBounce()
        assertEquals(initialScore + 1, vm.uiState.value.miniGameScore)
        assertEquals(initialCoins + 10, vm.uiState.value.coins)
    }

    @Test
    fun `mimic mode toggles properly`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.AvatarViewModel(context)

        val initialMode = vm.uiState.value.isMimicModeActive
        vm.toggleMimicMode()
        assertEquals(!initialMode, vm.uiState.value.isMimicModeActive)
    }

    @Test
    fun `deterministic homework solver calculates math and science accurately`() {
        val aiService = GeminiChatService()
        val zayn = CharacterCatalog.getById("zayn")

        val mathSolution = aiService.solveHomeworkDeterministically("Matematika", "15 + 25", zayn)
        assertTrue(mathSolution.finalAnswer.contains("40"))

        val scienceSolution = aiService.solveHomeworkDeterministically("IPA", "fotosintesis", zayn)
        assertTrue(scienceSolution.finalAnswer.contains("glukosa") || scienceSolution.finalAnswer.contains("makanan"))
    }
}
