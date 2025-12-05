package cafe.jiahui.portalscreenhelper.ui

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FoldableFloatingBar(
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onRecentsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    // 用于检测向下轻扫手势的逻辑
    var dragOffset by remember { mutableStateOf(0f) }

    // 使用最基本的容器，只占用实际内容所需的空间
    // 不再使用试图填充全屏的布局
    Box(modifier = modifier) { // 一个基本容器，但不强制尺寸
        // 展开状态：显示主导航栏（底部居中）
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp) // 减少底部边距
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            // 当拖动结束时，如果向下滑动超过阈值，则隐藏
                            if (dragOffset > 50) {
                                isExpanded = false
                            }
                            dragOffset = 0f
                        },
                        onDrag = { change, dragAmount ->
                            dragOffset += dragAmount.y
                            // 可以根据拖动距离添加一些视觉反馈
                        }
                    )
                }
        ) {
            FloatingNavBar(
                onBackClick = onBackClick,
                onHomeClick = onHomeClick,
                onRecentsClick = onRecentsClick,
                onCollapse = { isExpanded = false }
            )
        }

        // 两个展开按钮始终显示，但通过可见性控制是否可见
        // 左侧展开按钮（始终在屏幕左下角）
        ExpandButton(
            isVisible = !isExpanded,
            onClick = { isExpanded = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 8.dp) // 减少底部边距
        )

        // 右侧展开按钮（始终在屏幕右下角）
        ExpandButton(
            isVisible = !isExpanded,
            onClick = { isExpanded = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 8.dp) // 减少底部边距
        )
    }
}

@Composable
fun ExpandButton(
    isVisible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        // 使用标准的 FloatingActionButton 来确保正确的触摸处理
        FloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier
                .size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = "Expand",
                modifier = Modifier
                    .size(24.dp)
            )
        }
    }
}

// 为单独的展开按钮创建一个独立的Composable
@Composable
fun StandaloneExpandButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 使用标准的 FloatingActionButton 来确保正确的触摸处理
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape,
        modifier = modifier
            .size(40.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowUp,
            contentDescription = "Expand",
            modifier = Modifier
                .size(24.dp)
        )
    }
}

// 更新的浮动导航栏，包含折叠按钮
@Composable
fun FloatingNavBar(
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onRecentsClick: () -> Unit,
    onCollapse: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = RoundedCornerShape(50.dp) // Capsule shape
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(
            onClick = onHomeClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(
            onClick = onRecentsClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Recents",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // 收起按钮 - 这个按钮现在由独立的窗口处理
        // 在这个版本中保留，但实际应用中可能不需要显示
        IconButton(
            onClick = onCollapse,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Collapse",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}