package com.ssafy.seveniTax.data.model.auth

enum class Carrier(val displayName: String, val apiValue: String) {
    SKT("SKT", "skt"),
    KT("KT", "kt"),
    LGU("LG U+", "lgu"),
    SKT_MVNO("SKT 알뜰폰", "skt_mvno"),
    KT_MVNO("KT 알뜰폰", "kt_mvno"),
    LGU_MVNO("LG U+ 알뜰폰", "lgu_mvno")
}
