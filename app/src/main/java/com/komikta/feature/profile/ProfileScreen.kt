package com.komikta.feature.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.komikta.BuildConfig
import com.komikta.R
import com.komikta.data.remote.dto.CommunityUserDto
import com.komikta.feature.auth.rememberGoogleSignIn
import com.komikta.ui.components.Avatar
import com.komikta.ui.components.Chip
import com.komikta.ui.components.LevelChip
import com.komikta.ui.components.RoleBadge
import com.komikta.ui.theme.Ui

@Composable
fun ProfileScreen(onModeration: () -> Unit = {}, viewModel: ProfileViewModel = hiltViewModel()) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val signIn = rememberGoogleSignIn(onToken = viewModel::signIn, onError = viewModel::message)
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Column(
        Modifier
            .fillMaxSize()
            .background(Ui.Bg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Ui.Card).padding(16.dp)) {
            val u = user
            if (u == null) SignedOut(ui.busy, signIn) else SignedIn(u, onEdit = { editing = true }, onSignOut = viewModel::signOut, onDelete = { confirmDelete = true }, onModeration = onModeration)
            Text(
                "Dengan masuk kamu menyetujui Ketentuan dan Kebijakan Privasi",
                fontSize = 11.sp,
                color = Ui.Text3,
                modifier = Modifier.padding(top = 12.dp)
            )
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Ketentuan", fontSize = 12.sp, color = Ui.Text2, modifier = Modifier.clickable { uriHandler.openUri("${BuildConfig.BASE_URL}/terms") }.padding(vertical = 4.dp))
                Text("Kebijakan Privasi", fontSize = 12.sp, color = Ui.Text2, modifier = Modifier.clickable { uriHandler.openUri("${BuildConfig.BASE_URL}/privacy") }.padding(vertical = 4.dp))
            }
            ui.message?.let { Text(it, fontSize = 12.sp, color = Ui.Text2, modifier = Modifier.padding(top = 12.dp)) }
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Ui.Card).padding(16.dp)) {
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

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Ui.Card,
            title = { Text("Hapus akun?", style = MaterialTheme.typography.titleMedium) },
            text = { Text("Akun dan semua komentarmu akan dihapus permanen. XP dan level hilang. Tindakan ini tidak bisa dibatalkan.", color = Ui.Text2, fontSize = 14.sp) },
            confirmButton = { TextButton(onClick = { viewModel.deleteAccount(); confirmDelete = false }) { Text("Hapus", color = Color.White) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Batal", color = Ui.Text2) } }
        )
    }

    if (editing) {
        var value by remember { mutableStateOf(user?.username.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editing = false },
            containerColor = Ui.Card,
            title = { Text("Ganti username", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Ui.Chip).padding(12.dp)) {
                        BasicTextField(
                            value = value,
                            onValueChange = { value = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' }.take(20) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge,
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text("3-20 karakter: huruf kecil, angka, underscore. Bisa diganti sekali per 7 hari.", fontSize = 12.sp, color = Ui.Text3, modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.changeUsername(value) { ok -> if (ok) editing = false } }, enabled = value.length >= 3 && !ui.busy) { Text("Simpan", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Batal", color = Ui.Text2) } }
        )
    }
}

@Composable
private fun SignedOut(busy: Boolean, onSignIn: () -> Unit) {
    Text("Masuk untuk berkomentar, membalas, dan mengumpulkan XP.", fontSize = 14.sp, color = Ui.Text2)
    Text(
        if (busy) "Memproses…" else "Masuk dengan Google",
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color.Black,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(top = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .clickable(enabled = !busy, onClick = onSignIn)
            .padding(vertical = 12.dp)
    )
}

@Composable
private fun SignedIn(u: CommunityUserDto, onEdit: () -> Unit, onSignOut: () -> Unit, onDelete: () -> Unit, onModeration: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(u.picture, u.name.ifBlank { u.username }, 56.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(u.name.ifBlank { u.username }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("@${u.username}", fontSize = 13.sp, color = Ui.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                RoleBadge(u.role)
                LevelChip(u.level)
            }
        }
    }
    val span = (u.xpNextLevel - u.xpCurrentLevel).coerceAtLeast(1)
    val into = (u.xp - u.xpCurrentLevel).coerceIn(0, span)
    Row(Modifier.padding(top = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("XP", fontSize = 12.sp, color = Ui.Text2)
        Text("${u.xp} / ${u.xpNextLevel}", fontSize = 12.sp, color = Ui.Text2)
    }
    Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Ui.Chip)) {
        Box(Modifier.fillMaxWidth(into.toFloat() / span).height(6.dp).background(Color.White))
    }
    Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("Ganti username", selected = false, onClick = onEdit)
        Chip("Keluar", selected = false, onClick = onSignOut)
    }
    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (u.role == "MODERATOR" || u.role == "OWNER") Chip("Moderasi", selected = false, onClick = onModeration)
        Chip("Hapus akun", selected = false, onClick = onDelete)
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
