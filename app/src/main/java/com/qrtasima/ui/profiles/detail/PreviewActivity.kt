package com.qrtasima.ui.profiles.detail

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.qrtasima.QrTasimaApplication
import com.qrtasima.R
import com.qrtasima.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt

class PreviewActivity : AppCompatActivity() {

    private val detailViewModel: DetailViewModel by viewModels {
        ViewModelFactory((application as QrTasimaApplication).database.packageDao())
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        supportActionBar?.hide()

        val webView = findViewById<WebView>(R.id.preview_webview)
        val packageId = intent.getIntExtra("PACKAGE_ID", -1)

        val assetLoader = WebViewAssetLoader.Builder()
            .setDomain("appassets.androidplatform.net")
            .addPathHandler("/files/", WebViewAssetLoader.InternalStoragePathHandler(this, filesDir))
            .build()

        webView.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false

        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)


        if (packageId != -1) {
            detailViewModel.packageWithContents.observe(this) { packageWithContents ->
                if(packageWithContents != null) {
                    // Update activity background color dynamically for preview
                    packageWithContents.profilePackage.backgroundColor?.let { colorHex ->
                        try {
                            window.decorView.setBackgroundColor(colorHex.toColorInt())
                        } catch (e: Exception) {
                            window.decorView.setBackgroundColor(Color.parseColor("#f0f2f5"))
                        }
                    } ?: run {
                        window.decorView.setBackgroundColor(Color.parseColor("#f0f2f5"))
                    }

                    lifecycleScope.launch {
                        val htmlContent = detailViewModel.generateHtml(
                            packageWithContents.profilePackage,
                            packageWithContents.contents,
                            isForPreview = true
                        )
                        webView.loadDataWithBaseURL(
                            "https://appassets.androidplatform.net/",
                            htmlContent,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                }
            }
            detailViewModel.loadPackage(packageId)
        }
    }
}