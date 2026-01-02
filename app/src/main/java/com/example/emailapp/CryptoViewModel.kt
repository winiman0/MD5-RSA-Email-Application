package com.example.emailapp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CryptoViewModel : ViewModel() {
    // These store data that both fragments can see
    val sharedMessage = MutableLiveData<String>()
    val sharedSignature = MutableLiveData<String>()
    val sharedPublicKey = MutableLiveData<String>()
}