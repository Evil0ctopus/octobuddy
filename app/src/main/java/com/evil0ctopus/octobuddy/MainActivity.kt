package com.evil0ctopus.octobuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evil0ctopus.octobuddy.ui.PetScreen
import com.evil0ctopus.octobuddy.ui.PetViewModel
import com.evil0ctopus.octobuddy.ui.PetViewModelFactory
import com.evil0ctopus.octobuddy.ui.theme.Brand
import com.evil0ctopus.octobuddy.ui.theme.OctoBuddyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OctoBuddyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent,
                    contentColor = Brand.Foam,
                ) {
                    val viewModel: PetViewModel = viewModel(
                        factory = PetViewModelFactory(applicationContext),
                    )
                    PetScreen(viewModel = viewModel)
                }
            }
        }
    }
}
