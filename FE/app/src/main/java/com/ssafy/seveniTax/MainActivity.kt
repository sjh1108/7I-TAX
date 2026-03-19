package com.ssafy.seveniTax

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.ssafy.seveniTax.ui.navigation.NavGraph
import com.ssafy.seveniTax.ui.theme.SevenITaxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SevenITaxTheme {
                NavGraph(rememberNavController())
            }
        }
    }
}
