package cafe.jiahui.portalscreenhelper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.IBinder
import android.view.*
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.app.NotificationCompat
import cafe.jiahui.portalscreenhelper.ui.FoldableFloatingBar
import cafe.jiahui.portalscreenhelper.ui.FloatingNavBar
import cafe.jiahui.portalscreenhelper.ui.StandaloneExpandButton
import androidx.lifecycle.*
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import cafe.jiahui.portalscreenhelper.ui.theme.PortalScreenHelperTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var displayId: Int = Display.DEFAULT_DISPLAY

    // Manual Lifecycle & SavedState implementation
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    companion object {
        const val EXTRA_DISPLAY_ID = "extra_display_id"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "PortalScreenHelperChannel"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        displayId = intent?.getIntExtra(EXTRA_DISPLAY_ID, Display.DEFAULT_DISPLAY) ?: Display.DEFAULT_DISPLAY

        if (displayId == Display.DEFAULT_DISPLAY) {
            stopSelf()
            return START_NOT_STICKY
        }

        setupOverlay()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Portal Screen Helper Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Portal Screen Helper")
            .setContentText("Floating navigation bar is active.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    private var mainBarView: ComposeView? = null
    private var leftExpandButtonView: ComposeView? = null
    private var rightExpandButtonView: ComposeView? = null
    private var isExpandedState: Boolean = true

    private fun setupOverlay() {
        if (mainBarView != null) return

        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val targetDisplay = displayManager.getDisplay(displayId)

        if (targetDisplay == null) {
            stopSelf()
            return
        }

        val displayContext = createDisplayContext(targetDisplay)
        windowManager = displayContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // 创建主导航栏视图
        mainBarView = ComposeView(displayContext).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)

            setContent {
                PortalScreenHelperTheme {
                    // 只显示主导航栏，不含按钮
                    FloatingNavBar(
                        onBackClick = { executeRootCommand("input keyevent 4") },
                        onHomeClick = { executeRootCommand("input keyevent 3") },
                        onRecentsClick = { executeRootCommand("input keyevent 187") },
                        onCollapse = {
                            isExpandedState = false
                            updateVisibility()
                        }
                    )
                }
            }
        }

        // 创建左侧展开按钮视图
        leftExpandButtonView = ComposeView(displayContext).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)

            setContent {
                PortalScreenHelperTheme {
                    StandaloneExpandButton(
                        onClick = {
                            isExpandedState = true
                            updateVisibility()
                        }
                    )
                }
            }
        }

        // 创建右侧展开按钮视图
        rightExpandButtonView = ComposeView(displayContext).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)

            setContent {
                PortalScreenHelperTheme {
                    StandaloneExpandButton(
                        onClick = {
                            isExpandedState = true
                            updateVisibility()
                        }
                    )
                }
            }
        }

        // 添加主导航栏窗口
        val mainBarParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            x = 0
            y = 8 // 增加底部边距，防止图标被裁剪
            title = "PortalScreenMainBar"
        }

        // 添加左侧展开按钮窗口
        val leftButtonParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.BOTTOM
            x = 16 // 增加左边距，防止图标被裁剪
            y = 8 // 增加底部边距，防止图标被裁剪
            title = "PortalScreenLeftExpandButton"
        }

        // 添加右侧展开按钮窗口
        val rightButtonParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.BOTTOM
            x = 16 // 增加右边距，防止图标被裁剪
            y = 8 // 增加底部边距，防止图标被裁剪
            title = "PortalScreenRightExpandButton"
        }

        try {
            windowManager?.addView(mainBarView, mainBarParams)
            windowManager?.addView(leftExpandButtonView, leftButtonParams)
            windowManager?.addView(rightExpandButtonView, rightButtonParams)

            // 初始时隐藏不需要的组件
            updateVisibility()
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun updateVisibility() {
        if (!isExpandedState) {
            // 隐藏主导航栏，显示两个展开按钮
            mainBarView?.visibility = android.view.View.GONE
            leftExpandButtonView?.visibility = android.view.View.VISIBLE
            rightExpandButtonView?.visibility = android.view.View.VISIBLE
        } else {
            // 显示主导航栏，隐藏两个展开按钮
            mainBarView?.visibility = android.view.View.VISIBLE
            leftExpandButtonView?.visibility = android.view.View.GONE
            rightExpandButtonView?.visibility = android.view.View.GONE
        }
    }

    private fun executeRootCommand(command: String) {
        CoroutineScope(Dispatchers.IO).launch {
            RootShell.execute(command)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        if (mainBarView != null) {
            windowManager?.removeView(mainBarView)
            mainBarView = null
        }
        if (leftExpandButtonView != null) {
            windowManager?.removeView(leftExpandButtonView)
            leftExpandButtonView = null
        }
        if (rightExpandButtonView != null) {
            windowManager?.removeView(rightExpandButtonView)
            rightExpandButtonView = null
        }
        windowManager = null
    }
}