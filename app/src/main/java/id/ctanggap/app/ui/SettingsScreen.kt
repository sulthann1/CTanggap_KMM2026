package id.ctanggap.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import id.ctanggap.app.data.AppSettings
import kotlin.math.roundToInt

/** Layar 9: Pengaturan. Layar 10 (dialog opt-in) muncul saat saklar eskalasi dihidupkan. */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val s = remember { AppSettings(context) }
    var name by remember { mutableStateOf(s.userName) }
    var medical by remember { mutableStateOf(s.medicalInfo) }
    var region by remember { mutableStateOf(s.regionName) }
    var test by remember { mutableStateOf(s.testNumber) }
    var optIn by remember { mutableStateOf(s.escalationOptIn) }
    var sim by remember { mutableStateOf(s.simulationMode) }
    var log by remember { mutableStateOf(s.logSensors) }
    var impact by remember { mutableFloatStateOf(s.impactG) }
    var countdown by remember { mutableFloatStateOf(s.countdownSec.toFloat()) }
    var showDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenTitle("Pengaturan")

        WhiteCard {
            Text("Profil", color = Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            OutlinedTextField(name, { name = it; s.userName = it }, label = { Text("Nama (muncul di pesan)") },
                singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            OutlinedTextField(medical, { medical = it; s.medicalInfo = it }, label = { Text("Info medis (opsional)") },
                singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            OutlinedTextField(region, { region = it; s.regionName = it }, label = { Text("Wilayah uji") },
                singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }

        WhiteCard {
            Text("Deteksi", color = Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            InfoRow("Hitungan mundur", "${countdown.roundToInt()} detik")
            Slider(countdown, { countdown = it; s.countdownSec = it.roundToInt() }, valueRange = 10f..60f,
                colors = SliderDefaults.colors(thumbColor = Red, activeTrackColor = Red))
            InfoRow("Sensitivitas benturan", "${"%.1f".format(impact)} g")
            Slider(impact, { impact = it; s.impactG = it }, valueRange = 2f..10f,
                colors = SliderDefaults.colors(thumbColor = Red, activeTrackColor = Red))
            Text("Nilai g lebih kecil = lebih sensitif (lebih banyak alarm palsu). Mulai ulang mode berkendara agar berlaku.",
                fontSize = 12.sp, color = Muted)
        }

        WhiteCard {
            Text("Eskalasi", color = Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Eskalasi ke faskes, polisi, 112", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Opsional, hanya jika kontak tidak membalas dalam 3 menit", fontSize = 12.sp, color = Muted)
                }
                Switch(optIn, { on ->
                    if (on) showDialog = true else { optIn = false; s.escalationOptIn = false }
                }, colors = SwitchDefaults.colors(checkedTrackColor = Red))
            }
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Mode simulasi", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Pesan instansi diarahkan ke nomor uji", fontSize = 12.sp, color = Muted)
                }
                Switch(sim, { sim = it; s.simulationMode = it }, colors = SwitchDefaults.colors(checkedTrackColor = Red))
            }
            if (sim) OutlinedTextField(test, { test = it; s.testNumber = it }, label = { Text("Nomor uji milik tim") },
                singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }

        WhiteCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rekam data sensor (kalibrasi)", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Switch(log, { log = it; s.logSensors = it }, colors = SwitchDefaults.colors(checkedTrackColor = Red))
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showDialog) OptInDialog(
        region = region,
        onDismiss = { showDialog = false },
        onConfirm = { optIn = true; s.escalationOptIn = true; showDialog = false }
    )
}

/** Layar 10: Dialog persetujuan eskalasi (opt-in). */
@Composable
private fun OptInDialog(region: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var agreed by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = RoundedCornerShape(50), color = Cream, modifier = Modifier.size(72.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Warning, null, tint = Red, modifier = Modifier.size(36.dp)) }
                }
                Spacer(Modifier.height(12.dp))
                Text("Aktifkan eskalasi ke instansi?", fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text("Jika kontak darurat tidak membalas YA dalam 3 menit, C-Tanggap akan:", fontSize = 14.sp)
                Text("• Mengirim SMS berisi lokasi dan info medis ke rumah sakit/puskesmas dan kantor polisi terdekat", fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp))
                Text("• Menyiapkan panggilan ke 112", fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(12.dp))
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Cream)) {
                    Text("Pesan palsu dapat membebani petugas. Fitur ini hanya untuk wilayah uji $region. " +
                        "Nomor fasilitas diambil dari data peta dan tidak dijamin akurat.",
                        Modifier.padding(12.dp), color = Red, fontSize = 12.sp)
                }
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(agreed, { agreed = it }, colors = CheckboxDefaults.colors(checkedColor = Red))
                    Text("Saya memahami risikonya dan menyetujui eskalasi", fontSize = 13.sp)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlineButton("Batal", Modifier.weight(1f), onDismiss)
                    PrimaryButton("Saya Setuju", Modifier.weight(1f), enabled = agreed, onClick = onConfirm)
                }
            }
        }
    }
}
