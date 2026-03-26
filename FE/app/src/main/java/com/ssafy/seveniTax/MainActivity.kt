package com.ssafy.seveniTax

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.ssafy.seveniTax.ui.navigation.NavGraph
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.SevenITaxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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
        // savedInstanceState != null → 앱이 이미 실행 중 (내부 알림 탭)
        val isAppAlreadyRunning = savedInstanceState != null

        setContent {
            SevenITaxTheme {
                val navController = rememberNavController()

                NavGraph(
                    navController = navController,
                    pendingNavigateTo = if (isAppAlreadyRunning) null else navigateTo
                )

                // 앱 내부에서 알림 탭 → PIN 없이 바로 이동
                if (isAppAlreadyRunning && navigateTo != null) {
                    LaunchedEffect(Unit) {
                        when (navigateTo) {
                            "classification_result" -> navController.navigate(Route.ClassificationLoading.path)
                            "tax_calendar" -> navController.navigate(Route.TaxCalendar.path)
                        }
                    }
                }
            }
        }
    }
}

