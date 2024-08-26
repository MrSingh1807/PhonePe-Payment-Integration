package com.example.phonepeintegration

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.example.phonepeintegration.paymentManager.PaymentManager
import com.example.phonepeintegration.ui.theme.PhonePeIntegrationTheme
import com.phonepe.intent.sdk.api.PhonePe
import com.phonepe.intent.sdk.api.PhonePeInitException
import com.phonepe.intent.sdk.api.TransactionRequest
import com.phonepe.intent.sdk.api.models.PhonePeEnvironment
import org.json.JSONObject
import java.security.MessageDigest


class MainActivity : ComponentActivity() {

    fun call(b2BPGRequest: TransactionRequest) {
        //For SDK call below function
        try {
            val intent: Intent = PhonePe.getImplicitIntent(this, b2BPGRequest)!!
            startActivityForResult(intent, 1003)
        } catch (e: PhonePeInitException) {
            e.printStackTrace()
            println("Error: ${e.message}")
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val paymentManager = PaymentManager(this)
        PhonePe.init(this, PhonePeEnvironment.SANDBOX, "PGTESTPAYUAT", null)

        val payPagePayload = JSONObject().apply {
            put("merchantId", "PGTESTPAYUAT")
            put("merchantTransactionId", "MT7850590068188104")
            put("merchantUserId", "3453")
            put("amount", 9L)
            put("callbackUrl", "https://www.google.com")
            put("mobileNumber", "8171163739")

            val paymentInstrument = JSONObject().apply {
                put("type", "PAY_PAGE")
            }
            put("paymentInstrument", paymentInstrument)
        }
        val apiEndPoint = "/pg/v1/pay"
        val base64Body =
            Base64.encodeToString(payPagePayload.toString().toByteArray(), Base64.NO_WRAP)
        println("Base 64 Body -> " +  base64Body)
        val salt = "099eb0cd-02cf-4e2a-8aca-3e6c6aff0399"
        val checksum: String = sha256(base64Body + apiEndPoint + salt) + "###" + 1
        println("Checksum -> " +  checksum)

        val requestBuilder =  TransactionRequest.TransactionRequestBuilder()
            .setData(base64Body)
            .setChecksum(checksum)
            .setUrl(apiEndPoint)
            .build()

       /* val b2BPGRequest = B2BPGRequestBuilder()
            .setData(base64Body)
            .setChecksum(checksum)
            .setUrl(apiEndPoint)
            .build()*/

        setContent {
            PhonePeIntegrationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Row {
                            Button(onClick = {
                                call(requestBuilder)
                            }) {
                                Text(text = "UPI Payment ")
                            }
                            Button(onClick = {
                                paymentManager.firePayPageIntent {
                                    startActivityForResult(it, 1001)
                                }
                            }) {
                                Text(text = "Pay Page Payment ")
                            }
                            Button(onClick = { paymentManager.getUpiAppsWhichAreInstalled() }) {
                                Text(text = "Fetch UPI Apps")
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d("MrSingh", "onActivityResult: $requestCode - $resultCode - ${data?.dataString}")
        Log.i("MrSingh", "onActivityResult: ${data?.getStringExtra("MrSingh")}")

        if (resultCode == 1001) {
            Log.i(
                "MrSingh", "onActivityResult: Success - ${data?.dataString}" +
                        "< - > ${data?.data}"
            )
        } else if (resultCode == 1002) {
            Log.i(
                "MrSingh", "onActivityResult: Success - ${data?.dataString}" +
                        "< - > ${data?.data}"
            )
        }
    }

    private val upiSelectLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            Log.d("MrSingh", "Launcher :Success ")
        }

        Log.e("MrSingh", "Error: ${it.data?.dataString}")
    }


    fun sha256(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
