package com.ssafy.seveniTax.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.dashboard.DashboardScreen
import com.ssafy.seveniTax.ui.home.HomeScreen
import com.ssafy.seveniTax.ui.settings.SettingsScreen
import com.ssafy.seveniTax.ui.ai.AiScreen
import com.ssafy.seveniTax.ui.navigation.Route

@Composable
fun MainScreen(navController: NavController) {
    var selectedTab by remember { mutableStateOf(BottomTab.HOME) }

    Scaffold(
        bottomBar = {
            BottomTabBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onPayClick = { navController.navigate(Route.PayIntro.path) }
            )
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (selectedTab) {
            BottomTab.HOME -> HomeScreen(navController, modifier)
            BottomTab.DASHBOARD -> DashboardScreen(navController, modifier)
            BottomTab.SETTINGS -> SettingsScreen(navController, modifier)
            BottomTab.AI -> AiScreen(navController, modifier)
        }
    }
}
