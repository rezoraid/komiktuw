package com.komikta.feature.profile

import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import com.komikta.ui.components.AppIcons
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
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        val u = user
        if (u == null) {
            SignedOut(ui.busy, signIn)
        } else {
            SignedIn(
                u,
                onEdit = { editing = true },
                onSignOut = viewModel::signOut,
                onDelete = { confirmDelete = true },
                onModeration = onModeration
            )
        }
        ui.message?.let { Text(it, fontSize = 12.sp, color = Ui.Text2, modifier = Modifier.padding(top = 14.dp)) }
        Spacer(Modifier.height(28.dp))
        Text(
            "Dengan masuk kamu menyetujui Ketentuan dan Kebijakan Privasi",
            fontSize = 11.sp,
            color = Ui.Text3
        )
        Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Ketentuan", fontSize = 12.sp, color = Ui.Accent, modifier = Modifier.clickable { uriHandler.openUri("${BuildConfig.BASE_URL}/terms") }.padding(vertical = 6.dp))
            Text("Kebijakan Privasi", fontSize = 12.sp, color = Ui.Accent, modifier = Modifier.clickable { uriHandler.openUri("${BuildConfig.BASE_URL}/privacy") }.padding(vertical = 6.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Komikta ${BuildConfig.VERSION_NAME}", fontSize = 12.sp, color = Ui.Text3)
        Text("Sumber: Lumos · Manta · Shinigami", fontSize = 12.sp, color = Ui.Text3, modifier = Modifier.padding(top = 2.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Ui.Card,
            title = { Text("Hapus akun?", style = MaterialTheme.typography.titleMedium) },
            text = { Text("Akun dan semua komentarmu akan dihapus permanen. XP dan level hilang. Tindakan ini tidak bisa dibatalkan.", color = Ui.Text2, fontSize = 14.sp) },
            confirmButton = { TextButton(onClick = { viewModel.deleteAccount(); confirmDelete = false }) { Text("Hapus", color = Ui.Accent) } },
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
                            cursorBrush = SolidColor(Ui.Accent),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text("3-20 karakter: huruf kecil, angka, underscore. Bisa diganti sekali per 7 hari.", fontSize = 12.sp, color = Ui.Text3, modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.changeUsername(value) { ok -> if (ok) editing = false } }, enabled = value.length >= 3 && !ui.busy) { Text("Simpan", color = Ui.Accent) }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Batal", color = Ui.Text2) } }
        )
    }
}

@Composable
private fun SignedOut(busy: Boolean, onSignIn: () -> Unit) {
    Text("Masuk untuk berkomentar, membalas, dan mengumpulkan XP.", fontSize = 14.sp, lineHeight = 21.sp, color = Ui.Text2)
    Text(
        if (busy) "Memproses…" else "Masuk dengan Google",
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = Ui.OnAccent,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Ui.AccentBrush)
            .clickable(enabled = !busy, onClick = onSignIn)
            .padding(vertical = 13.dp)
    )
}

@Composable
private fun SignedIn(u: CommunityUserDto, onEdit: () -> Unit, onSignOut: () -> Unit, onDelete: () -> Unit, onModeration: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(u.picture, u.name.ifBlank { u.username }, 72.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(u.name.ifBlank { u.username }, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("@${u.username}", fontSize = 13.sp, color = Ui.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                RoleBadge(u.role)
                LevelChip(u.level)
            }
        }
    }
    val span = (u.xpNextLevel - u.xpCurrentLevel).coerceAtLeast(1)
    val into = (u.xp - u.xpCurrentLevel).coerceIn(0, span)
    Row(Modifier.padding(top = 24.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Menuju level ${u.level + 1}", fontSize = 12.sp, color = Ui.Text2)
        Text("${u.xp} / ${u.xpNextLevel} XP", fontSize = 12.sp, color = Ui.Text2)
    }
    Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Ui.Line)) {
        Box(Modifier.fillMaxWidth(into.toFloat() / span).height(4.dp).background(Ui.Accent))
    }
    Spacer(Modifier.height(24.dp))
    MenuRow("Ganti username", onEdit)
    if (u.role == "MODERATOR" || u.role == "OWNER") MenuRow("Moderasi", onModeration)
    MenuRow("Keluar", onSignOut)
    MenuRow("Hapus akun", onDelete, Color(0xFFE08F8F), last = true)
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit, color: Color = Color.White, last: Boolean = false) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Ui.Line))
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = color, modifier = Modifier.weight(1f))
        Icon(AppIcons.chevronRight, null, tint = Ui.Text3, modifier = Modifier.size(18.dp))
    }
    if (last) Box(Modifier.fillMaxWidth().height(1.dp).background(Ui.Line))
}
