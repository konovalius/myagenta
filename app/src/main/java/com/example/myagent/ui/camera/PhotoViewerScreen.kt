package com.example.myagent.ui.camera

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun PhotoViewerScreen(
    uri: Uri,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onUsePhoto: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AsyncImage(
            model = uri,
            contentDescription = "Снимок",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        TextButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Text("Назад", color = Color.White)
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            onUsePhoto?.let { usePhoto ->
                TextButton(onClick = usePhoto) {
                    Text("Использовать", color = Color.White)
                }
            }
            TextButton(onClick = onDelete) {
                Text("Удалить", color = Color.White)
            }
        }
    }
}