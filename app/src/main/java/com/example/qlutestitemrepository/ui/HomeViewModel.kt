package com.example.qlutestitemrepository.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qlutestitemrepository.data.model.GitHubFileItem
import com.example.qlutestitemrepository.data.network.RetrofitClient
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    var fileItems by mutableStateOf<List<GitHubFileItem>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    // Track current path stack for navigation
    private val pathStack = ArrayDeque<String>()
    var currentPath by mutableStateOf("")
        private set
    
    // Global file list for search
    private var allFiles: List<GitHubFileItem>? = null

    init {
        loadContents("")
    }

    fun loadContents(path: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                fileItems = RetrofitClient.instance.getContents(path)
                currentPath = path
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    fun search(query: String) {
        if (query.isEmpty()) {
            loadContents(currentPath)
            return
        }
        
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                // Lazy load all files if not already loaded
                if (allFiles == null) {
                    val treeResponse = RetrofitClient.instance.getRecursiveTree()
                    allFiles = treeResponse.tree.map { treeItem ->
                        // Map tree item to GitHubFileItem
                        // Note: Tree items don't have download_url directly usually, need to construct it or assume structure
                        // For "blob", we can construct raw.githubusercontent.com url
                        // https://raw.githubusercontent.com/Torchman005/QLU-Test-Item-Files/main/{path}
                        GitHubFileItem(
                            name = treeItem.path.substringAfterLast('/'),
                            path = treeItem.path,
                            sha = treeItem.sha,
                            size = treeItem.size ?: 0,
                            url = treeItem.url,
                            htmlUrl = "", // Not needed for search list display usually
                            gitUrl = "",
                            downloadUrl = if (treeItem.type == "blob") "https://raw.githubusercontent.com/Torchman005/QLU-Test-Item-Files/main/${treeItem.path}" else null,
                            type = if (treeItem.type == "tree") "dir" else "file",
                            links = com.example.qlutestitemrepository.data.model.Links("", "", "")
                        )
                    }
                }
                
                fileItems = allFiles!!.filter { 
                    it.name.contains(query, ignoreCase = true) 
                }
            } catch (e: Exception) {
                errorMessage = "Search Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    fun navigateTo(item: GitHubFileItem) {
        if (item.type == "dir") {
            pathStack.addLast(currentPath)
            loadContents(item.path)
        } else {
            // Handle file click (download or open)
            // For now, maybe just log or show toast (not implemented here)
        }
    }

    fun navigateBack(): Boolean {
        if (pathStack.isNotEmpty()) {
            val previousPath = pathStack.removeLast()
            loadContents(previousPath)
            return true
        }
        return false
    }
    
    fun refresh() {
        loadContents(currentPath)
    }
}
