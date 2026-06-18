package com.example

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors

@SuppressLint("MissingPermission")
class BluetoothHidManager(private val context: Context) {

    private val TAG = "BluetoothHidManager"

    private var bluetoothManager: BluetoothManager? = null
    var bluetoothAdapter: BluetoothAdapter? = null
    var hidDevice: BluetoothHidDevice? = null

    private val _isBluetoothEnabled = MutableStateFlow(false)
    val isBluetoothEnabled = _isBluetoothEnabled.asStateFlow()

    private val _isHidSupported = MutableStateFlow(false)
    val isHidSupported = _isHidSupported.asStateFlow()

    private val _isRegistered = MutableStateFlow(false)
    val isRegistered = _isRegistered.asStateFlow()

    private val _connectionState = MutableStateFlow(BluetoothProfile.STATE_DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice = _connectedDevice.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices = _pairedDevices.asStateFlow()

    private val _logFlow = MutableStateFlow<List<String>>(listOf("Manager di tastiera virtuale inizializzato."))
    val logFlow = _logFlow.asStateFlow()

    init {
        initBluetooth()
    }

    fun addLog(msg: String) {
        Log.d(TAG, msg)
        val current = _logFlow.value.toMutableList()
        current.add(msg)
        if (current.size > 100) {
            current.removeAt(0)
        }
        _logFlow.value = current
    }

    fun initBluetooth() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            addLog("Errore: Il tuo dispositivo ha Android inferiore a 9 (API 28). Il Bluetooth HID Device non è supportato.")
            _isHidSupported.value = false
            return
        }
        try {
            bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bluetoothAdapter = bluetoothManager?.adapter
            
            val adapter = bluetoothAdapter
            if (adapter == null) {
                addLog("Errore: Il Bluetooth non è presente su questo dispositivo.")
                _isHidSupported.value = false
                return
            }

            _isBluetoothEnabled.value = adapter.isEnabled
            if (adapter.isEnabled) {
                queryPairedDevices()
                bindHidProxy()
            } else {
                addLog("Bluetooth disattivato. Attivalo per procedere.")
            }
        } catch (e: Exception) {
            addLog("Eccezione durante initBluetooth: ${e.message}")
        }
    }

    fun queryPairedDevices() {
        try {
            val devices = bluetoothAdapter?.bondedDevices
            _pairedDevices.value = devices?.toList() ?: emptyList()
            addLog("Trovati ${devices?.size ?: 0} dispositivi associati.")
        } catch (e: Exception) {
            addLog("Errore nel recupero dispositivi associati: ${e.message}")
        }
    }

    private fun bindHidProxy() {
        val adapter = bluetoothAdapter ?: return
        addLog("Connessione in corso al proxy HID Device...")
        
        val listener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hidDevice = proxy as? BluetoothHidDevice
                    val supported = hidDevice != null
                    _isHidSupported.value = supported
                    if (supported) {
                        addLog("Proxy HID Device connesso con successo.")
                        registerHidApp()
                    } else {
                        addLog("Errore: HID Device Profile non supportato su questo dispositivo.")
                    }
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    addLog("Proxy HID Device disconnesso.")
                    hidDevice = null
                    _isRegistered.value = false
                    _connectionState.value = BluetoothProfile.STATE_DISCONNECTED
                    _connectedDevice.value = null
                }
            }
        }

        try {
            val success = adapter.getProfileProxy(context, listener, BluetoothProfile.HID_DEVICE)
            if (!success) {
                addLog("getProfileProxy per HID fallito.")
                _isHidSupported.value = false
            }
        } catch (e: Exception) {
            addLog("Eccezione in getProfileProxy: ${e.message}")
            _isHidSupported.value = false
        }
    }

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            super.onAppStatusChanged(pluggedDevice, registered)
            _isRegistered.value = registered
            if (registered) {
                addLog("Profilo Tastiera virtuale registrato nello stack Bluetooth.")
            } else {
                addLog("Profilo Tastiera virtuale disattivato dello stack Bluetooth.")
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            super.onConnectionStateChanged(device, state)
            _connectionState.value = state
            if (state == BluetoothProfile.STATE_CONNECTED) {
                _connectedDevice.value = device
                addLog("Connesso al PC: ${device?.name ?: "Dispositivo Bluetooth"} [${device?.address}]")
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                _connectedDevice.value = null
                addLog("Host HID scollegato.")
            } else if (state == BluetoothProfile.STATE_CONNECTING) {
                addLog("Inizializzazione connessione a: ${device?.name ?: device?.address}...")
            }
        }
    }

    fun registerHidApp() {
        val deviceProxy = hidDevice ?: return
        if (_isRegistered.value) {
            return
        }

        try {
            val sdpSettings = BluetoothHidDeviceAppSdpSettings(
                "Virtual Keyboard API",
                "Full Screen Bluetooth Keyboard",
                "Android",
                0x40.toByte(), // Subclass KEYBOARD
                DESCRIPTOR
            )

            val executor = Executors.newSingleThreadExecutor()
            val success = deviceProxy.registerApp(sdpSettings, null, null, executor, callback)
            if (success) {
                addLog("Richiesta di registrazione Tastiera avviata...")
            } else {
                addLog("Errore nella registrazione: registerApp ha ritornato false.")
            }
        } catch (e: Exception) {
            addLog("Errore registrazione App HID: ${e.message}")
        }
    }

    fun unregisterHidApp() {
        val deviceProxy = hidDevice ?: return
        try {
            val success = deviceProxy.unregisterApp()
            addLog("Deregistrazione richiamata: $success")
            _isRegistered.value = false
        } catch (e: Exception) {
            addLog("Errore deregistrazione: ${e.message}")
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        val proxy = hidDevice
        if (proxy == null) {
            addLog("Impossibile connettere: proxy HID nullo.")
            return
        }
        addLog("Connessione a ${device.name ?: device.address}...")
        try {
            proxy.connect(device)
        } catch (e: Exception) {
            addLog("Errore connect HID: ${e.message}")
        }
    }

    fun disconnectDevice() {
        val proxy = hidDevice
        val device = _connectedDevice.value
        if (proxy == null || device == null) {
            addLog("Nessun host attivo da scollegare.")
            return
        }
        try {
            proxy.disconnect(device)
            addLog("Scollegamento in corso...")
        } catch (e: Exception) {
            addLog("Errore disconnessione HID: ${e.message}")
        }
    }

    fun sendKeys(modifiers: Byte, activeKeys: List<Byte>): Boolean {
        val proxy = hidDevice ?: return false
        val device = _connectedDevice.value ?: return false

        val report = ByteArray(8)
        report[0] = modifiers
        report[1] = 0.toByte()
        
        for (i in 0 until 6) {
            if (i < activeKeys.size) {
                report[2 + i] = activeKeys[i]
            } else {
                report[2 + i] = 0.toByte()
            }
        }

        return try {
            proxy.sendReport(device, 1, report)
        } catch (e: Exception) {
            Log.e(TAG, "Errore invio report tasti: ${e.message}")
            false
        }
    }

    companion object {
        // Report Descriptor HID standard per Tastiera (Keyboard)
        private val DESCRIPTOR = byteArrayOf(
            0x05.toByte(), 0x01.toByte(), // USAGE_PAGE (Generic Desktop)
            0x09.toByte(), 0x06.toByte(), // USAGE (Keyboard)
            0xA1.toByte(), 0x01.toByte(), // COLLECTION (Application)
            0x85.toByte(), 0x01.toByte(), //   REPORT_ID (1)
            
            // Modificatori (Ctrl, Shift, Alt, Windows)
            0x05.toByte(), 0x07.toByte(), //   USAGE_PAGE (Keyboard)
            0x19.toByte(), 0xE0.toByte(), //   USAGE_MINIMUM (Keyboard LeftControl)
            0x29.toByte(), 0xE7.toByte(), //   USAGE_MAXIMUM (Keyboard Right GUI)
            0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
            0x25.toByte(), 0x01.toByte(), //   LOGICAL_MAXIMUM (1)
            0x75.toByte(), 0x01.toByte(), //   REPORT_SIZE (1)
            0x95.toByte(), 0x08.toByte(), //   REPORT_COUNT (8)
            0x81.toByte(), 0x02.toByte(), //   INPUT (Data,Var,Abs)
            
            // Riservato
            0x95.toByte(), 0x01.toByte(), //   REPORT_COUNT (1)
            0x75.toByte(), 0x08.toByte(), //   REPORT_SIZE (8)
            0x81.toByte(), 0x01.toByte(), //   INPUT (Cnst,Ary,Abs)
            
            // Feedback LED (Output)
            0x95.toByte(), 0x05.toByte(), //   REPORT_COUNT (5)
            0x75.toByte(), 0x01.toByte(), //   REPORT_SIZE (1)
            0x05.toByte(), 0x08.toByte(), //   USAGE_PAGE (LEDs)
            0x19.toByte(), 0x01.toByte(), //   USAGE_MINIMUM (Num Lock)
            0x29.toByte(), 0x05.toByte(), //   USAGE_MAXIMUM (Kana)
            0x91.toByte(), 0x02.toByte(), //   OUTPUT (Data,Var,Abs)
            0x95.toByte(), 0x01.toByte(), //   REPORT_COUNT (1)
            0x75.toByte(), 0x03.toByte(), //   REPORT_SIZE (3)
            0x91.toByte(), 0x01.toByte(), //   OUTPUT (Cnst,Ary,Abs)
            
            // Tastiera Standard Keycodes
            0x95.toByte(), 0x06.toByte(), //   REPORT_COUNT (6)
            0x75.toByte(), 0x08.toByte(), //   REPORT_SIZE (8)
            0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
            0x25.toByte(), 0x65.toByte(), //   LOGICAL_MAXIMUM (101)
            0x05.toByte(), 0x07.toByte(), //   USAGE_PAGE (Keyboard)
            0x19.toByte(), 0x00.toByte(), //   USAGE_MINIMUM (Reserved)
            0x29.toByte(), 0x65.toByte(), //   USAGE_MAXIMUM (Keyboard Application)
            0x81.toByte(), 0x00.toByte(), //   INPUT (Data,Ary,Abs)
            
            0xC0.toByte()                 // END_COLLECTION
        )
    }
}
