package com.example.qlutestitemrepository.data.network

import com.example.qlutestitemrepository.data.model.GitHubFileItem
import retrofit2.http.GET
import retrofit2.http.Path

interface GitHubApiService {
    @GET("repos/Torchman005/QLU-Test-Item-Files/contents/{path}")
    suspend fun getContents(@Path("path", encoded = true) path: String = ""): List<GitHubFileItem>

    @GET("repos/Torchman005/QLU-Test-Item-Files/git/trees/main?recursive=1")
    suspend fun getRecursiveTree(): GitHubTreeResponse
}

data class GitHubTreeResponse(
    val tree: List<GitHubTreeItem>,
    val truncated: Boolean
)

data class GitHubTreeItem(
    val path: String,
    val mode: String,
    val type: String, // "blob" for file, "tree" for dir
    val sha: String,
    val size: Int?,
    val url: String
)
