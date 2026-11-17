package id.ctanggap.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Layar 7: Kontak darurat. */
@Composable
fun ContactsScreen(vm: AppViewModel, onAdd: () -> Unit) {
    val contacts by vm.contacts.collectAsState()
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        ScreenTitle("Kontak Darurat", "Akan dihubungi sesuai prioritas")
        if (contacts.isEmpty()) Text("Belum ada kontak. Tambahkan minimal satu kontak darurat.", Modifier.padding(vertical = 12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(contacts, key = { it.id }) { c ->
                WhiteCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(Cream), contentAlignment = Alignment.Center) {
                            Text("${c.priority}.", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, fontSize = 17.sp)
                            Text(c.phone, color = Red, fontSize = 14.sp)
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Sms, null, tint = Red, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp)); Text("SMS", fontSize = 12.sp, color = Red, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        IconButton(onClick = { vm.deleteContact(c) }) { Icon(Icons.Filled.Delete, "Hapus", tint = Muted) }
                    }
                }
            }
            item {
                val full = contacts.size >= AppViewModel.MAX_CONTACTS
                Box(
                    Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp))
                        .border(BorderStroke(2.dp, if (full) Color.LightGray else Color.Black), RoundedCornerShape(16.dp))
                        .clickable(enabled = !full) { onAdd() },
                    contentAlignment = Alignment.Center
                ) { Text(if (full) "Maksimal ${AppViewModel.MAX_CONTACTS} kontak" else "+ Tambahkan Kontak") }
            }
            item {
                Text("Kontak dapat membalas SMS dengan YA agar eskalasi berikutnya berhenti.", fontSize = 12.sp, color = Muted,
                    modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

/** Layar 8: Tambah kontak. */
@Composable
fun AddContactScreen(vm: AppViewModel, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var priority by remember { mutableIntStateOf(vm.contacts.value.size + 1) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        ScreenTitle("Tambah Kontak", "Maksimal ${AppViewModel.MAX_CONTACTS} kontak, dihubungi sesuai prioritas")
        WhiteCard {
            OutlinedTextField(name, { name = it }, label = { Text("Nama") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(phone, { phone = it }, label = { Text("Nomor HP") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.height(16.dp))
            Text("Prioritas", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Muted)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                (1..AppViewModel.MAX_CONTACTS).forEach { p ->
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(if (priority == p) Red else GreyChip)
                            .clickable { priority = p },
                        contentAlignment = Alignment.Center
                    ) { Text("$p", color = if (priority == p) Color.White else Color.Black, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Cream)) {
                Text("Beri tahu kontak bahwa mereka akan menerima SMS darurat dan diminta membalas YA jika menuju lokasi.",
                    Modifier.padding(14.dp), color = Red, fontSize = 12.sp)
            }
            error?.let { Text(it, color = Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Simpan Kontak") {
            if (vm.addContact(name, phone, priority)) onDone()
            else error = "Nama wajib diisi dan nomor minimal 8 digit."
        }
        Spacer(Modifier.height(10.dp))
        OutlineButton("Batal", onClick = onDone)
    }
}
