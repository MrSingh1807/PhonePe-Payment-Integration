package com.example.phonepeintegration.paymentManager.models

data class ServerPayload(
    val merchantId: String,
    val merchantTransactionId: String,
    val merchantUserId: String,
    val amount: Long,
    val mobileNumber: String,
    val callbackUrl: String,
    val paymentInstrument: PaymentInstrument,
    val deviceContext: Device
)


data class PaymentInstrument(
    val type: String,
    val targetApp: String
)

data class Device(
    val deviceOS: String
)