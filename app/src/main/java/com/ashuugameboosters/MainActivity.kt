package com.ashuugameboosters

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val bg = Color(0xFF090B14)
private val panel = Color(0xFF171C2B)
private val purple = Color(0xFF9B7BFF)
private val cyan = Color(0xFF54D6E8)
private val muted = Color(0xFF9AA4BA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BoosterHome(this) }
    }
}

@Composable
private fun BoosterHome(context: Context) {
    var profile by remember { mutableStateOf("Balanced") }
    var seconds by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    val memory = remember {
        runCatching {
            val m = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            ActivityManager.MemoryInfo().also(m::getMemoryInfo).availMem / 1048576
        }.getOrDefault(0L)
    }
    val temp = remember {
        runCatching {
            val i = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            (i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1) / 10f
        }.getOrDefault(-1f)
    }
    LaunchedEffect(running) {
        while (running) { delay(1000); seconds++ }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = purple, secondary = cyan, background = bg, surface = panel)) {
        LazyColumn(
            Modifier.fillMaxSize().background(bg),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Gamepad, null, tint = purple, modifier = Modifier.size(38.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("ASHUUUGAME", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)
                        Text("BOOSTERS • GAMING HUB", color = muted, fontSize = 10.sp, letterSpacing = 2.sp)
                    }
                }
            }
            item {
                Column {
                    Text("YOUR GAMING SPACE", color = muted, fontSize = 11.sp, letterSpacing = 2.sp)
                    Text("Ready to play?", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("Keep your sessions focused.", color = muted)
                }
            }
            item {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = panel)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, null, tint = cyan)
                            Text(" GAMING PROFILE", color = cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text(profile, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Balanced", "Performance", "Battery Saver").forEach { p ->
                                FilterChip(selected = profile == p, onClick = { profile = p }, label = { Text(p, fontSize = 10.sp) })
                            }
                        }
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = panel)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("SESSION TIMER", color = muted, fontSize = 11.sp, letterSpacing = 2.sp)
                        Text("%02d:%02d".format(seconds / 60, seconds % 60), color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                        Button(onClick = { running = !running }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (running) "Pause session" else "Start session")
                        }
                        OutlinedButton(onClick = { running = false; seconds = 0 }, modifier = Modifier.fillMaxWidth()) { Text("Reset timer") }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("AVAILABLE RAM", "$memory MB", Icons.Default.Memory, Modifier.weight(1f))
                    StatCard("BATTERY TEMP", if (temp >= 0) "%.1f°C".format(temp) else "N/A", Icons.Default.Thermostat, Modifier.weight(1f))
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF252044)), shape = RoundedCornerShape(18.dp)) {
                    Text(
                        "Note: Profiles are in-app presets. Android controls actual CPU/GPU performance and thermal protection.",
                        Modifier.padding(16.dp), color = Color.White, fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = panel)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = cyan)
            Text(title, color = muted, fontSize = 9.sp, letterSpacing = 1.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }
}
