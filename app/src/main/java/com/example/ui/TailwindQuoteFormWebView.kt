package com.example.ui

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.GoldPrimary
import com.example.viewmodel.ServiceViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TailwindQuoteFormWebView(
    viewModel: ServiceViewModel,
    modifier: Modifier = Modifier,
    onQuoteSubmitted: () -> Unit = {}
) {
    var isLoaded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = CharcoalSurface,
        border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("tailwind_quote_webview")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(720.dp)
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoaded = true
                            }
                        }
                        
                        // Add JavaScript Interface to safely transmit form values back to our SaaS logic
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun submitQuote(
                                name: String,
                                email: String,
                                phone: String,
                                address: String,
                                services: String,
                                method: String,
                                notes: String,
                                bookingDate: String
                            ) {
                                post {
                                    viewModel.nameInput.value = name
                                    viewModel.emailInput.value = email
                                    viewModel.phoneInput.value = phone
                                    viewModel.addressInput.value = address
                                    viewModel.selectedMappingMethod.value = method
                                    viewModel.notesInput.value = notes
                                    viewModel.bookingDateInput.value = bookingDate
                                    
                                    // Map comma-separated string back to services set
                                    val serviceSet = services.split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                        .toSet()
                                    viewModel.selectedServices.value = serviceSet
                                    
                                    // Process the quote logic and start Lead Generation Sequence (Stage 1)
                                    viewModel.submitQuoteRequest()
                                    onQuoteSubmitted()
                                }
                            }
                        }, "AndroidInterface")

                        loadUrl("file:///android_asset/quote_form.html")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (!isLoaded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CharcoalSurface),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GoldPrimary)
                }
            }
        }
    }
}
