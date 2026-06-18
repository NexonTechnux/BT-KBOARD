package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var btHidManager: BluetoothHidManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        btHidManager = BluetoothHidManager(applicationContext)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF07080D) // Sfondo Cyberpunk OLED profondissimo
                ) {
                    KeyboardApp(btHidManager)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Scollega e deregistra in modo pulito
        btHidManager.disconnectDevice()
        btHidManager.unregisterHidApp()
    }
}

@SuppressLint("MissingPermission")
@Composable
fun KeyboardApp(btManager: BluetoothHidManager) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Stato permessi
    var hasPermissions by remember {
        mutableStateOf(checkBluetoothPermissions(context))
    }

    // Launcher per i permessi Bluetooth
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.all { it.value }
        hasPermissions = allGranted
        if (allGranted) {
            btManager.initBluetooth()
            Toast.makeText(context, "Permessi concessi correttamente!", Toast.LENGTH_SHORT).show()
        } else {
            btManager.addLog("Permessi negati. Impossibile avviare il Bluetooth HID.")
            Toast.makeText(context, "Permessi Bluetooth richiesti per il funzionamento.", Toast.LENGTH_LONG).show()
        }
    }

    // Stati del manager
    val isEnabled by btManager.isBluetoothEnabled.collectAsState()
    val isHidSupported by btManager.isHidSupported.collectAsState()
    val isRegistered by btManager.isRegistered.collectAsState()
    val connectionState by btManager.connectionState.collectAsState()
    val connectedDevice by btManager.connectedDevice.collectAsState()
    val pairedDevices by btManager.pairedDevices.collectAsState()
    val logMessages by btManager.logFlow.collectAsState()

    // Stati della tastiera virtuale
    var shiftActive by remember { mutableStateOf(false) }
    var ctrlActive by remember { mutableStateOf(false) }
    var altActive by remember { mutableStateOf(false) }
    var winActive by remember { mutableStateOf(false) }
    var capsLockActive by remember { mutableStateOf(false) }

    // Elenco tasti attualmente premuti per gestire invii multipli simultanei
    val currentlyPressedKeys = remember { mutableStateListOf<Byte>() }

    // Interfaccia a schermo intero vs split console
    var fullscreenMode by remember { mutableStateOf(false) }

    // Chiamata periodica per rinfrescare lo stato o auto-collegarsi
    LaunchedEffect(hasPermissions) {
        if (hasPermissions) {
            btManager.initBluetooth()
        }
    }

    if (!hasPermissions) {
        // Schermata di configurazione permessi
        PermissionBlockScreen(
            permissions = permissionsToRequest,
            onRequest = {
                permissionLauncher.launch(permissionsToRequest)
            }
        )
    } else {
        // Flusso principale dell'applicazione
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Se NON è a schermo intero, mostra il pannello impostazioni e log superiore
            AnimatedVisibility(
                visible = !fullscreenMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                BluetoothControlDashboard(
                    isBluetoothEnabled = isEnabled,
                    isHidSupported = isHidSupported,
                    isRegistered = isRegistered,
                    connectionState = connectionState,
                    connectedDevice = connectedDevice,
                    pairedDevices = pairedDevices,
                    logMessages = logMessages,
                    onRefresh = { btManager.initBluetooth() },
                    onToggleRegister = {
                        if (isRegistered) btManager.unregisterHidApp() else btManager.registerHidApp()
                    },
                    onConnectDevice = { device -> btManager.connectToDevice(device) },
                    onDisconnect = { btManager.disconnectDevice() },
                    onMakeDiscoverable = {
                        try {
                            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            btManager.addLog("Richiesto stato visibile per 120s al sistema...")
                        } catch (e: Exception) {
                            btManager.addLog("Impossibile attivare discoverable: ${e.message}")
                            Toast.makeText(context, "Abilita la visibilità nelle impostazioni Bluetooth", Toast.LENGTH_LONG).show()
                        }
                    },
                    onOpenBluetoothSettings = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Impossibile aprire impostazioni BT", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // Barra di controllo rapido (sempre presente per poter uscire dal fullscreen!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C0E17))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Info stato connessione rapido
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusColor = when (connectionState) {
                        BluetoothProfile.STATE_CONNECTED -> Color(0xFF00FF66) // Verde neon
                        BluetoothProfile.STATE_CONNECTING -> Color(0xFFFFCC00) // Ambra
                        else -> Color(0xFFFF3B30) // Rosso neon
                    }
                    val statusText = when (connectionState) {
                        BluetoothProfile.STATE_CONNECTED -> "Connesso: ${connectedDevice?.name ?: "PC"}"
                        BluetoothProfile.STATE_CONNECTING -> "Connessione in corso..."
                        else -> "Scollegato"
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(statusColor, RoundedCornerShape(50))
                    )
                    Text(
                        text = statusText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                // Bottone di toggle Schermo Intero
                Button(
                    onClick = { fullscreenMode = !fullscreenMode },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (fullscreenMode) Color(0xFF222435) else Color(0xFF6200EE)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (fullscreenMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "Schermo intero",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (fullscreenMode) "Riduci" else "Schermo Intero",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Area della Tastiera virtuale
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF050609)) // Nero OLED profondo
                    .padding(4.dp)
            ) {
                KeyboardFullLayout(
                    shiftToggled = shiftActive,
                    ctrlToggled = ctrlActive,
                    altToggled = altActive,
                    winToggled = winActive,
                    capsToggled = capsLockActive,
                    onShiftClick = {
                        shiftActive = !shiftActive
                        btManager.sendKeys(
                            buildModifierByte(ctrlActive, shiftActive, altActive, winActive),
                            currentlyPressedKeys
                        )
                    },
                    onCtrlClick = {
                        ctrlActive = !ctrlActive
                        btManager.sendKeys(
                            buildModifierByte(ctrlActive, shiftActive, altActive, winActive),
                            currentlyPressedKeys
                        )
                    },
                    onAltClick = {
                        altActive = !altActive
                        btManager.sendKeys(
                            buildModifierByte(ctrlActive, shiftActive, altActive, winActive),
                            currentlyPressedKeys
                        )
                    },
                    onWinClick = {
                        winActive = !winActive
                        btManager.sendKeys(
                            buildModifierByte(ctrlActive, shiftActive, altActive, winActive),
                            currentlyPressedKeys
                        )
                    },
                    onCapsClick = {
                        capsLockActive = !capsLockActive
                        // Caps Lock è un tasto standard, mandiamo l'impulso
                        coroutineScope.launch {
                            btManager.sendKeys(
                                buildModifierByte(ctrlActive, shiftActive, altActive, winActive),
                                listOf(HidKeys.KEY_CAPS_LOCK)
                            )
                            delay(50)
                            btManager.sendKeys(
                                buildModifierByte(ctrlActive, shiftActive, altActive, winActive),
                                emptyList()
                            )
                        }
                    },
                    onKeyPress = { code ->
                        if (code != 0.toByte()) {
                            if (!currentlyPressedKeys.contains(code)) {
                                currentlyPressedKeys.add(code)
                            }
                            btManager.sendKeys(
                                buildModifierByte(ctrlActive, shiftActive || capsLockActive, altActive, winActive),
                                currentlyPressedKeys
                            )
                        }
                    },
                    onKeyRelease = { code ->
                        if (code != 0.toByte()) {
                            currentlyPressedKeys.remove(code)
                            btManager.sendKeys(
                                buildModifierByte(ctrlActive, shiftActive || capsLockActive, altActive, winActive),
                                currentlyPressedKeys
                            )
                            // Auto-rilascio Shift classico se non siamo in CapsLock
                            if (shiftActive) {
                                shiftActive = false
                                btManager.sendKeys(
                                    buildModifierByte(ctrlActive, false, altActive, winActive),
                                    currentlyPressedKeys
                                )
                            }
                        }
                    },
                    onClearModifiers = {
                        shiftActive = false
                        ctrlActive = false
                        altActive = false
                        winActive = false
                        currentlyPressedKeys.clear()
                        btManager.sendKeys(0, emptyList())
                    }
                )
            }
        }
    }
}

// Calcolo bitmask modificatori
fun buildModifierByte(ctrl: Boolean, shift: Boolean, alt: Boolean, win: Boolean): Byte {
    var mask = 0
    if (ctrl) mask = mask or HidModifiers.MODIFIER_LEFT_CTRL.toInt()
    if (shift) mask = mask or HidModifiers.MODIFIER_LEFT_SHIFT.toInt()
    if (alt) mask = mask or HidModifiers.MODIFIER_LEFT_ALT.toInt()
    if (win) mask = mask or HidModifiers.MODIFIER_LEFT_GUI.toInt()
    return mask.toByte()
}

// Schermata di blocco per richiesta permessi
@Composable
fun PermissionBlockScreen(permissions: Array<String>, onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07080D))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color(0xFF1E1F30), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = "Bluetooth",
                tint = Color(0xFF00FF66),
                modifier = Modifier.size(44.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Tastiera Bluetooth Virtuale",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Per poter associare il tuo dispositivo Android come una tastiera per il tuo PC, l'applicazione necessita dei permessi per scansionare e connettersi ad altri dispositivi Bluetooth.",
            color = Color(0xFFA0A5C0),
            fontSize = 14.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E1FF)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(50.dp)
        ) {
            Text(
                text = "Consenti Bluetooth",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

// Dashboard impostazioni e log
@SuppressLint("MissingPermission")
@Composable
fun BluetoothControlDashboard(
    isBluetoothEnabled: Boolean,
    isHidSupported: Boolean,
    isRegistered: Boolean,
    connectionState: Int,
    connectedDevice: BluetoothDevice?,
    pairedDevices: List<BluetoothDevice>,
    logMessages: List<String>,
    onRefresh: () -> Unit,
    onToggleRegister: () -> Unit,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onDisconnect: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    onOpenBluetoothSettings: () -> Unit
) {
    val logListState = rememberLazyListState()
    
    // Autoscroll logs ad ogni messaggio
    LaunchedEffect(logMessages.size) {
        if (logMessages.isNotEmpty()) {
            logListState.animateScrollToItem(logMessages.size - 1)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Color(0xFF090A10))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Pannello sinistro: Controllo e Dispositivi Associati
        Card(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131520)),
            border = BorderStroke(1.dp, Color(0xFF1D2136))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Text(
                    text = "DISPOSITIVI ASSOCIATI",
                    color = Color(0xFF00E1FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                if (pairedDevices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF191A26), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nessun dispositivo associato.\nAssocia prima il telefono dal PC.",
                            color = Color(0xFF7A809E),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF191A26), RoundedCornerShape(6.dp))
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(pairedDevices) { device ->
                            val isThisConnected = connectedDevice?.address == device.address && connectionState == BluetoothProfile.STATE_CONNECTED
                            val isConnecting = connectedDevice?.address == device.address && connectionState == BluetoothProfile.STATE_CONNECTING
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isThisConnected) Color(0xFF102E23)
                                        else if (isConnecting) Color(0xFF2C2210)
                                        else Color(0xFF222435)
                                    )
                                    .clickable {
                                        if (isThisConnected) {
                                            onDisconnect()
                                        } else {
                                            onConnectDevice(device)
                                        }
                                    }
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = device.name ?: "Dispositivo Sconosciuto",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = device.address,
                                        color = Color(0xFF7A809E),
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (isThisConnected) Color(0xFF00FF66)
                                            else if (isConnecting) Color(0xFFFFB300)
                                            else Color(0xFF00C3FF)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isThisConnected) "Disconnetti" else if (isConnecting) "Connessione..." else "Connetti",
                                        color = Color.Black,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pulsanti rapidi sotto i dispositivi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onMakeDiscoverable,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1F30)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                    ) {
                        Text("Rendi visibile", color = Color(0xFF00E1FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onOpenBluetoothSettings,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1F30)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                    ) {
                        Text("Impostazioni BT", color = Color.White, fontSize = 10.sp)
                    }
                }
            }
        }

        // Pannello destro: Stato Generale & Console Logs term
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131520)),
            border = BorderStroke(1.dp, Color(0xFF1D2136))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Info Stati principali
                Text(
                    text = "RETE & STATO HID",
                    color = Color(0xFFD000FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bluetooth Attivo:", color = Color(0xFFA0A5C0), fontSize = 11.sp)
                    Text(
                        text = if (isBluetoothEnabled) "SÌ" else "NO",
                        color = if (isBluetoothEnabled) Color(0xFF00FF66) else Color(0xFFFF3B30),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Compatibilità HID:", color = Color(0xFFA0A5C0), fontSize = 11.sp)
                    Text(
                        text = if (isHidSupported) "COMPATIBILE" else "NON SUPP.",
                        color = if (isHidSupported) Color(0xFF00FF66) else Color(0xFFFF3B30),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Profilo Tastiera:", color = Color(0xFFA0A5C0), fontSize = 11.sp)
                    Text(
                        text = if (isRegistered) "ATTIVO" else "SPENTO",
                        color = if (isRegistered) Color(0xFF00FF66) else Color(0xFFFF3B30),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable { onToggleRegister() }
                    )
                }

                Divider(color = Color(0xFF1D2136), thickness = 1.dp)
                
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "LOG ATTIVITÀ:",
                    color = Color(0xFF7A809E),
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Finestra di log scollabile tipo terminale
                LazyColumn(
                    state = logListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF07080D), RoundedCornerShape(4.dp))
                        .padding(6.dp)
                ) {
                    items(logMessages) { log ->
                        Text(
                            text = "> $log",
                            color = Color(0xFF00FF66), // Verde retro-terminale
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// Layout completo della tastiera
@Composable
fun KeyboardFullLayout(
    shiftToggled: Boolean,
    ctrlToggled: Boolean,
    altToggled: Boolean,
    winToggled: Boolean,
    capsToggled: Boolean,
    onShiftClick: () -> Unit,
    onCtrlClick: () -> Unit,
    onAltClick: () -> Unit,
    onWinClick: () -> Unit,
    onCapsClick: () -> Unit,
    onKeyPress: (Byte) -> Unit,
    onKeyRelease: (Byte) -> Unit,
    onClearModifiers: () -> Unit
) {
    val keyRows = remember { getKeyboardRows() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0D16), RoundedCornerShape(12.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keyRows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { keyItem ->
                    val isStickyActive = when {
                        keyItem.modifierBit == HidModifiers.MODIFIER_LEFT_SHIFT || keyItem.modifierBit == HidModifiers.MODIFIER_RIGHT_SHIFT -> shiftToggled
                        keyItem.modifierBit == HidModifiers.MODIFIER_LEFT_CTRL -> ctrlToggled
                        keyItem.modifierBit == HidModifiers.MODIFIER_LEFT_ALT -> altToggled
                        keyItem.modifierBit == HidModifiers.MODIFIER_LEFT_GUI -> winToggled
                        keyItem.code == HidKeys.KEY_CAPS_LOCK -> capsToggled
                        else -> false
                    }

                    KeyboardKeyCap(
                        keyItem = keyItem,
                        shiftActive = shiftToggled || capsToggled,
                        isStickyActive = isStickyActive,
                        modifier = Modifier.weight(keyItem.flexWidth),
                        onPress = {
                            if (keyItem.isModifier) {
                                when (keyItem.modifierBit) {
                                    HidModifiers.MODIFIER_LEFT_SHIFT, HidModifiers.MODIFIER_RIGHT_SHIFT -> onShiftClick()
                                    HidModifiers.MODIFIER_LEFT_CTRL -> onCtrlClick()
                                    HidModifiers.MODIFIER_LEFT_ALT -> onAltClick()
                                    HidModifiers.MODIFIER_LEFT_GUI -> onWinClick()
                                }
                                if (keyItem.code == HidKeys.KEY_CAPS_LOCK) {
                                    onCapsClick()
                                }
                            } else {
                                onKeyPress(keyItem.code)
                            }
                        },
                        onRelease = {
                            if (!keyItem.isModifier && keyItem.code != HidKeys.KEY_CAPS_LOCK) {
                                onKeyRelease(keyItem.code)
                            }
                        }
                    )
                }
            }
        }
    }
}

// Componente per il singolo tasto fisico simulato
@Composable
fun KeyboardKeyCap(
    keyItem: KeyItem,
    shiftActive: Boolean,
    isStickyActive: Boolean,
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    val view = LocalView.current
    var isPressedLocally by remember { mutableStateOf(false) }

    // Colore tasto a seconda delle funzioni
    val capBackground = when {
        isStickyActive -> Brush.verticalGradient(
            colors = listOf(Color(0xFF00FFCC), Color(0xFF0099FF)) // Tasto speciale attivo: neon ciano/blu
        )
        isPressedLocally -> Brush.verticalGradient(
            colors = listOf(Color(0xFF1A1D2F), Color(0xFF2C324D)) // Pressione attiva
        )
        keyItem.code == HidKeys.KEY_ESC -> Brush.verticalGradient(
            colors = listOf(Color(0xFFFF3B30), Color(0xFFAA1100)) // Rosso per Esc
        )
        keyItem.code == HidKeys.KEY_ENTER || keyItem.code == HidKeys.KEY_BACKSPACE -> Brush.verticalGradient(
            colors = listOf(Color(0xFF9B51E0), Color(0xFF5A189A)) // Viola per tasti grossi d'invio
        )
        keyItem.isModifier || keyItem.code == HidKeys.KEY_TAB -> Brush.verticalGradient(
            colors = listOf(Color(0xFF222533), Color(0xFF161822)) // Modificatori scuriti
        )
        else -> Brush.verticalGradient(
            colors = listOf(Color(0xFF2E3347), Color(0xFF1B1E2B)) // Tasti standard grigiastri
        )
    }

    val capTextColor = if (isStickyActive) Color.Black else Color.White
    val capBorderColor = if (isPressedLocally) Color(0xFF00FF66) else if (isStickyActive) Color.White else Color(0xFF383C53)

    // Animazione di profondità per simulare l'effetto "molla meccanica"
    val visualOffset by animateDpAsState(
        targetValue = if (isPressedLocally || isStickyActive) 1.dp else 4.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "key_spring"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 1.dp)
            .shadow(
                elevation = if (isPressedLocally || isStickyActive) 0.dp else 2.dp,
                shape = RoundedCornerShape(6.dp)
            )
            .background(Color(0xFF0A0B10), RoundedCornerShape(6.dp)) // Base d'appoggio grigio scuro
            .padding(bottom = visualOffset) // Crea lo scalino della profondità meccanica
            .clip(RoundedCornerShape(6.dp))
            .background(capBackground)
            .border(BorderStroke(1.dp, capBorderColor), RoundedCornerShape(6.dp))
            .pointerInput(keyItem.label, keyItem.code) {
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown(requireUnconsumed = false)
                        isPressedLocally = true
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onPress()
                        
                        waitForUpOrCancellation()
                        isPressedLocally = false
                        onRelease()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {
            val hasShiftLabel = keyItem.shiftLabel.isNotEmpty()
            val textLabel = if (shiftActive && !keyItem.isModifier && keyItem.code != HidKeys.KEY_ESC) {
                if (hasShiftLabel) keyItem.shiftLabel else keyItem.label.uppercase()
            } else {
                keyItem.label
            }

            // Se ha un secondo simbolo (es: tasti numerici col shift), disegna l'indicazione superiore
            if (hasShiftLabel && !isPressedLocally && !isStickyActive) {
                Text(
                    text = keyItem.shiftLabel,
                    color = Color(0xFF7E84A3),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }

            Text(
                text = textLabel,
                color = capTextColor,
                fontSize = if (keyItem.label.length > 3) 10.sp else 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

// Funzione di utilità per verificare al volo i permessi Bluetooth necessari
fun checkBluetoothPermissions(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}
