package com.qrtasima.network
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GithubApiService {
    @POST("gists")
    suspend fun createGist(
        @Header("Authorization") token: String,
        @Header("Accept") accept: String = "application/vnd.github.v3+json",
        @Body request: GistRequest
    ): Response<GistResponse>
}