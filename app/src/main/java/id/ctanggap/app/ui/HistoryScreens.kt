package id.ctanggap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ctanggap.app.data.Incident
import java.util.Locale

private fun statusLabel(s: String) = when (s) {
    "TERKIRIM" -> "Terkirim"; "DITANGANI" -> "Ditangani"; "DIBATALKAN" -> "Dibatalkan"; else -> "Hitung mundur"
}
private fun statusColor(s: String) = when (s) {
    "TERKIRIM" -> InfoBlue; "DITANGANI" -> OkGreen; "DIBATALKAN" -> WarnOrange; else -> GreyChip
}

/** Layar 11: Riwayat. */
@Composable
fun HistoryScreen(vm: AppViewModel, onOpen: (Long) -> Unit) {
    val incidents by vm.incidents.collectAsState()
    val trips by vm.trips.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenTitle("Riwayat") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CountCard(trips.size, "Perjalanan", Modifier.weight(1f))
                CountCard(incidents.size, "Insiden", Modifier.weight(1f))
            }
        }
        item { Text("Perjalanan", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp)) }
        if (trips.isEmpty()) item { Text("Belum ada perjalanan.", color = Muted) }
        items(trips, key = { "t${it.id}" }) { t ->
            WhiteCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Perjalanan ${fmtDateTime(t.startMs)}")
                        Text(String.format(Locale.US, "%.1f km", t.distanceM / 1000f), fontSize = 12.sp, color = Muted)
                    }
                    if (t.hadIncident) StatusChip("Insiden", Color(0xFFF28B8B)) else StatusChip("Aman", OkGreen)
                }
            }
        }
        item { Text("Insiden", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp)) }
        if (incidents.isEmpty()) item { Text("Belum ada insiden.", color = Muted) }
        items(incidents, key = { "i${it.id}" }) { i ->
            WhiteCard(Modifier.clickable { onOpen(i.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Insiden ${fmtDateTime(i.timeMs)}")
                        Text(locText(i), fontSize = 12.sp, color = Muted)
                    }
                    StatusChip(statusLabel(i.status), statusColor(i.status))
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

private fun locText(i: Incident) =
    if (i.lat != null && i.lng != null) String.format(Locale.US, "%.4f, %.4f", i.lat, i.lng) else "Lokasi tidak tersedia"

@Composable
private fun CountCard(n: Int, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(28.dp)).background(Red).padding(vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$n", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Medium)
        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

/** Layar 12: Detail insiden (log pengiriman per tahap). */
@Composable
fun IncidentDetailScreen(vm: AppViewModel, id: Long, onBack: () -> Unit) {
    val flow = remember(id) { vm.incident(id) }
    val logsFlow = remember(id) { vm.logsFor(id) }
    val incident by flow.collectAsState(initial = null)
    val logs by logsFlow.collectAsState(initial = emptyList())

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") }
                Text("Detail Insiden", Modifier.weight(1f), fontSize = 26.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.width(48.dp))
            }
        }
        incident?.let { i ->
            item {
                WhiteCard {
                    InfoRow("Waktu", fmtFull(i.timeMs))
                    InfoRow("Pemicu", if (i.trigger == "SOS") "SOS manual" else "Otomatis (deteksi benturan)")
                    InfoRow("Lokasi", locText(i))
                    InfoRow("Status", statusLabel(i.status))
                }
            }
        }
        item { Text("Log pengiriman", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp)) }
        if (logs.isEmpty()) item { Text("Belum ada pengiriman (dibatalkan sebelum hitungan mundur selesai).", color = Muted, fontSize = 13.sp) }
        items(logs, key = { it.id }) { l ->
            WhiteCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Cream), contentAlignment = Alignment.Center) {
                        Text(if (l.stage == 0) "-" else "${l.stage}", color = Red, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${if (l.channel == "CALL") "Panggilan" else "SMS"}: ${l.recipientName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${l.recipientType} ${l.phone}", fontSize = 12.sp, color = Muted)
                        Text(fmtDateTime(l.timeMs), fontSize = 11.sp, color = Muted)
                    }
                    StatusChip(l.status.substringBefore(" ").lowercase().replaceFirstChar { it.uppercase() },
                        when (l.status.substringBefore(" ")) { "TERKIRIM" -> InfoBlue; "GAGAL" -> Color(0xFFF28B8B); else -> WarnOrange })
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
