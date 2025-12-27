package com.example.qlutestitemrepository.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

enum class DataSource {
    GITHUB,
    CLOUDFLARE_R2
}

class MainViewModel : ViewModel() {
    var isDarkTheme by mutableStateOf(false)
    var themeColor by mutableStateOf<Color?>(null)
    
    var dataSource by mutableStateOf(DataSource.GITHUB)
        private set

    // Auto Update State
    var autoUpdateInterval by mutableStateOf(0L) // 0 means disabled
        private set
    
    // Data Update Cooling State
    var isCoolingDown by mutableStateOf(false)
        private set
    var coolingProgress by mutableStateOf(0f)
        private set
    var coolingTimeLeft by mutableStateOf(0)
        private set

    private var updateJob: Job? = null
    private var coolingJob: Job? = null
    
    private val _refreshTrigger = MutableSharedFlow<Unit>()
    val refreshTrigger = _refreshTrigger.asSharedFlow()

    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
    }

    fun updateThemeColor(color: Color) {
        themeColor = color
    }
    
    fun updateDataSource(source: DataSource) {
        dataSource = source
    }

    fun updateAutoUpdateInterval(minutes: Long) {
        autoUpdateInterval = minutes
        startAutoUpdateTimer()
    }

    private fun startAutoUpdateTimer() {
        updateJob?.cancel()
        if (autoUpdateInterval > 0) {
            updateJob = viewModelScope.launch {
                while (true) {
                    delay(autoUpdateInterval * 60 * 1000)
                    _refreshTrigger.emit(Unit)
                }
            }
        }
    }

    // Cooling Logic
    fun startCoolingDown() {
        if (isCoolingDown) return
        isCoolingDown = true
        coolingProgress = 1f
        coolingTimeLeft = 60
        
        coolingJob?.cancel()
        coolingJob = viewModelScope.launch {
            val totalTime = 60
            for (i in 0 until totalTime) {
                delay(1000)
                coolingTimeLeft--
                coolingProgress = coolingTimeLeft / totalTime.toFloat()
            }
            isCoolingDown = false
            coolingProgress = 0f
        }
    }
}
