package me.martelli.wheresmycar

import android.Manifest
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltipBox
import androidx.compose.material3.RichTooltipState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.checkSelfPermission
import androidx.core.content.getSystemService
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.content.pm.ShortcutManagerCompat.FLAG_MATCH_PINNED
import androidx.core.graphics.drawable.IconCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.martelli.wheresmycar.ui.theme.WheresMyCarTheme
import java.io.IOException
import kotlin.math.absoluteValue
import kotlin.math.sign

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (ShortcutManagerCompat.getDynamicShortcuts(applicationContext).size == 0) {
            if (ShortcutManagerCompat.getShortcuts(applicationContext, FLAG_MATCH_PINNED).size > 0) {
                val device = runBlocking { applicationContext.savedDevice.first() }
                device?.let {
                    pushDynamicShortcut(applicationContext, it.latitude, it.longitude)
                }
            }
        }

        setContent {
            val context = LocalContext.current
            val configs by remember { context.configurations }.collectAsStateWithLifecycle(
                initialValue = null
            )

            configs?.let {
                WheresMyCarTheme {
                    AnimatedContent(
                        targetState = it.onboardingCompleted,
                        label = "main_content"
                    ) { onboardingCompleted ->
                        if (onboardingCompleted) {
                            AppContent()
                        } else {
                            Onboarding()
                        }
                    }
                }
            }
        }
    }

    @Composable
    @OptIn(ExperimentalFoundationApi::class)
    private fun Onboarding() {
        val context = LocalContext.current
        val pagerState = rememberPagerState { onboardingPages.size }

        Scaffold(
            bottomBar = {
                Surface(
                    color = BottomAppBarDefaults.containerColor,
                    tonalElevation = BottomAppBarDefaults.ContainerElevation,
                    shape = RoundedCornerShape(32.dp, 32.dp, 0.dp, 0.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .windowInsetsPadding(BottomAppBarDefaults.windowInsets)
                            .height(80.dp)
                            .padding(16.dp, 4.dp, 16.dp, 0.dp)
                    ) {
                        HorizontalPagerIndicator(pagerState)

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val coroutineScope = rememberCoroutineScope()
                            AnimatedVisibility(visible = pagerState.currentPage > 0) {
                                TextButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(
                                                pagerState.currentPage - 1
                                            )
                                        }
                                    }
                                ) {
                                    Text(stringResource(R.string.back))
                                }
                            }

                            Spacer(Modifier.weight(1f))

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        if (pagerState.currentPage == pagerState.pageCount - 1) {
                                            context.configurationsStore.edit { prefs ->
                                                prefs[OnboardingCompleted] = true
                                            }
                                        } else {
                                            pagerState.animateScrollToPage(
                                                pagerState.currentPage + 1
                                            )
                                        }
                                    }
                                }
                            ) {
                                val text = if (pagerState.currentPage == pagerState.pageCount - 1) {
                                    stringResource(R.string.complete_onboarding)
                                } else {
                                    stringResource(R.string.next)
                                }

                                AnimatedContent(
                                    targetState = text,
                                    label = "onboarding_next_button"
                                ) {
                                    Text(it)
                                }
                            }
                        }
                    }
                }
            }
        ) { contentPadding ->
            HorizontalPager(
                state = pagerState,
                contentPadding = contentPadding,
                beyondBoundsPageCount = 1,
                userScrollEnabled = false,
                key = { it }
            ) {
                onboardingPages[it]()
            }
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun AppContent() {
        val context = LocalContext.current
        val selectedDevice by remember { context.savedDevice }.collectAsStateWithLifecycle(
            initialValue = null
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(stringResource(R.string.app_name))
                    },
                    actions = {
                        PermissionBox(
                            permission = Manifest.permission.BLUETOOTH_CONNECT,
                            rationale = stringResource(R.string.bluetooth_rationale),
                            button = {
                                ErrorIconButton(onClick = it) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.bluetooth_connected),
                                        contentDescription = null
                                    )
                                }
                            }
                        ) {
                            PermissionBox(
                                permissions = listOf(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ),
                                rationale = stringResource(R.string.permissions_rationale),
                                button = {
                                    ErrorIconButton(onClick = it) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null
                                        )
                                    }
                                }
                            ) {
                                PermissionBox(
                                    permission = Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                                    rationale = stringResource(
                                        id = R.string.background_rationale,
                                        context.packageManager.backgroundPermissionOptionLabel
                                    ),
                                    button = {
                                        ErrorIconButton(onClick = it) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    onGranted = {}
                                )
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                InstallShortcut()
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FindCar()

                selectedDevice?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    DeviceInfo(device = it)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box {
        FilledTonalIconButton(
            onClick = onClick,
            content = content
        )
        Badge(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
        ) {
            Text("!")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HorizontalPagerIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    activeColor: Color = LocalContentColor.current,
    inactiveColor: Color = activeColor.copy(0.38f),
    indicatorWidth: Dp = 8.dp,
    indicatorHeight: Dp = indicatorWidth,
    spacing: Dp = indicatorWidth,
    indicatorShape: Shape = CircleShape,
) {
    val indicatorWidthPx = LocalDensity.current.run { indicatorWidth.roundToPx() }
    val spacingPx = LocalDensity.current.run { spacing.roundToPx() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val indicatorModifier = Modifier
                .size(width = indicatorWidth, height = indicatorHeight)
                .background(color = inactiveColor, shape = indicatorShape)

            repeat(pagerState.pageCount) {
                Box(indicatorModifier)
            }
        }

        Box(
            Modifier
                .offset {
                    val position = pagerState.currentPage
                    val offset = pagerState.currentPageOffsetFraction
                    val next = pagerState.currentPage + offset.sign.toInt()
                    val scrollPosition = ((next - position) * offset.absoluteValue + position)
                        .coerceIn(
                            0f,
                            (pagerState.pageCount - 1)
                                .coerceAtLeast(0)
                                .toFloat()
                        )

                    IntOffset(
                        x = ((spacingPx + indicatorWidthPx) * scrollPosition).toInt(),
                        y = 0
                    )
                }
                .size(width = indicatorWidth, height = indicatorHeight)
                .then(
                    if (pagerState.pageCount > 0) Modifier.background(
                        color = activeColor,
                        shape = indicatorShape,
                    )
                    else Modifier
                )
        )
    }
}

val onboardingPages: List<@Composable () -> Unit> = listOf(
    { Welcome() },
    { BluetoothPermission() },
    { LocationPermissions() },
)

@Composable
fun Welcome() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.directions_car),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = stringResource(R.string.app_description),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}

@Composable
fun BluetoothPermission() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.bluetooth_connected),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = stringResource(R.string.bluetooth_permission),
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = stringResource(R.string.bluetooth_description),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        PermissionBox(
            permission = Manifest.permission.BLUETOOTH_CONNECT,
            rationale = stringResource(R.string.bluetooth_rationale)
        ) {
            Text(
                text = stringResource(R.string.permissions_granted),
                color = Color(0xFF3A4032)
            )
        }
    }
}

@Composable
fun LocationPermissions() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = stringResource(R.string.location_permissions),
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = stringResource(R.string.location_description),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        val context = LocalContext.current
        PermissionBox(
            permissions = listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            rationale = stringResource(R.string.permissions_rationale)
        ) {
            PermissionBox(
                permission = Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                rationale = stringResource(
                    id = R.string.background_rationale,
                    context.packageManager.backgroundPermissionOptionLabel
                )
            ) {
                Text(
                    text = stringResource(R.string.permissions_granted),
                    color = Color(0xFF3A4032)
                )
            }
        }
    }
}

@Composable
fun FindCar(modifier: Modifier = Modifier) {
    var openDialog by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
                Text(stringResource(R.string.find_car))
            },
            text = {
                val connectedDevices = getConnectedBluetoothDevices(context)
                LazyColumn {
                    items(connectedDevices, key = { it.address }) {
                        ListItem(
                            modifier = Modifier.clickable {
                                openDialog = false
                                coroutineScope.launch {
                                    context.savedDeviceStore.edit { preferences ->
                                        preferences[Name] = it.name
                                        preferences[Address] = it.address
                                    }
                                }
                            },
                            headlineContent = {
                                Text(it.name)
                            },
                            supportingContent = {
                                Text(it.address)
                            },
                            overlineContent = {
                                if (it.connected) {
                                    Text(stringResource(R.string.device_connected))
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
        Text(stringResource(R.string.find_car))
    }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun getConnectedBluetoothDevices(context: Context): List<Device> {
    val isConnected = BluetoothDevice::class.java.getMethod("isConnected")

    return context.getSystemService<BluetoothManager>()?.adapter?.bondedDevices?.filterNotNull().orEmpty().map {
        Device(it.name, it.address, isConnected.invoke(it) as Boolean)
    }
}

data class Config(
    val onboardingCompleted: Boolean
)

const val Configurations = "configurations"
val OnboardingCompleted = booleanPreferencesKey("onboarding_completed")

val Context.configurationsStore by preferencesDataStore(
    name = Configurations
)

val Context.configurations: Flow<Config>
    get() = configurationsStore.data
        .catch { e ->
            if (e is IOException) {
                emit(emptyPreferences())
            } else {
                throw e
            }
        }.map { preferences ->
            val onboardingCompleted = preferences[OnboardingCompleted] ?: false

            Config(onboardingCompleted)
        }

data class Device(
    val name: String,
    val address: String,
    val connected: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val time: Long = 0
)

const val SavedDevice = "saved_device"
val Name = stringPreferencesKey("name")
val Address = stringPreferencesKey("address")
val Latitude = floatPreferencesKey("latitude")
val Longitude = floatPreferencesKey("longitude")
val Time = longPreferencesKey("time")

val Context.savedDeviceStore by preferencesDataStore(
    name = SavedDevice
)

val Context.savedDevice: Flow<Device?>
    get() = savedDeviceStore.data
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
            val time = preferences[Time] ?: 0

            if (name != null && address != null) {
                Device(name, address, false, latitude.toDouble(), longitude.toDouble(), time)
            } else {
                null
            }
        }

const val ShortcutId = "navigate"

fun locationIntent(latitude: Double, longitude: Double) =
    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${latitude}%2C${longitude}"))

fun buildShortcut(context: Context, latitude: Double, longitude: Double) =
    ShortcutInfoCompat.Builder(context, ShortcutId)
    .setShortLabel(context.getString(R.string.shortcut_short_description))
    .setLongLabel(context.getString(R.string.shortcut_long_description))
    .setIcon(IconCompat.createWithResource(context, R.drawable.directions_car))
    .setIntent(locationIntent(latitude, longitude))
    .build()

fun pushDynamicShortcut(context: Context, latitude: Double, longitude: Double) {
    val shortcut = buildShortcut(context, latitude, longitude)
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
        cameraPositionState.animate(CameraUpdateFactory.newCameraPosition(position))
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
                    rotationGesturesEnabled = false,
                    scrollGesturesEnabled = false,
                    scrollGesturesEnabledDuringRotateOrZoom = false,
                    tiltGesturesEnabled = false,
                    zoomControlsEnabled = false,
                    zoomGesturesEnabled = false
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
                Text(stringResource(R.string.open_maps))
            }
        }

        if (device.time > 0) {
            val datetime = DateUtils.formatDateTime(
                context,
                device.time,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
            )
            Text(
                text = stringResource(R.string.last_check, datetime),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallShortcut() {
    val context = LocalContext.current
    if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
        val tooltipState = remember { RichTooltipState() }
        val coroutineScope = rememberCoroutineScope()

        RichTooltipBox(
            tooltipState = tooltipState,
            title = {
                Text("Add Shortcut")
            },
            text = {
                Text("With this shortcut added to your home screen, you can find your car location even faster!")
            },
            action = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            tooltipState.dismiss()
                        }
                    }
                ) {
                    Text("Got it!")
                }
            }
        ) {
            FloatingActionButton(
                modifier = Modifier.tooltipAnchor(),
                onClick = {
                    val shortcut = buildShortcut(context, 0.0, 0.0) // not the best
                    val shortcutResultIntent = ShortcutManagerCompat.createShortcutResultIntent(context, shortcut)
                    val successCallback = PendingIntent.getBroadcast(context, 0, shortcutResultIntent, PendingIntent.FLAG_IMMUTABLE)

                    ShortcutManagerCompat.requestPinShortcut(context, shortcut, successCallback.intentSender)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.add_shortcut)
                )
            }
        }
    }
}
