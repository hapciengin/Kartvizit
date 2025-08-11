package com.qrtasima.ui.profiles.detail

import androidx.lifecycle.*
import com.qrtasima.data.PackageContent
import com.qrtasima.data.PackageDao
import com.qrtasima.data.PackageWithContents
import com.qrtasima.data.ProfilePackage
import com.qrtasima.network.GistFileContent
import com.qrtasima.network.GistRequest
import com.qrtasima.network.GistResponse
import com.qrtasima.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder

class DetailViewModel(private val dao: PackageDao) : ViewModel() {

    private val GITHUB_TOKEN = "token"
    private val GITHUB_USERNAME = "kullanici_adi"

    private val _packageId = MutableLiveData<Int>()
    val packageWithContents: LiveData<PackageWithContents> = _packageId.switchMap { id ->
        dao.getPackageWithContents(id).asLiveData()
    }

    private val _qrResult = MutableLiveData<QrResult>()
    val qrResult: LiveData<QrResult> get() = _qrResult

    fun loadPackage(id: Int) {
        _packageId.value = id
    }

    fun deleteContent(content: PackageContent) = viewModelScope.launch {
        withContext(Dispatchers.IO) { dao.deleteContent(content) }
    }

    fun updatePackageAndContents(profilePackage: ProfilePackage, contents: List<PackageContent>) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            dao.updatePackage(profilePackage)
            dao.saveAllContents(profilePackage.id, contents)
        }
    }

    fun saveAndGenerateGist(profilePackage: ProfilePackage, contents: List<PackageContent>) = viewModelScope.launch {
        try {
            withContext(Dispatchers.IO) {
                dao.updatePackage(profilePackage)
                dao.saveAllContents(profilePackage.id, contents)
            }

            val packageData = withContext(Dispatchers.IO) {
                dao.getPackageWithContents(profilePackage.id).first()
            }

            val hasContent = packageData.contents.any { it.value.isNotBlank() } || packageData.profilePackage.profileImageBase64.isNullOrBlank().not()
            if (!hasContent) {
                _qrResult.postValue(QrResult.Error("QR oluşturmak için en az bir içerik veya fotoğraf eklemelisiniz."))
                return@launch
            }

            val htmlContent = generateHtml(packageData.profilePackage, packageData.contents, isForPreview = false)

            val files = mapOf(
                "index.html" to GistFileContent(htmlContent)
            )
            val gistRequest = GistRequest("QR Tasima Profile Page: ${profilePackage.name}", true, files)
            val response = RetrofitClient.instance.createGist("Bearer $GITHUB_TOKEN", request = gistRequest)

            if (response.isSuccessful && response.body() != null) {
                val gistResponse: GistResponse = response.body()!!
                val gistId = gistResponse.id
                val finalUrl = "https://htmlpreview.github.io/?https://gist.github.com/$GITHUB_USERNAME/$gistId/raw/index.html"
                _qrResult.postValue(QrResult.Success(finalUrl))
            } else {
                val errorBody = response.errorBody()?.string() ?: "Bilinmeyen hata."
                val errorMsg = "Gist oluşturulamadı: ${response.code()} ${response.message()} - $errorBody"
                _qrResult.postValue(QrResult.Error(errorMsg))
            }
        } catch (e: Exception) {
            _qrResult.postValue(QrResult.Error("Ağ hatası veya bilinmeyen bir sorun oluştu: ${e.message}"))
        }
    }

    private fun getButtonStyle(content: PackageContent): String {
        val color = content.customColor ?: when(content.type) {
            "WHATSAPP" -> "#25D366"
            "TELEFON" -> "#343A40"
            "E-POSTA" -> "#6c757d"
            "INSTAGRAM" -> "#E1306C"
            "FACEBOOK" -> "#1877F2"
            "LINKEDIN" -> "#0A66C2"
            "KONUM" -> "#DC3545"
            else -> "#007BFF"
        }
        return "style=\"background-color: $color;\""
    }

    fun generateHtml(profilePackage: ProfilePackage, contents: List<PackageContent>, isForPreview: Boolean): String {
        val nameContent = contents.firstOrNull { it.type.equals("İSİM", ignoreCase = true) }
        val name = nameContent?.value?.takeIf { it.isNotBlank() } ?: ""
        val nameColor = nameContent?.customColor

        val imageTag = profilePackage.profileImageBase64?.let { "<img src='data:image/jpeg;base64,$it' alt='Profile Picture' class='profile-pic'/>" } ?: ""

        var bodyContent = "$imageTag"
        if (name.isNotBlank()) {
            val nameStyle = nameColor?.let { "style=\"color: $it;\"" } ?: ""
            bodyContent += "<h1 $nameStyle>$name</h1>"
        }

        contents.forEach { content ->
            val value = content.value
            val label = content.label
            val type = content.type

            if (value.isNotBlank() && !type.equals("İSİM", ignoreCase = true)) {
                var link = ""
                var isTextOnly = false

                when (type) {
                    "WHATSAPP" -> link = "https://wa.me/${value.filter { it.isDigit() }}"
                    "TELEFON" -> link = "tel:$value"
                    "E-POSTA" -> link = "mailto:$value"
                    "INSTAGRAM" -> link = "https://www.instagram.com/$value"
                    "FACEBOOK" -> link = "https://www.facebook.com/$value"
                    "LINKEDIN" -> link = "https://www.linkedin.com/in/$value"
                    "KONUM" -> link = "https://www.google.com/maps/search/?api=1&query=${URLEncoder.encode(value, "UTF-8")}"
                    "CUSTOM_TEXT" -> isTextOnly = true
                }

                if (isTextOnly) {
                    val customTextStyle = content.customColor?.let { "style=\"color: $it;\"" } ?: ""
                    bodyContent += "<p class='custom-text' $customTextStyle><strong>$label:</strong> ${value.replace("\n", "<br/>")}</p>"
                } else {
                    val style = getButtonStyle(content)
                    if (isForPreview) {
                        // Önizleme modunda linkleri tıklanamaz div'lere dönüştür
                        bodyContent += "<div class='button' $style>$label</div>"
                    } else {
                        // Canlı modda link olarak kalsın
                        bodyContent += "<a href='$link' class='button' $style>$label</a>"
                    }
                }
            }
        }

        val bodyTagStyle = profilePackage.backgroundColor?.let { "style=\"background-color: $it;\"" } ?: "style=\"background-color:#f0f2f5;\""

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>${name.ifBlank { "Profil" }}</title>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
                body {
                    font-family: sans-serif;
                    margin: 0;
                    display: flex;
                    flex-direction: column;
                    justify-content: center;
                    align-items: center;
                    min-height: 100vh;
                    padding: 20px;
                    box-sizing: border-box;
                    background-color: #f0f2f5;
                }
                .card {
                    background-color: rgba(255, 255, 255, 0.85);
                    backdrop-filter: blur(5px);
                    -webkit-backdrop-filter: blur(5px);
                    border-radius: 16px;
                    padding: 24px;
                    text-align: center;
                    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
                    width: 100%;
                    max-width: 400px;
                }
                .profile-pic {
                    width: 100px;
                    height: 100px;
                    border-radius: 50%;
                    object-fit: cover;
                    margin-bottom: 16px;
                    border: 3px solid #007bff;
                }
                h1 {
                    margin-top: 0;
                    color: #333;
                }
                .button { /* Class for both <a> and <div> elements used as buttons */
                    display: block;
                    color: white !important;
                    padding: 15px;
                    margin: 12px 0;
                    border-radius: 8px;
                    text-decoration: none;
                    font-weight: bold;
                    font-size: 16px;
                    transition: background-color 0.3s ease;
                    -webkit-tap-highlight-color: transparent;
                    outline: none;
                    cursor: pointer; /* Indicate it's clickable (for <a>) but for <div> acts as visual clue */
                }
                a.button:hover {
                    opacity: 0.9;
                }
                .custom-text {
                    text-align: left;
                    color: #555;
                    margin-bottom: 12px;
                }
                .custom-text strong {
                    color: #333;
                }
            </style>
        </head>
        <body $bodyTagStyle>
            <div class="card">
                $bodyContent
            </div>
        </body>
        </html>
        """.trimIndent()
    }
}
