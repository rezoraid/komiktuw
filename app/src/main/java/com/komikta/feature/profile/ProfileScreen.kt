package com.komikta.feature.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.komikta.BuildConfig
import com.komikta.R
import com.komikta.ui.theme.Ui

@Composable
fun ProfileScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .background(Ui.Bg)
            .padding(20.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Ui.Card)
                .padding(16.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = "Komikta",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = 120.dp, height = 40.dp)
            )
            Spacer(Modifier.height(16.dp))
            InfoRow("Version", BuildConfig.VERSION_NAME)
            Spacer(Modifier.height(12.dp))
            InfoRow("Source", "Lumos · Manta · Shinigami")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Ui.Text2)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
