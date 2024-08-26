package com.example.phonepeintegration.paymentManager

import android.content.Context
import android.content.Intent
import android.telecom.PhoneAccount
import android.util.Base64
import android.util.Log
import com.example.phonepeintegration.paymentManager.models.Device
import com.example.phonepeintegration.paymentManager.models.PaymentInstrument
import com.example.phonepeintegration.paymentManager.models.ServerPayload
import com.google.gson.Gson
import com.phonepe.intent.sdk.api.AvailabilityCheckRequest
import com.phonepe.intent.sdk.api.B2BPGRequest
import com.phonepe.intent.sdk.api.B2BPGRequestBuilder
import com.phonepe.intent.sdk.api.CheckPhonePeAvailabilityCallback
import com.phonepe.intent.sdk.api.PhonePe
import com.phonepe.intent.sdk.api.PhonePeInitException
import com.phonepe.intent.sdk.api.ShouldShowMandateCallback
import com.phonepe.intent.sdk.api.ShouldShowMandateCallbackResult
import com.phonepe.intent.sdk.api.ShowPhonePeCallback
import com.phonepe.intent.sdk.api.TransactionRequest
import com.phonepe.intent.sdk.api.UPIApplicationInfo
import com.phonepe.intent.sdk.api.models.PhonePeEnvironment
import org.json.JSONObject
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.UUID


class PaymentManager(private val context: Context) {

    private val merchantId =  "PGTESTPAYUAT"
    private val salt = "099eb0cd-02cf-4e2a-8aca-3e6c6aff0399"  // testAPIKeyValue
    private val saltIndex = 2 // testAPIKeyIndex
    private val testHostURL = "https://api-preprod.phonepe.com/apis/hermes"

    val callBackUrl = "https://webhook.site/a3e89dad-c64e-4ace-a0a4-fc47200baee2"
    val testMerchantTrnsId = System.currentTimeMillis().toString()
    var apiEndPoint: String = "/pg/v1/pay"

    private val paytmPackageName = "net.one97.paytm"
    private val googlePePackageName = "com.google.android.apps.nbu.paisa.user"
    private val phonePePackageName = "com.phonepe.app"
    private val phonePeSimulatorPackageName = "com.phonepe.simulator"

    init {
        //PhonePe.init(Context context, PhonePeEnvironment environment, String merchantId, String appId)
        // app id ->  will be empty string in case of testing

        PhonePe.init(context, PhonePeEnvironment.SANDBOX, merchantId, null)

        PhonePe.checkAvailability(
            object : AvailabilityCheckRequest() {

            },
            object : CheckPhonePeAvailabilityCallback {
                override fun onResponse(isAvailable: Boolean, responseCode: String) {
                    Log.i(
                        "Mr Singh",
                        "checkAvailability: Is Available : $isAvailable - Response Code : $responseCode"
                    )
                }
            })
        PhonePe.isMandateSupported(object : ShouldShowMandateCallback {
            override fun onResponse(result: ShouldShowMandateCallbackResult) {

                Log.i(
                    "Mr Singh",
                    "isMandateSupported:SingleMandateSupported : ${result.isSingleMandateSupported} - \n" +
                            "RecurringMandateSupported : ${result.isRecurringMandateSupported}"
                )
            }
        })

        PhonePe.isUPIAccountRegistered(object : ShowPhonePeCallback {
            override fun onResponse(show: Boolean) {
                Log.i("Mr Singh", "isUPIAccountRegistered: $show")
            }
        })
    }

    private fun payPageIntegration(): B2BPGRequest {

        val payPagePayload = JSONObject().apply {
            put("merchantId", merchantId)
            put("merchantTransactionId", testMerchantTrnsId)
            put("merchantUserId", "3453")
            put("amount", 100L)
            put("callbackUrl", callBackUrl)
            put("mobileNumber", "8171163739")

            val paymentInstrument = JSONObject().apply {
                put("type", "PAY_PAGE")
            }
            put("paymentInstrument", paymentInstrument)
        }

        val base64Body = payPagePayload.toString().payloadBase64()
        val checkSum = sha256(base64Body + apiEndPoint + salt) + "###1"

        Log.i("MrSingh", "serverSideSetup: Payload : $base64Body \nCheckSum : -> $checkSum ")

        /*   return TransactionRequest.TransactionRequestBuilder()
               .setData(base64Body)
               .setChecksum(checkSum)
               .setUrl(apiEndPoint)
               .build()
           */
        return B2BPGRequestBuilder()
            .setData(base64Body)
            .setChecksum(checkSum)
            .setUrl(apiEndPoint)
            .build()

    }

    private fun serverSideSetupUPIIntent(): B2BPGRequest {

        val paymentInstrument = PaymentInstrument(
            type = "UPI_INTENT",
            targetApp = googlePePackageName
        )

        val serverPayload = ServerPayload(
            merchantId = merchantId,
            merchantTransactionId = "MT7850590068188104",
            merchantUserId = "90223250",
            amount = 100,
            mobileNumber = "8171163739",
            callbackUrl = callBackUrl,
            paymentInstrument = paymentInstrument,
            deviceContext = Device(deviceOS = "ANDROID"),
        )


        val json = Gson().toJson(serverPayload)
        Log.d("MrSingh", "serverSideSetup: $json")

        val payload = json.payloadBase64()
        val checkSum = sha256("$payload$apiEndPoint$salt") + "###" + saltIndex

        Log.i("MrSingh", "serverSideSetup: Payload : $payload \nCheckSum : -> $checkSum")

        return B2BPGRequestBuilder()
            .setData(payload)
            .setChecksum(checkSum)
            .setUrl(apiEndPoint)
            .build()


        /* return TransactionRequest.TransactionRequestBuilder()
             .setData(payload)
             .setChecksum(checkSum)
             .setUrl(apiEndPoint)
             .build()*/

    }

    fun getUpiAppsWhichAreInstalled() {
        try {
            PhonePe.setFlowId("MrSingh1807") // Recommended, not mandatory , An alphanumeric string without any special character
            val upiApps: List<UPIApplicationInfo> = PhonePe.getUpiApps()

            upiApps.forEach {
                Log.d(
                    "MrSingh",
                    "getUpiAppsWhichAreInstalled: ${it.packageName} - ${it.applicationName} - ${it.version}"
                )
            }

        } catch (e: PhonePeInitException) {
            e.printStackTrace();
        }
    }

    fun firePayPageIntent(launcher: (launcherIntent: Intent) -> Unit) {
        try {
            payPageIntegration().let {
                PhonePe.getImplicitIntent(context, it, "")?.let { intent ->
                    launcher(intent)
                }
            }
        } catch (e: PhonePeInitException) {
            e.printStackTrace()
        }
    }

    fun fireUPIIntent(launcher: (launcherIntent: Intent) -> Unit) {
        try {
            serverSideSetupUPIIntent().let {
                PhonePe.getImplicitIntent(context, it, googlePePackageName)?.let { intent ->
                    launcher(intent)
                }
            }
        } catch (e: PhonePeInitException) {
            e.printStackTrace()
        }
    }

    private fun String.payloadBase64(): String {
        return Base64.encodeToString(this.toByteArray(Charset.defaultCharset()), Base64.NO_WRAP)
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

}
