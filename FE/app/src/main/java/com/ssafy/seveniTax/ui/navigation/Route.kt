package com.ssafy.seveniTax.ui.navigation

sealed class Route(val path: String) {
    object Splash : Route("splash")
    object Main : Route("main")
    object Home : Route("home")
    object Dashboard : Route("dashboard")
    object PhoneInput : Route("phone_input")
    object ResidentNumber : Route("resident_number")
    object NameInput : Route("name_input")
    object PinSetup : Route("pin_setup")
    object PinConfirm : Route("pin_confirm")
    object Terms : Route("terms")
    object AuthSuccess : Route("auth_success")
    object PinLogin : Route("pin_login")

    object PayIntro : Route("pay_intro")
    object PayTerms : Route("pay_terms")
    object PayVerify : Route("pay_verify")
    object PayComplete : Route("pay_complete")
    object QrPayment : Route("qr_payment")
    object PaymentProcessing : Route("payment_processing")
    object PaymentComplete : Route("payment_complete")

    object CardList : Route("card_list")
    object CardTypeSelect : Route("card_type_select")
    object CardInput : Route("card_input/{cardType}") {
        fun create(cardType: String) = "card_input/$cardType"
    }
    object CardBusinessInfo : Route("card_business_info")
    object CardOwnerVerify : Route("card_owner_verify")
    object CardSms : Route("card_sms")
    object CardComplete : Route("card_complete")
    object CardChange : Route("card_change")
    object CardDetail : Route("card_detail/{cardId}") {
        fun create(cardId: String) = "card_detail/$cardId"
    }

    object ServerTest : Route("server_test")
}
