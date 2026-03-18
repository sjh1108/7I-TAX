package com.ssafy.seveniTax.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ssafy.seveniTax.ui.auth.*
import com.ssafy.seveniTax.ui.card.*
import com.ssafy.seveniTax.ui.dashboard.DashboardScreen
import com.ssafy.seveniTax.ui.pay.*

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Route.Splash.path) {

        // ── Auth ──────────────────────────────────────────────
        composable(Route.Splash.path)         { SplashScreen(navController) }
        composable(Route.PhoneInput.path)     { PhoneInputScreen(navController) }
        composable(Route.ResidentNumber.path) { ResidentNumberScreen(navController) }
        composable(Route.CarrierSelect.path)  { CarrierSelectScreen(navController) }
        composable(Route.NameInput.path)      { NameInputScreen(navController) }
        composable(Route.Terms.path)          { TermsScreen(navController) }
        composable(Route.SmsVerify.path)      { SmsVerifyScreen(navController) }
        composable(Route.PinSetup.path)       { PinSetupScreen(navController) }
        composable(Route.PinConfirm.path)     { PinConfirmScreen(navController) }
        composable(Route.AuthSuccess.path)    { AuthSuccessScreen(navController) }

        // ── Pay ───────────────────────────────────────────────
        composable(Route.PayIntro.path)    { PayIntroScreen(navController) }
        composable(Route.PayTerms.path)    { PayTermsScreen(navController) }
        composable(Route.PayVerify.path) {
            PayVerifyScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Route.PayComplete.path) }
            )
        }
        composable(Route.PayComplete.path) { PayCompleteScreen(navController) }

        // ── Card ──────────────────────────────────────────────
        composable(Route.CardList.path)        { CardListScreen(navController) }
        composable(Route.CardTypeSelect.path)  { CardTypeSelectScreen(navController) }
        composable(Route.CardInput.path)       { CardInputScreen(navController) }
        composable(Route.CardOwnerVerify.path) { CardOwnerVerifyScreen(navController) }
        composable(Route.CardSms.path)         { CardSmsScreen(navController) }
        composable(Route.CardComplete.path)    { CardCompleteScreen(navController) }
        composable(Route.CardChange.path)      { CardChangeScreen(navController) }

        // ── Dashboard (WebView) ───────────────────────────────
        composable(Route.Dashboard.path) { DashboardScreen(navController) }
    }
}
