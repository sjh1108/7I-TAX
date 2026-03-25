package com.ssafy.seveniTax.viewmodel

import androidx.lifecycle.ViewModel
import com.ssafy.seveniTax.data.local.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val secureStorage: SecureStorage
) : ViewModel() {

    fun getUserName(): String = secureStorage.getUserName().orEmpty()

    fun isPayEnrolled(): Boolean = secureStorage.isPayEnrolled()

    fun setPayEnrolled(enrolled: Boolean) {
        secureStorage.setPayEnrolled(enrolled)
    }
}
