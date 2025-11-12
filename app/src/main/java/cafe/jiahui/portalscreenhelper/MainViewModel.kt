package cafe.jiahui.portalscreenhelper

import android.app.Application
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _externalDisplayId = MutableStateFlow<String?>("Detecting...")
    val externalDisplayId: StateFlow<String?> = _externalDisplayId.asStateFlow()

    private val _isDisplayConnected = MutableStateFlow(false)
    val isDisplayConnected: StateFlow<Boolean> = _isDisplayConnected.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var previousConnectionState: Pair<Boolean, String?> = false to null

    init {
        // Create a ticker flow that emits every 5 seconds
        val ticker = flow {
            while (true) {
                emit(Unit)
                delay(5000)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            ticker.collect {
                detectExternalDisplay()
            }
        }
    }

    fun detectExternalDisplay() {
        viewModelScope.launch(Dispatchers.IO) {
            val command = "dumpsys display | grep -oE 'mDisplayId=[0-9]+' | cut -d'=' -f2 | grep -v '^0$' | head -n 1"
            val result = RootShell.execute(command)

            val newIsConnected = !result.isNullOrBlank()
            val newDisplayId = if (newIsConnected) result else null

            // Only update if the connection state or display ID has actually changed
            if (previousConnectionState.first != newIsConnected || 
                (newIsConnected && previousConnectionState.second != newDisplayId)) {
                
                _externalDisplayId.value = newDisplayId
                _isDisplayConnected.value = newIsConnected

                // Update previous state
                previousConnectionState = newIsConnected to newDisplayId

                if (newIsConnected) {
                    try {
                        val id = newDisplayId!!.toInt()
                        startOverlayService(getApplication(), id)
                    } catch (e: NumberFormatException) {
                        // Handle cases where the result is not a valid integer
                        _externalDisplayId.value = "Error: Invalid ID"
                        _isDisplayConnected.value = false
                        stopOverlayService(getApplication())
                        previousConnectionState = false to null
                    }
                } else {
                    stopOverlayService(getApplication())
                }
            }
        }
    }

    private fun startOverlayService(context: Context, displayId: Int) {
        val intent = Intent(context, OverlayService::class.java).apply {
            putExtra(OverlayService.EXTRA_DISPLAY_ID, displayId)
        }
        context.startForegroundService(intent) // Use startForegroundService for Android O+
    }

    private fun stopOverlayService(context: Context) {
        val intent = Intent(context, OverlayService::class.java)
        context.stopService(intent)
    }

    fun launchApp(appInfo: AppInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!_isDisplayConnected.value) {
                withContext(Dispatchers.Main) {
                    _toastMessage.value = "External display not connected."
                }
                return@launch
            }
            val displayId = _externalDisplayId.value

            // 1. Resolve component name
            val resolveCommand = "cmd package resolve-activity --brief ${appInfo.packageName} | grep '/' | xargs"
            val componentName = RootShell.execute(resolveCommand)

            if (componentName.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    _toastMessage.value = "Could not resolve activity for ${appInfo.appName}"
                }
                return@launch
            }

            // 2. Start activity on the external display
            val startCommand = "am start --display $displayId $componentName"
            RootShell.execute(startCommand)

            withContext(Dispatchers.Main) {
                _toastMessage.value = "Launching ${appInfo.appName} on display $displayId"
            }
        }
    }

    fun onToastShown() {
        _toastMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopOverlayService(getApplication()) // Ensure service is stopped when ViewModel is cleared
        RootShell.close()
    }
}


