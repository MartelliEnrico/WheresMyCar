package me.martelli.wheresmycar

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.parcelize.Parcelize
import me.martelli.wheresmycar.ui.theme.WheresMyCarTheme
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()

            var permissionsGranted by rememberSaveable { mutableStateOf(permissionsGranted(context, *AllLocationPermissions)) }

            val selectedDevice by remember {
                context.dataStore.data
                    .catch { e ->
                        if (e is IOException) {
                            emit(emptyPreferences())
                        } else {
                            throw e
                        }
                    }.map { preferences ->
                        val name = preferences[Name]
                        val address = preferences[Address]
                        val latitude = preferences[Latitude] ?: 0.0
                        val longitude = preferences[Longitude] ?: 0.0

                        if (name != null && address != null) {
                            Device(name, address, false, latitude.toDouble(), longitude.toDouble())
                        } else {
                            null
                        }
                    }
            }.collectAsStateWithLifecycle(initialValue = null)

            WheresMyCarTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text(stringResource(R.string.app_name)) })
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FindCar(
                            selectDevice = {
                                coroutineScope.launch {
                                    context.dataStore.edit { preferences ->
                                        preferences[Name] = it.name
                                        preferences[Address] = it.address
                                    }
                                }
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
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
) : Parcelable

const val SharedPreference = "saved_device"
val Name = stringPreferencesKey("name")
val Address = stringPreferencesKey("address")
val Latitude = floatPreferencesKey("latitude")
val Longitude = floatPreferencesKey("longitude")

val Context.dataStore by preferencesDataStore(
    name = SharedPreference
)

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

    val coordinates = LatLng(device.latitude, device.longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(coordinates, 16f)
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = device.name, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier.aspectRatio(1f).padding(16.dp).fillMaxSize().clip(RoundedCornerShape(24.dp))
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false
                ),
                onMapClick = { context.startActivity(intent) }
            ) {
                Marker(state = MarkerState(position = coordinates))
            }
            Button(
                modifier = Modifier.align(Alignment.BottomEnd).padding(horizontal = 8.dp, vertical = 5.dp),
                onClick = {}
            ) {
                Text("Open Maps")
            }
        }
    }
}
