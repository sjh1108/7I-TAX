package com.ssafy.seveniTax.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
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
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = LogoPurple)) { append("7i") }
                    withStyle(SpanStyle(color = LogoTeal)) { append("t") }
                    withStyle(SpanStyle(color = LogoOrange)) { append("ax") }
                },
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
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
