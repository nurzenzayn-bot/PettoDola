package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.AvatarViewModel
import com.example.ui.screens.AvatarRoomScreen
import com.example.ui.theme.PawPalTheme

class MainActivity : ComponentActivity() {
    private val avatarViewModel: AvatarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.util.configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            PawPalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    AvatarRoomScreen(viewModel = avatarViewModel)
                }
            }
        }
    }
}
