package com.tick.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.tick.app.data.TickViewModel
import com.tick.app.ui.TickApp

class MainActivity : ComponentActivity() {
    private val viewModel: TickViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TickApp(viewModel = viewModel)
        }
    }
}
