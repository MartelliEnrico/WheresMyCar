package me.martelli.wheresmycar

import android.Manifest
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.checkSelfPermission
import androidx.core.content.getSystemService
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.martelli.wheresmycar.ui.theme.WheresMyCarTheme
import java.io.IOException

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val selectedDevice by remember { context.savedDevice }.collectAsStateWithLifecycle(initialValue = null)

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

                        selectedDevice?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            DeviceInfo(device = it)
                        }

                        InstallShortcut()

                        PermissionBox(
                            permissions = listOf(
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ),
                            description = stringResource(id = R.string.permissions_rationale)
                        ) {
                            PermissionBox(
                                permission = Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                                description = stringResource(id = R.string.background_rationale, context.packageManager.backgroundPermissionOptionLabel),
                                onGranted = {}
                            )
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
                    painter = painterResource(id = R.drawable.directions_car),
                    contentDescription = null
                )
            },
            title = {
                Text(stringResource(id = R.string.find_car))
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
                                    Text(stringResource(id = R.string.device_connected))
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
            openDialog = true
        }
    }

    Button(
        modifier = modifier,
        onClick = {
            when (PackageManager.PERMISSION_GRANTED) {
                checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) -> {
                    openDialog = true
                }
                else -> {
                    launcher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }
        }
    ) {
        Text(stringResource(id = R.string.find_car))
    }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun getConnectedBluetoothDevices(context: Context): List<Device> {
    val isConnected = BluetoothDevice::class.java.getMethod("isConnected")

    return context.getSystemService<BluetoothManager>()?.adapter?.bondedDevices?.filterNotNull().orEmpty().map {
        Device(it.name, it.address, isConnected.invoke(it) as Boolean)
    }
}

data class Device(
    val name: String,
    val address: String,
    val connected: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
)

const val SharedPreference = "saved_device"
val Name = stringPreferencesKey("name")
val Address = stringPreferencesKey("address")
val Latitude = floatPreferencesKey("latitude")
val Longitude = floatPreferencesKey("longitude")

val Context.dataStore by preferencesDataStore(
    name = SharedPreference
)

val Context.savedDevice: Flow<Device?>
    get() = dataStore.data
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

const val ShortcutId = "navigate"

fun locationIntent(latitude: Double, longitude: Double) =
    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${latitude}%2C${longitude}"))

fun pushDynamicShortcut(context: Context, latitude: Double, longitude: Double) {
    val shortcut = ShortcutInfoCompat.Builder(context, ShortcutId)
        .setShortLabel(context.getString(R.string.shortcut_short_description))
        .setLongLabel(context.getString(R.string.shortcut_long_description))
        .setIcon(IconCompat.createWithResource(context, R.drawable.directions_car))
        .setIntent(locationIntent(latitude, longitude))
        .build()

    ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
}

@Composable
fun DeviceInfo(modifier: Modifier = Modifier, device: Device) {
    val context = LocalContext.current
    val intent = remember(device) { locationIntent(device.latitude, device.longitude) }

    val coordinates = LatLng(device.latitude, device.longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(coordinates, 16f)
    }

    LaunchedEffect(coordinates) {
        val position = CameraPosition.fromLatLngZoom(coordinates, 16f)
        cameraPositionState.move(CameraUpdateFactory.newCameraPosition(position))
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = device.name, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .padding(16.dp)
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
        ) {
            val mapStyleOptions = if (isSystemInDarkTheme()) {
                MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
            } else {
                null
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapStyleOptions = mapStyleOptions
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false
                ),
                onMapClick = { context.startActivity(intent) }
            ) {
                Marker(state = MarkerState(position = coordinates))
            }

            Button(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                onClick = { context.startActivity(intent) }
            ) {
                Text(stringResource(id = R.string.open_maps))
            }
        }
    }
}

@Composable
fun InstallShortcut() {
    val context = LocalContext.current
    val shortcutManager = context.getSystemService<ShortcutManager>()

    if (shortcutManager!!.isRequestPinShortcutSupported) {
        Button(
            onClick = {
                val pinShortcutInfo = ShortcutInfo.Builder(context, ShortcutId).build()
                val pinnedShortcutCallbackIntent = shortcutManager.createShortcutResultIntent(pinShortcutInfo)
                val successCallback = PendingIntent.getBroadcast(context, 0, pinnedShortcutCallbackIntent, PendingIntent.FLAG_IMMUTABLE)

                shortcutManager.requestPinShortcut(pinShortcutInfo, successCallback.intentSender)
            }
        ) {
            Text(stringResource(id = R.string.add_shortcut))
        }
    }
}
