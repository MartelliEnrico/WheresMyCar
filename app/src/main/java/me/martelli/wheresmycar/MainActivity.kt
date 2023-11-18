package me.martelli.wheresmycar

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.parcelize.Parcelize
import me.martelli.wheresmycar.ui.theme.WheresMyCarTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val selectedDevice by subscribeToDevice(context).collectAsStateWithLifecycle(initialValue = getSavedDevice(context))
            var permissionsGranted by rememberSaveable { mutableStateOf(permissionsGranted(context, *AllLocationPermissions)) }

            WheresMyCarTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text(stringResource(R.string.app_name)) })
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FindCar(
                            selectDevice = {
                                saveDevice(context, it)
                            }
                        )
                        if (!permissionsGranted) {
                            Spacer(modifier = Modifier.height(16.dp))
                            GetLocationPermissions(
                                permissionsGranted = {
                                    permissionsGranted = true
                                }
                            )
                        }
                        selectedDevice?.let {
                            Spacer(modifier = Modifier.height(16.dp))
                            DeviceInfo(device = it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FindCar(modifier: Modifier = Modifier, selectDevice: (Device) -> Unit) {
    var openDialog by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    if (openDialog) {
        AlertDialog(
            onDismissRequest = { openDialog = false },
            confirmButton = {},
            icon = {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null
                )
            },
            title = {
                Text("Select your Car")
            },
            text = {
                val connectedDevices = getConnectedBluetoothDevices(context)
                LazyColumn {
                    items(connectedDevices, key = { it.address }) {
                        ListItem(
                            modifier = Modifier.clickable {
                                openDialog = false
                                selectDevice(it)
                            },
                            headlineContent = {
                                Text(it.name)
                            },
                            supportingContent = {
                                Text(it.address)
                            },
                            overlineContent = {
                                if (it.connected) {
                                    Text("Connected")
                                }
                            }
                        )
                    }
                }
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission Accepted: Do something
            openDialog = true
        }
    }

    Button(
        modifier = modifier,
        onClick = {
            when (PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) -> {
                    // Some works that require permission
                    openDialog = true
                }
                else -> {
                    // Asking for permission
                    launcher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }
        }
    ) {
        Text("Select you car")
    }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun getConnectedBluetoothDevices(context: Context): List<Device> {
    val isConnected = BluetoothDevice::class.java.getMethod("isConnected")

    return context.getSystemService<BluetoothManager>()?.adapter?.bondedDevices?.filterNotNull().orEmpty().map {
        Device(it.name, it.address, isConnected.invoke(it) as Boolean)
    }
}

@Parcelize
data class Device(
    val name: String,
    val address: String,
    val connected: Boolean = false,
    val latitude: Float = -1f,
    val longitude: Float = -1f
) : Parcelable

const val SharedPreference = "saved_device"
const val Name = "name"
const val Address = "address"
const val Latitude = "Latitude"
const val Longitude = "Longitude"

fun getSavedDevice(context: Context): Device? {
    val sharedPref = context.getSharedPreferences(SharedPreference, Context.MODE_PRIVATE)
    val name = sharedPref.getString(Name, null)
    val address = sharedPref.getString(Address, null)
    val latitude = sharedPref.getFloat(Latitude, -1f)
    val longitude = sharedPref.getFloat(Longitude, -1f)

    if (name != null && address != null) {
        return Device(name, address, false, latitude, longitude)
    } else {
        return null
    }
}

fun subscribeToDevice(context: Context): Flow<Device> {
    return callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPref, _ ->
            val device = getSavedDevice(context)

            if (device != null) {
                trySendBlocking(device)
            }
        }

        val sharedPref = context.getSharedPreferences(SharedPreference, Context.MODE_PRIVATE)
        sharedPref.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { sharedPref.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}

fun saveDevice(context: Context, device: Device) {
    val sharedPref = context.getSharedPreferences(SharedPreference, Context.MODE_PRIVATE)
    with(sharedPref.edit()) {
        putString(Name, device.name)
        putString(Address, device.address)
        commit()
    }
}

@Composable
fun GetLocationPermissions(modifier: Modifier = Modifier, permissionsGranted: () -> Unit) {
    val context = LocalContext.current

    val launcherSingle = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission Accepted: Do something
            permissionsGranted()
        }
    }

    val launcherMultiple = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

        if (coarse && fine) {
            val backgroundPermission = permissionsGranted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)

            if (backgroundPermission) {
                permissionsGranted()
            } else {
                launcherSingle.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
    }

    Button(
        modifier = modifier,
        onClick = {
            val locationPermissions = permissionsGranted(context, *LocationPermissions)
            val backgroundPermission = permissionsGranted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            when {
                locationPermissions && backgroundPermission -> {
                    // Some works that require permission
                    permissionsGranted()
                }
                locationPermissions -> {
                    launcherSingle.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
                else -> {
                    // Asking for permission
                    launcherMultiple.launch(LocationPermissions)
                }
            }
        }
    ) {
        Text("Grant background location permissions")
    }
}

val LocationPermissions = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
val AllLocationPermissions = LocationPermissions + Manifest.permission.ACCESS_BACKGROUND_LOCATION

fun permissionsGranted(context: Context, vararg permissions: String) = permissions.all {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun DeviceInfo(modifier: Modifier = Modifier, device: Device) {
    val context = LocalContext.current
    val intent = remember(device) { Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${device.latitude}%2C${device.longitude}")) }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = device.name, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { context.startActivity(intent) }) {
            Text("Open Maps")
        }
    }
}
