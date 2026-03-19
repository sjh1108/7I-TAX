package com.ssafy.seveniTax.viewmodel

import androidx.lifecycle.ViewModel
import com.ssafy.seveniTax.data.local.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val secureStorage: SecureStorage
) : ViewModel() {

    // TODO: 백엔드 연동 시 GET /api/cards로 판단하도록 변경
    fun isPayEnrolled(): Boolean = secureStorage.isPayEnrolled()

    fun setPayEnrolled(enrolled: Boolean) {
        secureStorage.setPayEnrolled(enrolled)
    }
}
