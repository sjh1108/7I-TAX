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
import com.ssafy.seveniTax.ui.auth.AuthSuccessScreen
import com.ssafy.seveniTax.ui.auth.IdentityVerificationScreen
import com.ssafy.seveniTax.ui.auth.PinConfirmScreen
import com.ssafy.seveniTax.ui.auth.PinLoginScreen
import com.ssafy.seveniTax.ui.auth.PinSetupScreen
import com.ssafy.seveniTax.ui.auth.SmsAuthScreen
import com.ssafy.seveniTax.ui.auth.SplashScreen
import com.ssafy.seveniTax.ui.card.CardBusinessInfoScreen
import com.ssafy.seveniTax.ui.card.CardChangeScreen
import com.ssafy.seveniTax.ui.card.CardCompleteScreen
import com.ssafy.seveniTax.ui.card.CardDetailScreen
import com.ssafy.seveniTax.ui.card.CardInputScreen
import com.ssafy.seveniTax.ui.card.CardListScreen
import com.ssafy.seveniTax.ui.card.CardOwnerVerifyScreen
import com.ssafy.seveniTax.ui.card.CardSmsScreen
import com.ssafy.seveniTax.ui.card.CardTypeSelectScreen
import com.ssafy.seveniTax.ui.classification.AutoClassificationScreen
import com.ssafy.seveniTax.ui.classification.CategorySelectScreen
import com.ssafy.seveniTax.ui.classification.ClassificationCompleteScreen
import com.ssafy.seveniTax.ui.classification.ClassificationLoadingScreen
import com.ssafy.seveniTax.ui.classification.ClassificationResultScreen
import com.ssafy.seveniTax.ui.classification.MemoAddScreen
import com.ssafy.seveniTax.ui.classification.BulkClassificationLoadingScreen
import com.ssafy.seveniTax.ui.classification.UnclassifiedListScreen
import com.ssafy.seveniTax.ui.main.MainScreen
import com.ssafy.seveniTax.ui.pay.PayBusinessInfoScreen
import com.ssafy.seveniTax.ui.pay.PayCompleteScreen
import com.ssafy.seveniTax.ui.pay.PayConfirmScreen
import com.ssafy.seveniTax.ui.pay.PayIntroScreen
import com.ssafy.seveniTax.ui.pay.PayTermsScreen
import com.ssafy.seveniTax.ui.pay.PayVerifyScreen
import com.ssafy.seveniTax.ui.payment.PaymentCompleteScreen
import com.ssafy.seveniTax.ui.payment.PaymentProcessingScreen
import com.ssafy.seveniTax.ui.payment.QrPaymentScreen
import com.ssafy.seveniTax.ui.book.BookEntryDetailScreen
import com.ssafy.seveniTax.ui.book.BookEntryListScreen
import com.ssafy.seveniTax.ui.book.BookFilterScreen
import com.ssafy.seveniTax.ui.book.BookMemoAddScreen
import com.ssafy.seveniTax.ui.book.ExportDateRangeScreen
import com.ssafy.seveniTax.ui.book.ExportFormatScreen
import com.ssafy.seveniTax.ui.book.ExportPurposeScreen
import com.ssafy.seveniTax.ui.book.TaxReportScreen
import com.ssafy.seveniTax.ui.book.TaxSavingsDetailScreen
import com.ssafy.seveniTax.ui.calendar.NotificationSettingsScreen
import com.ssafy.seveniTax.ui.calendar.TaxCalendarDetailScreen
import com.ssafy.seveniTax.ui.calendar.TaxCalendarScreen
import com.ssafy.seveniTax.ui.test.ServerTestScreen
import com.ssafy.seveniTax.viewmodel.AuthViewModel
import com.ssafy.seveniTax.viewmodel.BookEntryViewModel
import com.ssafy.seveniTax.viewmodel.CardViewModel
import com.ssafy.seveniTax.viewmodel.TaxCalendarViewModel

private const val AUTH_GRAPH_ROUTE = "auth_graph"

@Composable
fun NavGraph(navController: NavHostController) {
    val cardViewModel: CardViewModel = hiltViewModel()
    val taxCalendarViewModel: TaxCalendarViewModel = hiltViewModel()
    val bookEntryViewModel: BookEntryViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = AUTH_GRAPH_ROUTE
    ) {
        composable(Route.ServerTest.path) {
            ServerTestScreen(navController)
        }

        navigation(
            startDestination = Route.Splash.path,
            route = AUTH_GRAPH_ROUTE
        ) {
            composable(Route.Splash.path) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AUTH_GRAPH_ROUTE)
                }
                val viewModel: AuthViewModel = hiltViewModel(parentEntry)
                SplashScreen(navController, viewModel)
            }

            composable(Route.PinLogin.path) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AUTH_GRAPH_ROUTE)
                }
                val viewModel: AuthViewModel = hiltViewModel(parentEntry)
                PinLoginScreen(navController, viewModel)
            }

            composable(Route.IdentityVerify.path) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AUTH_GRAPH_ROUTE)
                }
                val viewModel: AuthViewModel = hiltViewModel(parentEntry)
                IdentityVerificationScreen(navController, viewModel)
            }

            composable(Route.SmsAuth.path) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AUTH_GRAPH_ROUTE)
                }
                val viewModel: AuthViewModel = hiltViewModel(parentEntry)
                SmsAuthScreen(navController, viewModel)
            }

            composable(Route.PinSetup.path) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AUTH_GRAPH_ROUTE)
                }
                val viewModel: AuthViewModel = hiltViewModel(parentEntry)
                PinSetupScreen(navController, viewModel)
            }

            composable(Route.PinConfirm.path) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AUTH_GRAPH_ROUTE)
                }
                val viewModel: AuthViewModel = hiltViewModel(parentEntry)
                PinConfirmScreen(navController, viewModel)
            }

            composable(Route.AuthSuccess.path) {
                AuthSuccessScreen(navController)
            }
        }

        composable(Route.Main.path) {
            MainScreen(navController)
        }

        composable(Route.PayIntro.path) {
            PayIntroScreen(navController)
        }

        composable(Route.PayBusinessInfo.path) {
            PayBusinessInfoScreen(navController)
        }

        composable(Route.PayTerms.path) {
            PayTermsScreen(navController)
        }

        composable(Route.PayConfirm.path) {
            PayConfirmScreen(navController)
        }

        composable(Route.PayVerify.path) {
            PayVerifyScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Route.PayComplete.path) }
            )
        }

        composable(Route.PayComplete.path) {
            PayCompleteScreen(navController)
        }

        composable(Route.QrPayment.path) {
            QrPaymentScreen(navController)
        }

        composable(Route.PaymentProcessing.path) {
            PaymentProcessingScreen(navController)
        }

        composable(Route.PaymentComplete.path) {
            PaymentCompleteScreen(navController)
        }

        composable(Route.CardList.path) {
            CardListScreen(navController, cardViewModel)
        }

        composable(Route.CardTypeSelect.path) {
            CardTypeSelectScreen(navController, cardViewModel)
        }

        composable(
            route = Route.CardInput.path,
            arguments = listOf(navArgument("cardType") { type = NavType.StringType })
        ) { backStackEntry ->
            val cardType = backStackEntry.arguments?.getString("cardType") ?: "personal"
            CardInputScreen(navController, cardViewModel, cardType)
        }

        composable(Route.CardBusinessInfo.path) {
            CardBusinessInfoScreen(navController)
        }

        composable(Route.CardOwnerVerify.path) {
            CardOwnerVerifyScreen(navController)
        }

        composable(Route.CardSms.path) {
            CardSmsScreen(navController, cardViewModel)
        }

        composable(Route.CardComplete.path) {
            CardCompleteScreen(navController, cardViewModel)
        }

        composable(Route.CardChange.path) {
            CardChangeScreen(navController, cardViewModel)
        }

        composable(
            route = Route.CardDetail.path,
            arguments = listOf(navArgument("cardId") { type = NavType.StringType })
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getString("cardId").orEmpty()
            CardDetailScreen(navController, cardViewModel, cardId)
        }

        composable(Route.ClassificationLoading.path) {
            ClassificationLoadingScreen(navController)
        }

        composable(Route.ClassificationResult.path) {
            ClassificationResultScreen(
                navController = navController,
                onConfirm = {
                    // 확인 → 바로 세목 저장 완료
                    navController.navigate(Route.ClassificationComplete.path)
                },
                onChangeCategory = {
                    // 세목 변경 → 카테고리 선택 페이지
                    navController.navigate(Route.CategorySelect.path)
                }
            )
        }

        composable(
            route = Route.CategorySelect.path,
            arguments = listOf(
                navArgument("returnTo") { type = NavType.StringType; defaultValue = "" },
                navArgument("entryId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val returnTo = backStackEntry.arguments?.getString("returnTo") ?: ""
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: -1L
            CategorySelectScreen(
                navController = navController,
                onCategorySelected = { category ->
                    if (returnTo == "book" && entryId > 0) {
                        bookEntryViewModel.updateEntryCategory(entryId, category)
                        navController.navigate(Route.ClassificationComplete.create("book")) {
                            popUpTo(Route.BookEntryList.path) { inclusive = false }
                        }
                    } else {
                        navController.navigate(Route.MemoAdd.path) {
                            popUpTo(Route.CategorySelect.path) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Route.MemoAdd.path) {
            MemoAddScreen(
                navController = navController,
                onSave = { navController.navigate(Route.ClassificationComplete.path) },
                onSkip = { navController.navigate(Route.ClassificationComplete.path) }
            )
        }

        composable(
            route = Route.ClassificationComplete.path,
            arguments = listOf(navArgument("returnTo") {
                type = NavType.StringType; defaultValue = ""
            })
        ) { backStackEntry ->
            val returnTo = backStackEntry.arguments?.getString("returnTo") ?: ""
            ClassificationCompleteScreen(
                navController = navController,
                onConfirm = {
                    if (returnTo == "book") {
                        navController.navigate(Route.BookEntryList.path) {
                            popUpTo(Route.BookEntryList.path) { inclusive = false }
                        }
                    } else {
                        navController.navigate(Route.Main.path) {
                            popUpTo(Route.Main.path) { inclusive = true }
                        }
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
                onAiRecommend = {
                    // AI로 경비 추천 받기 → 일괄 분석 로딩
                    navController.navigate(Route.BulkClassificationLoading.path)
                },
                onReviewAll = {
                    // 항목 한개씩 개별 분류 → 세목 변경 플로우
                    navController.navigate(Route.ClassificationResult.path)
                },
                onTransactionClick = {
                    navController.navigate(Route.ClassificationResult.path)
                }
            )
        }

        composable(Route.BulkClassificationLoading.path) {
            BulkClassificationLoadingScreen(
                navController = navController,
                onComplete = {
                    // 일괄 분석 완료 → 미분류 내역으로 복귀 (AI 추천 표시됨)
                    // AI 추천대로 일괄 확정 또는 개별 선택 가능
                    navController.navigate(Route.UnclassifiedList.path) {
                        popUpTo(Route.BulkClassificationLoading.path) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.BookEntryList.path) {
            BookEntryListScreen(navController, viewModel = bookEntryViewModel)
        }

        composable(Route.BookFilter.path) {
            BookFilterScreen(navController, viewModel = bookEntryViewModel)
        }

        composable(Route.BookMemoAdd.path) {
            BookMemoAddScreen(navController)
        }

        composable(Route.TaxReport.path) {
            TaxReportScreen(navController, bookEntryViewModel)
        }

        composable(Route.TaxSavingsDetail.path) {
            TaxSavingsDetailScreen(navController)
        }

        composable(
            route = Route.BookEntryDetail.path,
            arguments = listOf(navArgument("entryId") { type = NavType.LongType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: 0L
            BookEntryDetailScreen(navController, entryId)
        }

        composable(Route.ExportPurpose.path) {
            ExportPurposeScreen(navController)
        }

        composable(
            route = Route.ExportDateRange.path,
            arguments = listOf(navArgument("purpose") { type = NavType.StringType })
        ) { backStackEntry ->
            val purpose = backStackEntry.arguments?.getString("purpose").orEmpty()
            ExportDateRangeScreen(navController, purpose)
        }

        composable(
            route = Route.ExportFormat.path,
            arguments = listOf(
                navArgument("purpose") { type = NavType.StringType },
                navArgument("startDate") { type = NavType.StringType },
                navArgument("endDate") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val purpose = backStackEntry.arguments?.getString("purpose").orEmpty()
            val startDate = backStackEntry.arguments?.getString("startDate").orEmpty()
            val endDate = backStackEntry.arguments?.getString("endDate").orEmpty()
            ExportFormatScreen(navController, purpose, startDate, endDate)
        }

        composable(Route.TaxCalendar.path) {
            TaxCalendarScreen(navController, viewModel = taxCalendarViewModel)
        }

        composable(Route.NotificationSettings.path) {
            NotificationSettingsScreen(navController, taxCalendarViewModel)
        }

        composable(
            route = Route.TaxCalendarDetail.path,
            arguments = listOf(
                navArgument("taxName") { type = NavType.StringType },
                navArgument("deadline") { type = NavType.StringType },
                navArgument("dDay") { type = NavType.IntType },
                navArgument("description") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val taxName = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("taxName").orEmpty(), "UTF-8"
            )
            val deadline = backStackEntry.arguments?.getString("deadline").orEmpty()
            val dDay = backStackEntry.arguments?.getInt("dDay") ?: 0
            val description = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("description").orEmpty(), "UTF-8"
            )
            TaxCalendarDetailScreen(navController, taxName, deadline, dDay, description, taxCalendarViewModel)
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
