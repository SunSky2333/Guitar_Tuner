package com.example.guitartuner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.guitartuner.ui.TunerScreen
import com.example.guitartuner.ui.theme.GuitarTunerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GuitarTunerTheme {
                TunerScreen()
            }
        }
    }
}
