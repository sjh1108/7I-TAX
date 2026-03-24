package com.ssafy.seveniTax

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.ssafy.seveniTax.ui.navigation.NavGraph
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.SevenITaxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        val navigateTo = intent?.getStringExtra("navigate_to")

        setContent {
            SevenITaxTheme {
                val navController = rememberNavController()

                NavGraph(navController)

                // 알림 탭으로 진입 시 해당 화면으로 이동
                if (navigateTo != null) {
                    LaunchedEffect(Unit) {
                        when (navigateTo) {
                            "classification_result" -> navController.navigate(Route.ClassificationResult.path)
                            "tax_calendar" -> navController.navigate(Route.TaxCalendar.path)
                        }
                    }
                }
            }
        }
    }
}
