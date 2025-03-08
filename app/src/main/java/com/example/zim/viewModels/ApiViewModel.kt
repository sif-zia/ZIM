package com.example.zim.viewModels

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zim.api.Post
import com.example.zim.api.ClientRepository
import com.example.zim.api.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApiViewModel @Inject constructor(
    private val postRepository: ClientRepository,
    private val application: Application,
    private val serverRepository: ServerRepository // Inject the ServerProvider instead
) : ViewModel() {
    // State for posts
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        // Start server when ViewModel is created
        serverRepository.startServer()

        // Observe server status
        viewModelScope.launch {
            serverRepository.isServerRunning.collectLatest { isRunning ->
                if (isRunning) {
                    // Server is running, load posts
                    loadPosts()
                }
            }
        }
    }

    fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                postRepository.fetchPosts()
                _posts.value = postRepository.posts.value

                Log.d("ApiViewModel", "Posts loaded successfully: ${_posts.value.size}")
                Toast.makeText(
                    application,
                    "Loaded ${_posts.value.size} posts",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.e("ApiViewModel", "Error loading posts", e)
                _error.value = e.message ?: "Unknown error occurred"
                Toast.makeText(
                    application,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        serverRepository.stopServer()
        super.onCleared()
    }
}