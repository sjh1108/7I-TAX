package com.ssafy.seveniTax.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.Background
import com.ssafy.seveniTax.ui.theme.Primary
import com.ssafy.seveniTax.ui.theme.Typography
import com.ssafy.seveniTax.viewmodel.AuthViewModel
import com.ssafy.seveniTax.viewmodel.AutoLoginResult

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val autoLoginResult by viewModel.autoLoginResult.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkAutoLogin()
    }

    LaunchedEffect(autoLoginResult) {
        when (autoLoginResult) {
            AutoLoginResult.SUCCESS -> navController.navigate(Route.PayIntro.path) {
                popUpTo(Route.Splash.path) { inclusive = true }
            }
            AutoLoginResult.FAILURE -> navController.navigate(Route.PhoneInput.path) {
                popUpTo(Route.Splash.path) { inclusive = true }
            }
            AutoLoginResult.NONE -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Primary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "7iTAX",
            style = Typography.headlineLarge,
            color = Background,
            fontSize = 48.sp
        )
    }
}
