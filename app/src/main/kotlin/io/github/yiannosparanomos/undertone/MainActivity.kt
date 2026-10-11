package io.github.yiannosparanomos.undertone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.yiannosparanomos.undertone.core.designsystem.UnderToneTheme
import io.github.yiannosparanomos.undertone.ui.home.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UnderToneTheme {
                HomeScreen()
            }
        }
    }
}
