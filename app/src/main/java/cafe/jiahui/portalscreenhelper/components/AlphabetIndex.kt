package cafe.jiahui.portalscreenhelper.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AlphabetIndex(
    modifier: Modifier = Modifier,
    onLetterSelected: (String) -> Unit
) {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    
    Box(
        modifier = modifier
            .width(24.dp) // Reduced width
            .fillMaxHeight()
            .background(Color.Transparent)
            .padding(end = 4.dp) // Add small padding to the edge
            .pointerInput(alphabet) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val index = (offset.y / (size.height / alphabet.length)).toInt()
                        if (index >= 0 && index < alphabet.length) {
                            onLetterSelected(alphabet[index].toString())
                        }
                    },
                    onDrag = { change, _ ->
                        if (change.pressed) {
                            val index = (change.position.y / (size.height / alphabet.length)).toInt()
                            if (index >= 0 && index < alphabet.length) {
                                onLetterSelected(alphabet[index].toString())
                            }
                        }
                    }
                )
            }
    ) {
        Column {
            alphabet.forEach { letter ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable { onLetterSelected(letter.toString()) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter.toString(),
                        fontSize = 8.sp, // Smaller font
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}