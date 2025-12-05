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

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // 可折叠的主栏 - 添加向下轻扫隐藏功能
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(1f)
                .padding(bottom = 16.dp)
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
        
        // 左侧展开按钮
        ExpandButton(
            isVisible = isExpanded.not(),
            onClick = { isExpanded = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .zIndex(2f) // 确保按钮在最上层
                .padding(start = 16.dp, bottom = 16.dp)
        )
        
        // 右侧展开按钮
        ExpandButton(
            isVisible = isExpanded.not(),
            onClick = { isExpanded = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .zIndex(2f) // 确保按钮在最上层
                .padding(end = 16.dp, bottom = 16.dp)
        )
        
        // 隐藏状态时的拖动手柄区域
        if (isExpanded.not()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(60.dp) // 可拖动区域的高度
                    .padding(horizontal = 16.dp)
                    .background(Color.Transparent)
            )
        }
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
        
        // 收起按钮
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