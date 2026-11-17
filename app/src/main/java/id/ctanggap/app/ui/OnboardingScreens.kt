package id.ctanggap.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay

/** Layar 1: Splash. */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) { delay(1200); onDone() }
    Column(Modifier.fillMaxSize().background(Red), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(150.dp).clip(CircleShape).background(Cream), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Place, null, tint = Red, modifier = Modifier.size(80.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text("C-Tanggap", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold)
        Text("Cepat tanggap saat di jalan", color = Cream, fontSize = 16.sp)
    }
}

/** Layar 2: Izin dan onboarding. */
@Composable
fun PermissionScreen(onFinish: () -> Unit) {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val loc = remember(tick) { Permissions.locationOk(ctx) }
    val sms = remember(tick) { Permissions.smsOk(ctx) }
    val notif = remember(tick) { Permissions.notifOk(ctx) }
    val battery = remember(tick) { Permissions.batteryOk(ctx) }

    // Urutan: izin runtime -> lokasi latar belakang -> optimasi baterai
    val bgLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (!Permissions.batteryOk(ctx)) ctx.startActivity(Permissions.batterySettingsIntent(ctx))
    }
    val runtime = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        bgLocation.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    Column(
        Modifier.fillMaxSize().background(AppBg).verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Box(Modifier.size(84.dp).clip(CircleShape).background(Cream), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Shield, null, tint = Red, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Izinkan akses agar C-Tanggap dapat bekerja", fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        PermRow(Icons.Filled.Place, "Lokasi", "Kecepatan dan lokasi saat insiden (termasuk di latar belakang)", loc)
        PermRow(Icons.Filled.Sms, "SMS dan panggilan", "Mengirim peringatan tanpa internet dan membuka panggilan 112", sms)
        PermRow(Icons.Filled.Notifications, "Notifikasi", "Menampilkan hitungan mundur di layar kunci", notif)
        PermRow(Icons.Filled.BatteryAlert, "Optimasi baterai", "Agar pemantauan tidak dihentikan sistem", battery)
        Spacer(Modifier.height(16.dp))
        PrimaryButton(if (Permissions.allOk(ctx)) "Lanjut" else "Berikan Izin") {
            if (Permissions.allOk(ctx)) onFinish() else runtime.launch(Permissions.runtime())
        }
        TextButton(onClick = onFinish) { Text("Nanti saja", color = Red, fontWeight = FontWeight.Medium) }
        Text("Data lokasi hanya diambil saat mode berkendara aktif.", fontSize = 12.sp, color = Muted)
    }
}

@Composable
private fun PermRow(icon: ImageVector, title: String, desc: String, granted: Boolean) {
    WhiteCard(Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Cream), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Red)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(desc, fontSize = 12.sp, color = Muted)
            }
            Spacer(Modifier.width(8.dp))
            StatusChip(if (granted) "Aktif" else "Perlu izin", if (granted) OkGreen else WarnOrange)
        }
    }
}
