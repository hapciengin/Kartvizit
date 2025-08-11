package com.qrtasima.viewmodel

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.qrtasima.data.PackageDao
import com.qrtasima.data.ProfilePackage
import com.qrtasima.network.GistFileContent
import com.qrtasima.network.GistRequest
import com.qrtasima.network.GistResponse
import com.qrtasima.network.RetrofitClient
import com.qrtasima.ui.profiles.detail.DetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URLEncoder
import java.util.Base64

class MainViewModel(private val dao: PackageDao) : ViewModel() {

    val allPackages: LiveData<List<ProfilePackage>> = dao.getAllPackages().asLiveData()

    private val _qrCodeUrl = MutableLiveData<String?>()
    val qrCodeUrl: LiveData<String?> get() = _qrCodeUrl

    private val _quickSendQrUrl = MutableLiveData<String?>()
    val quickSendQrUrl: LiveData<String?> get() = _quickSendQrUrl

    private val GITHUB_TOKEN = "token"
    private val GITHUB_USERNAME = "kullanici_adi"

    fun insert(profilePackage: ProfilePackage) = viewModelScope.launch {
        withContext(Dispatchers.IO) { dao.insertPackage(profilePackage) }
    }

    fun update(profilePackage: ProfilePackage) = viewModelScope.launch {
        withContext(Dispatchers.IO) { dao.updatePackage(profilePackage) }
    }

    fun delete(profilePackage: ProfilePackage) = viewModelScope.launch {
        withContext(Dispatchers.IO) { dao.deletePackage(profilePackage) }
    }

    fun generateQrForPackage(packageId: Int) = viewModelScope.launch {
        _qrCodeUrl.postValue(null)

        try {
            val packageData = withContext(Dispatchers.IO) {
                dao.getPackageWithContents(packageId).first()
            }

            val htmlContent = DetailViewModel(dao).generateHtml(packageData.profilePackage, packageData.contents, isForPreview = false) // isForPreview eklendi
            val files = mapOf("index.html" to GistFileContent(htmlContent))
            val gistRequest = GistRequest("QR Tasima Profile Page: ${packageData.profilePackage.name}", true, files)
            val response = RetrofitClient.instance.createGist("Bearer $GITHUB_TOKEN", request = gistRequest)

            if (response.isSuccessful && response.body() != null) {
                val gistResponse: GistResponse = response.body()!!
                val gistId = gistResponse.id
                val finalUrl = "https://htmlpreview.github.io/?https://gist.github.com/$GITHUB_USERNAME/$gistId/raw/index.html"
                _qrCodeUrl.postValue(finalUrl)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Bilinmeyen hata."
                _qrCodeUrl.postValue("UNAVAILABLE")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _qrCodeUrl.postValue("UNAVAILABLE")
        }
    }

    fun resetQrCodeUrl() {
        _qrCodeUrl.value = null
    }

    fun resetQuickSendQrUrl() {
        _quickSendQrUrl.value = null
    }

    fun createQuickSendGistForText(text: String) = viewModelScope.launch {
        if (text.isBlank()) {
            _quickSendQrUrl.postValue(null)
            return@launch
        }
        val htmlContent = """
            <!DOCTYPE html><html><head><title>Metin</title><meta name="viewport" content="width=device-width, initial-scale=1">
            <style>body{font-family:sans-serif; background-color:#f0f2f5; display:flex; justify-content:center; align-items:center; min-height:100vh; margin:0;} pre{white-space:pre-wrap; word-wrap:break-word; background-color:white; padding:20px; border-radius:8px; box-shadow:0 2px 10px rgba(0,0,0,0.1); max-width:800px;}</style>
            </head><body><pre>${text.replace("<", "&lt;").replace(">", "&gt;")}</pre></body></html>
        """.trimIndent()
        createGistForQuickSend("Hızlı Metin Paylaşımı", "index.html", htmlContent)
    }

    fun createQuickSendGistForFile(uri: Uri, resolver: ContentResolver) = viewModelScope.launch {
        try {
            val fileName = getFileName(uri, resolver) ?: "indirilen_dosya"
            val mimeType = resolver.getType(uri) ?: "application/octet-stream"

            val fileBytes = resolver.openInputStream(uri)?.use { inputStream: InputStream ->
                inputStream.readBytes()
            }

            if (fileBytes == null) {
                _quickSendQrUrl.postValue(null)
                return@launch
            }

            val base64String = Base64.getEncoder().encodeToString(fileBytes)
            val dataUrl = "data:$mimeType;base64,$base64String"

            val htmlContent = """
            <!DOCTYPE html><html><head><title>${fileName}</title><meta name="viewport" content="width=device-width, initial-scale=1">
            <style>body{font-family:sans-serif; background-color:#f0f2f5; display:flex; justify-content:center; align-items:center; min-height:100vh; margin:0;} a{display:inline-block; padding:20px 40px; background-color:#007bff; color:white; text-decoration:none; border-radius:8px; font-size:18px; box-shadow:0 2px 10px rgba(0,0,0,0.2);}</style>
            </head><body><a href="$dataUrl" download="${URLEncoder.encode(fileName, "UTF-8")}">"${fileName}" Dosyasını İndir</a></body></html>
            """.trimIndent()

            createGistForQuickSend("Hızlı Dosya Paylaşımı: $fileName", "index.html", htmlContent)
        } catch (e: Exception) {
            e.printStackTrace()
            _quickSendQrUrl.postValue(null)
        }
    }

    private suspend fun createGistForQuickSend(description: String, fileName: String, content: String) {
        withContext(Dispatchers.IO) {
            try {
                val files = mapOf(fileName to GistFileContent(content))
                val gistRequest = GistRequest(description, true, files)
                val response = RetrofitClient.instance.createGist("Bearer $GITHUB_TOKEN", request = gistRequest)

                if (response.isSuccessful && response.body() != null) {
                    val gistResponse: GistResponse = response.body()!!
                    val gistId = gistResponse.id
                    val finalUrl = "https://htmlpreview.github.io/?https://gist.github.com/$GITHUB_USERNAME/$gistId/raw/index.html"
                    _quickSendQrUrl.postValue(finalUrl)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Bilinmeyen hata."
                    _quickSendQrUrl.postValue(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _quickSendQrUrl.postValue(null)
            }
        }
    }

    private fun getFileName(uri: Uri, resolver: ContentResolver): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = resolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if(columnIndex >= 0) result = it.getString(columnIndex)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                if (cut != null) {
                    result = result?.substring(cut + 1)
                }
            }
        }
        return result
    }
}
