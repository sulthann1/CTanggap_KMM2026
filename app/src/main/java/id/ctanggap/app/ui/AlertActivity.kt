package id.ctanggap.app.ui

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ctanggap.app.alert.IncidentState
import id.ctanggap.app.alert.Phase
import id.ctanggap.app.alert.StageState
import id.ctanggap.app.data.AppSettings
import id.ctanggap.app.sendToService
import id.ctanggap.app.service.DrivingService
import kotlinx.coroutines.delay
import kotlin.math.ceil

/**
 * Layar penuh di atas layar kunci. Layar 5 (hitungan mundur) dan 6 (status eskalasi) berada di Activity yang sama;
 * isinya mengikuti IncidentState.phase. Timer sebenarnya dipegang DrivingService.
 */
class AlertActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        else @Suppress("DEPRECATION") window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { CTanggapTheme { AlertRoot(onFinish = { finish() }) } }
    }
}

@Composable
private fun AlertRoot(onFinish: () -> Unit) {
    val phase by IncidentState.phase.collectAsState()
    LaunchedEffect(phase) { if (phase == Phase.NONE) onFinish() }
    Box(Modifier.fillMaxSize().background(Red)) {
        when (phase) {
            Phase.COUNTDOWN -> CountdownContent()
            Phase.ESCALATING, Phase.DONE -> StatusContent(done = phase == Phase.DONE, onClose = {
                IncidentState.phase.value = Phase.NONE
            })
            Phase.NONE -> {}
        }
    }
}

@Composable
private fun Pill() {
    Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFF5F6F8)).padding(horizontal = 28.dp, vertical = 12.dp)) {
        Text("PERINGATAN", color = Red, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** Layar 5: Hitungan mundur. */
@Composable
private fun CountdownContent() {
    val ctx = LocalContext.current
    val deadline by IncidentState.deadlineMs.collectAsState()
    val contact by IncidentState.firstContact.collectAsState()
    val location by IncidentState.locationText.collectAsState()
    val totalMs = remember { AppSettings(ctx).countdownSec * 1_000L }
    var remaining by remember { mutableLongStateOf(totalMs) }
    LaunchedEffect(deadline) {
        while (true) { remaining = (deadline - System.currentTimeMillis()).coerceAtLeast(0); delay(100) }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(12.dp))
        Pill()
        Spacer(Modifier.height(20.dp))
        Text("Keadaan darurat terdeteksi", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("Akan mengirim SMS ke ${contact.ifBlank { "kontak darurat" }} jika tidak dibatalkan",
            color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(24.dp))
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(230.dp)) {
                val w = 12.dp.toPx()
                drawCircle(Color.White.copy(alpha = 0.25f), style = Stroke(w))
                val frac = if (totalMs > 0) remaining.toFloat() / totalMs else 0f
                drawArc(Color.White, -90f, 360f * frac, false, style = Stroke(w, cap = StrokeCap.Round),
                    topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${ceil(remaining / 1000.0).toInt()}", color = Color.White, fontSize = 80.sp, fontWeight = FontWeight.Bold)
                Text("DETIK", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Place, null, tint = Color.White)
            Spacer(Modifier.width(6.dp))
            Text("Lokasi terakhir: $location", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        Spacer(Modifier.height(28.dp))
        Button(onClick = { sendToService(ctx, DrivingService.ACTION_SAFE) },
            modifier = Modifier.fillMaxWidth().height(72.dp), shape = RoundedCornerShape(36.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Cream, contentColor = Red)) {
            Text("Saya baik-baik saja", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(14.dp))
        OutlinedButton(onClick = { sendToService(ctx, DrivingService.ACTION_SEND_NOW) },
            modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(26.dp),
            border = androidx.compose.foundation.BorderStroke(3.dp, Cream),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
            Text("Kirim Bantuan Segera", fontWeight = FontWeight.SemiBold)
        }
        Text("Melewati sisa hitungan mundur dan langsung mengirim SMS.", color = Cream, fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
    }
}

/** Layar 6: Status eskalasi setelah hitungan mundur selesai. */
@Composable
private fun StatusContent(done: Boolean, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val stages by IncidentState.stages.collectAsState()
    val outcome by IncidentState.outcome.collectAsState()
    val location by IncidentState.locationText.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1_000) } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(12.dp))
        Pill()
        Spacer(Modifier.height(16.dp))
        Text("Peringatan terkirim", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(if (done && outcome.isNotBlank()) outcome else "Menunggu balasan YA dari kontak darurat",
            color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(20.dp))
        WhiteCard {
            stages.forEachIndexed { idx, st ->
                Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                    val col = when (st.state) {
                        StageState.DONE -> OkGreen; StageState.NOW -> WarnOrange; StageState.SKIPPED -> Color(0xFFD9D9DD); StageState.WAIT -> GreyChip
                    }
                    Box(Modifier.size(34.dp).clip(CircleShape).background(col), contentAlignment = Alignment.Center) {
                        if (st.state == StageState.DONE) Icon(Icons.Filled.Check, null, tint = Color(0xFF0B5A22), modifier = Modifier.size(20.dp))
                        else Text("${st.stage}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(st.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        val eta = st.etaAtMs
                        val detail = if (eta != null && (st.state == StageState.NOW || st.state == StageState.WAIT)) {
                            val s = ((eta - now) / 1000).coerceAtLeast(0)
                            "${st.detail} (%02d:%02d)".format(s / 60, s % 60)
                        } else st.detail
                        Text(detail, fontSize = 12.sp, color = Muted)
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Place, null, tint = Red)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Lokasi terbaru sudah dikirim", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(location, fontSize = 12.sp, color = Muted)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = { sendToService(ctx, DrivingService.ACTION_SAFE) },
            modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Cream, contentColor = Red)) {
            Text("Saya baik-baik saja", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
        Text("Menekan tombol ini mengirim SMS pembatalan ke semua penerima.", color = Cream, fontSize = 12.sp,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        if (done) TextButton(onClick = onClose) { Text("Tutup", color = Color.White, fontWeight = FontWeight.Bold) }
    }
}
