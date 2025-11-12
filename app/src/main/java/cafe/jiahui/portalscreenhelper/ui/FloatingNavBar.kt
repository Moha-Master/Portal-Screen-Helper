package cafe.jiahui.portalscreenhelper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun FloatingNavBar(
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onRecentsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .wrapContentSize()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), // More opaque background
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
                Icons.AutoMirrored.Filled.ArrowBack, 
                contentDescription = "Back",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface // Use onSurface color for proper theme support
            )
        }
        IconButton(
            onClick = onHomeClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Default.Home, 
                contentDescription = "Home",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface // Use onSurface color for proper theme support
            )
        }
        IconButton(
            onClick = onRecentsClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.List, 
                contentDescription = "Recents",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface // Use onSurface color for proper theme support
            )
        }
    }
}
