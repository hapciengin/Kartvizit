package com.qrtasima.network
import com.google.gson.annotations.SerializedName

data class GistRequest(
    val description: String,
    val public: Boolean,
    val files: Map<String, GistFileContent>
)

data class GistFileContent(
    val content: String
)

data class GistResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("html_url")
    val htmlUrl: String?
)