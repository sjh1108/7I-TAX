package com.ssafy.seveniTax.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.ssafy.seveniTax.ui.auth.*
import com.ssafy.seveniTax.ui.card.*
import com.ssafy.seveniTax.ui.classification.*
import com.ssafy.seveniTax.ui.main.MainScreen
import com.ssafy.seveniTax.ui.pay.*
import com.ssafy.seveniTax.ui.payment.*
import com.ssafy.seveniTax.ui.test.ServerTestScreen
import com.ssafy.seveniTax.viewmodel.AuthViewModel

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Route.ServerTest.path) {

        // ── Server Test ───────────────────────────────────────
        composable(Route.ServerTest.path) { ServerTestScreen(navController) }

        // ── Auth (ViewModel 공유) ────────────────────────────
        navigation(startDestination = Route.Splash.path, route = "auth_graph") {

            composable(Route.Splash.path) {
                val parentEntry = remember(it) { navController.getBackStackEntry("auth_graph") }
                val vm: AuthViewModel = hiltViewModel(parentEntry)
                SplashScreen(navController, vm)
            }
            composable(Route.PinLogin.path) {
                val parentEntry = remember(it) { navController.getBackStackEntry("auth_graph") }
                val vm: AuthViewModel = hiltViewModel(parentEntry)
                PinLoginScreen(navController, vm)
            }
            composable(Route.PhoneInput.path) {
                val parentEntry = remember(it) { navController.getBackStackEntry("auth_graph") }
                val vm: AuthViewModel = hiltViewModel(parentEntry)
                PhoneInputScreen(navController, vm)
            }
            composable(Route.ResidentNumber.path) {
                val parentEntry = remember(it) { navController.getBackStackEntry("auth_graph") }
                val vm: AuthViewModel = hiltViewModel(parentEntry)
                ResidentNumberScreen(navController, vm)
            }
            composable(Route.NameInput.path) {
                val parentEntry = remember(it) { navController.getBackStackEntry("auth_graph") }
                val vm: AuthViewModel = hiltViewModel(parentEntry)
                NameInputScreen(navController, vm)
            }
            composable(Route.PinSetup.path) {
                val parentEntry = remember(it) { navController.getBackStackEntry("auth_graph") }
                val vm: AuthViewModel = hiltViewModel(parentEntry)
                PinSetupScreen(navController, vm)
            }
            composable(Route.PinConfirm.path) {
                val parentEntry = remember(it) { navController.getBackStackEntry("auth_graph") }
                val vm: AuthViewModel = hiltViewModel(parentEntry)
                PinConfirmScreen(navController, vm)
            }
            composable(Route.Terms.path) {
                val parentEntry = remember(it) { navController.getBackStackEntry("auth_graph") }
                val vm: AuthViewModel = hiltViewModel(parentEntry)
                TermsScreen(navController, vm)
            }
            composable(Route.SmsVerification.path) {
                val parentEntry = remember(it) { navController.getBackStackEntry("auth_graph") }
                val vm: AuthViewModel = hiltViewModel(parentEntry)
                SmsVerificationScreen(navController, vm)
            }
            composable(Route.AuthSuccess.path) {
                AuthSuccessScreen(navController)
            }
        }

        // ── Main (탭바 포함) ─────────────────────────────────
        composable(Route.Main.path) { MainScreen(navController) }

        // ── Pay ───────────────────────────────────────────────
        composable(Route.PayIntro.path)        { PayIntroScreen(navController) }
        composable(Route.PayBusinessInfo.path) { PayBusinessInfoScreen(navController) }
        composable(Route.PayTerms.path)        { PayTermsScreen(navController) }
        composable(Route.PayConfirm.path)      { PayConfirmScreen(navController) }
        composable(Route.PayVerify.path) {
            PayVerifyScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Route.PayComplete.path) }
            )
        }
        composable(Route.PayComplete.path) { PayCompleteScreen(navController) }

        // ── QR Payment ──────────────────────────────────────────
        composable(Route.QrPayment.path)         { QrPaymentScreen(navController) }
        composable(Route.PaymentProcessing.path)  { PaymentProcessingScreen(navController) }
        composable(Route.PaymentComplete.path)    { PaymentCompleteScreen(navController) }

        // ── Card ──────────────────────────────────────────────
        composable(Route.CardList.path)        { CardListScreen(navController) }
        composable(Route.CardTypeSelect.path)  { CardTypeSelectScreen(navController) }
        composable(
            route = Route.CardInput.path,
            arguments = listOf(navArgument("cardType") { type = NavType.StringType })
        ) { backStackEntry ->
            val cardType = backStackEntry.arguments?.getString("cardType") ?: "personal"
            CardInputScreen(navController, cardType)
        }
        composable(Route.CardBusinessInfo.path) { CardBusinessInfoScreen(navController) }
        composable(Route.CardOwnerVerify.path) { CardOwnerVerifyScreen(navController) }
        composable(Route.CardSms.path)         { CardSmsScreen(navController) }
        composable(Route.CardComplete.path)    { CardCompleteScreen(navController) }
        composable(Route.CardChange.path)      { CardChangeScreen(navController) }
        composable(
            route = Route.CardDetail.path,
            arguments = listOf(navArgument("cardId") { type = NavType.StringType })
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getString("cardId") ?: ""
            CardDetailScreen(navController, cardId)
        }

        // ── AI 세목 자동분류 ─────────────────────────────────
        composable(Route.ClassificationLoading.path) {
            ClassificationLoadingScreen(navController)
        }
        composable(Route.ClassificationResult.path) {
            ClassificationResultScreen(
                navController = navController,
                onConfirm = { navController.navigate(Route.MemoAdd.path) },
                onChangeCategory = { navController.navigate(Route.CategorySelect.path) }
            )
        }
        composable(Route.CategorySelect.path) {
            CategorySelectScreen(
                navController = navController,
                onCategorySelected = { navController.popBackStack() }
            )
        }
        composable(Route.MemoAdd.path) {
            MemoAddScreen(
                navController = navController,
                onSave = { navController.navigate(Route.ClassificationComplete.path) },
                onSkip = { navController.navigate(Route.ClassificationComplete.path) }
            )
        }
        composable(Route.ClassificationComplete.path) {
            ClassificationCompleteScreen(
                navController = navController,
                onConfirm = {
                    navController.navigate(Route.Main.path) {
                        popUpTo(Route.Main.path) { inclusive = true }
                    }
                }
            )
        }
        composable(Route.UnclassifiedList.path) {
            UnclassifiedListScreen(
                navController = navController,
                onBulkConfirm = {
                    navController.navigate(Route.ClassificationComplete.path)
                },
                onReviewAll = {
                    navController.navigate(Route.ClassificationResult.path)
                },
                onTransactionClick = {
                    navController.navigate(Route.ClassificationResult.path)
                }
            )
        }
        composable(Route.AutoClassification.path) {
            AutoClassificationScreen(
                navController = navController,
                onConfirm = { navController.popBackStack() },
                onEditCategory = { navController.navigate(Route.CategorySelect.path) }
            )
        }
    }
}
