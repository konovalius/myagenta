package com.example.myagent.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myagent.ui.theme.GradientBackground

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Добро пожаловать!",
                color = Color.White,
                fontSize = 32.sp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Войдите через фото-ориентир: выберите изображение, наложите его на видоискатель камеры и совместите ракурс. Затем нажмите кнопку съёмки.",
                color = Color(0xFF9AA3AF),
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Button(onClick = onContinue) {
                Text("Продолжить")
            }
        }
    }
}