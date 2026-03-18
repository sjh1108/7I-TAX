package com.ssafy.seveniTax.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.R
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
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
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "7iTAX 로고",
                modifier = Modifier.width(200.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "사용자 정보 확인 중...",
                style = Typography.bodyMedium,
                color = BrandPurple
            )
        }
    }
}
