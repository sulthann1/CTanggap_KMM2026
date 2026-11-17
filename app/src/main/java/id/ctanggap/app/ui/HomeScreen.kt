package id.ctanggap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import id.ctanggap.app.data.AppSettings
import id.ctanggap.app.sendToService
import id.ctanggap.app.service.DrivingService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Layar 3 (aktif) dan 4 (nonaktif): Beranda. */
@Composable
fun HomeScreen(vm: AppViewModel, openPermission: () -> Unit) {
    val ctx = LocalContext.current
    val settings = remember { AppSettings(ctx) }
    val running by DrivingService.running.collectAsState()
    val contacts by vm.contacts.collectAsState()
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val permsOk = remember(tick) { Permissions.allOk(ctx) }
    val scope = rememberCoroutineScope()
    var sosProgress by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 23.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.padding(top = 28.dp)) {
            Text("Hati-hati dalam berkendara", fontSize = 14.sp)
            Text("Halo, ${settings.userName.ifBlank { "User" }}", fontSize = 32.sp, fontWeight = FontWeight.Medium)
        }
        if (!permsOk) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Cream)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Izin atau pengaturan baterai belum lengkap.", Modifier.weight(1f), color = Red, fontSize = 13.sp)
                    TextButton(onClick = openPermission) { Text("Lengkapi", color = Red, fontWeight = FontWeight.Bold) }
                }
            }
        }
        WhiteCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(130.dp).clip(CircleShape).background(if (running) Color(0xFFDDF1E3) else GreyChip),
                    contentAlignment = Alignment.Center) {
                    Icon(if (running) Icons.Filled.Check else Icons.Filled.Close, null, modifier = Modifier.size(64.dp),
                        tint = if (running) Color(0xFF1B4D2B) else Muted)
                }
                Spacer(Modifier.height(14.dp))
                Text(if (running) "Pemantauan aktif" else "Pemantauan nonaktif", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (running) "Mendeteksi indikasi benturan selama mode berkendara." else "Aktifkan mode berkendara agar benturan dapat terdeteksi.",
                    fontSize = 14.sp, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))
                PrimaryButton(if (running) "Berhenti Berkendara" else "Mulai Berkendara", Modifier.padding(horizontal = 20.dp)) {
                    sendToService(ctx, if (running) DrivingService.ACTION_STOP else DrivingService.ACTION_START)
                }
            }
        }
        WhiteCard {
            InfoRow("Sensor gerak", if (running) "Aktif" else "Nonaktif")
            InfoRow("Izin lokasi", if (Permissions.locationOk(ctx)) "Lengkap" else "Belum")
            InfoRow("Kontak darurat", "${contacts.size} kontak")
            InfoRow("Info medis", settings.medicalInfo.ifBlank { "Belum diisi" })
        }
        // SOS: tahan 2 detik agar tidak tertekan tidak sengaja
        Box(
            Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp))
                .background(if (sosProgress) Color(0xFFB3261E) else Red)
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        sosProgress = true
                        val job = scope.launch { delay(2_000); sendToService(ctx, DrivingService.ACTION_SOS) }
                        tryAwaitRelease()
                        job.cancel(); sosProgress = false
                    })
                },
            contentAlignment = Alignment.Center
        ) { Text(if (sosProgress) "Tahan..." else "SOS (tahan 2 detik)", color = Cream, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
    }
}
