package com.komikta.feature.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.komikta.BuildConfig
import com.komikta.R

@Composable
fun ProfileScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val floatingNav by viewModel.floatingNav.collectAsState()
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(20.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0A0A0A),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_logo),
                        contentDescription = "Komikta",
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Komikta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                InfoRow("Version", BuildConfig.VERSION_NAME)
                Spacer(Modifier.height(12.dp))
                InfoRow("Source", "Lumos · Manta · Shinigami")
            }
        }

        Spacer(Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0A0A0A),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Navigasi mengambang", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(2.dp))
                    Text("Bar bawah gaya glass yang mengambang", style = MaterialTheme.typography.labelMedium, color = Color(0xFF999999))
                }
                Switch(checked = floatingNav, onCheckedChange = { viewModel.setFloatingNav(it) })
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF999999))
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
