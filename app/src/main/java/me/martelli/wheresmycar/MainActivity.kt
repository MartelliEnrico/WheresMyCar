package me.martelli.wheresmycar

import android.Manifest
import android.app.PendingIntent
import android.app.UiModeManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFloatingActionButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.checkSelfPermission
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.content.pm.ShortcutManagerCompat.FLAG_MATCH_PINNED
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.martelli.wheresmycar.data.AppViewModel
import me.martelli.wheresmycar.data.Event
import me.martelli.wheresmycar.data.UiState
import me.martelli.wheresmycar.data.hasLocation
import me.martelli.wheresmycar.proto.Device
import java.lang.reflect.Method
import kotlin.math.absoluteValue
import kotlin.math.sign

class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels { AppViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        val keepSplashscreen = MutableStateFlow(true)
        installSplashScreen().setKeepOnScreenCondition { keepSplashscreen.value }
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        setContent {
            val uiState by appViewModel.uiState.collectAsState()

            LaunchedEffect(uiState.loading) {
                keepSplashscreen.value = uiState.loading
            }

            App(uiState = uiState)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun App(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    uiState: UiState
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkColorScheme()
        else -> expressiveLightColorScheme()
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
    ) {
        LaunchedEffect(context, uiState.devices) {
            if (ShortcutManagerCompat.getDynamicShortcuts(context).isEmpty()) {
                if (ShortcutManagerCompat.getShortcuts(context, FLAG_MATCH_PINNED).isNotEmpty()) {
                    uiState.devices.forEach {
                        if (it.hasLocation) {
                            pushDynamicShortcut(context, it)
                        }
                    }
                }
            }
        }

        AnimatedContent(uiState.onboardingCompleted) {
            if (it) {
                AppContent(uiState.devices, uiState.eventSink)
            } else {
                Onboarding(uiState.eventSink)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
fun AppContent(devices: List<Device>, eventSink: (Event) -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = rememberHazeState(blurEnabled = true)
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.hazeEffect(state = hazeState, style = HazeMaterials.ultraThin()),
                title = { Text(stringResource(R.string.app_name)) },
                actions = { MainMenu() },
                colors = TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                ),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FindCar(
                devices = devices,
                eventSink = eventSink
            )
        },
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 112.dp)
                    + WindowInsets.navigationBars.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(devices, key = { it.address }) {
                DeviceCard(
                    modifier = Modifier.animateItem(),
                    device = it,
                    eventSink = eventSink
                )
            }

            if (devices.isEmpty()) {
                item("empty_state") {
                    EmptyState(
                        modifier = Modifier
                            .animateItem()
                            .fillParentMaxWidth()
                            .aspectRatio(1f)
                            .clip(CardDefaults.shape)
                    )
                }
            }
        }
    }
}

operator fun PaddingValues.plus(other: PaddingValues): PaddingValues = PaddingValues(
    start = this.calculateStartPadding(LayoutDirection.Ltr) +
            other.calculateStartPadding(LayoutDirection.Ltr),
    top = this.calculateTopPadding() + other.calculateTopPadding(),
    end = this.calculateEndPadding(LayoutDirection.Ltr) +
            other.calculateEndPadding(LayoutDirection.Ltr),
    bottom = this.calculateBottomPadding() + other.calculateBottomPadding(),
)

@Composable
fun DeviceCard(modifier: Modifier = Modifier, device: Device, eventSink: (Event) -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        if (device.hasLocation) {
            LocationMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9, matchHeightConstraintsFirst = true)
                    .clip(CardDefaults.shape),
                device = device
            )
        }

        DeviceInfo(
            modifier = Modifier.padding(vertical = 8.dp),
            device = device,
            updateDevice = { eventSink(Event.UpdateDevice(it)) },
            removeDevice = { eventSink(Event.RemoveDevice(device)) },
        )
    }
}

@Composable
fun MainMenu() {
    val uiModeManager = LocalContext.current.getSystemService(UiModeManager::class.java)

    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    var themeDialog by rememberSaveable { mutableStateOf(false) }

    val allGranted = allPermissionsGranted(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
    )

    val onClick = { menuExpanded = true }
    val icon = remember {
        movableContentOf {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null
            )
        }
    }

    if (allGranted) {
        IconButton(
            onClick = onClick,
            content = icon
        )
    } else {
        ErrorIconButton(
            onClick = onClick,
            content = icon
        )
    }

    DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false },
        modifier = Modifier.requiredSizeIn(minWidth = 168.dp)
    ) {
        PermissionBox(
            permission = Manifest.permission.BLUETOOTH_CONNECT,
            rationale = stringResource(R.string.bluetooth_rationale),
            button = {
                DropdownMenuItem(
                    text = {
                        Text(stringResource(R.string.bluetooth_permission))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.BluetoothConnected,
                            contentDescription = null
                        )
                    },
                    onClick = it
                )
            },
            onGranted = {}
        )

        PermissionBox(
            permissions = listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            rationale = stringResource(R.string.permissions_rationale),
            button = {
                DropdownMenuItem(
                    text = {
                        Text(stringResource(R.string.location_permissions))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null
                        )
                    },
                    onClick = it
                )
            }
        ) {
            val context = LocalContext.current
            PermissionBox(
                permission = Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                rationale = stringResource(
                    id = R.string.background_rationale,
                    context.packageManager.backgroundPermissionOptionLabel
                ),
                button = {
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.location_permissions))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null
                            )
                        },
                        onClick = it
                    )
                },
                onGranted = {}
            )
        }

        if (!allGranted) {
            HorizontalDivider()
        }

        DropdownMenuItem(
            text = {
                Text(stringResource(R.string.choose_theme))
            },
            onClick = {
                menuExpanded = false
                themeDialog = true
            }
        )

        DropdownMenuItem(
            text = { Text(stringResource(R.string.version, BuildConfig.VERSION_NAME)) },
            onClick = {},
            enabled = false,
            colors = MenuDefaults.itemColors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }

    val themes = mapOf(
        UiModeManager.MODE_NIGHT_AUTO to stringResource(R.string.theme_auto),
        UiModeManager.MODE_NIGHT_NO to stringResource(R.string.theme_light),
        UiModeManager.MODE_NIGHT_YES to stringResource(R.string.theme_dark),
    )

    var selectedTheme by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(themeDialog) { selectedTheme = null }

    if (themeDialog) {
        AlertDialog(
            onDismissRequest = { themeDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        themeDialog = false

                        selectedTheme?.let {
                            uiModeManager.setApplicationNightMode(it)
                        }
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { themeDialog = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = {
                Text(stringResource(R.string.choose_theme))
            },
            text = {
                Column(Modifier.selectableGroup()) {
                    themes.forEach { (theme, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = selectedTheme == theme,
                                    onClick = { selectedTheme = theme },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTheme == theme,
                                onClick = null
                            )

                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        )
    }
}

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

fun pushDynamicShortcut(context: Context, device: Device) {
    val shortcut = buildShortcut(context, device)
    ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
}

fun removeDynamicShortcut(context: Context, device: Device) {
    ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(shortcutId(device)))
}

fun buildShortcut(context: Context, device: Device) =
    ShortcutInfoCompat.Builder(context, shortcutId(device))
        .setShortLabel(context.getString(R.string.shortcut_short_description, device.name))
        .setLongLabel(context.getString(R.string.shortcut_long_description, device.name))
        .setIcon(IconCompat.createWithResource(context, R.drawable.location_pin))
        .setIntent(locationIntent(device.latitude, device.longitude))
        .build()

fun shortcutId(device: Device) = "navigate_to_${device.address}"

fun locationIntent(latitude: Double, longitude: Double) =
    Intent(
        Intent.ACTION_VIEW,
        "https://www.google.com/maps/search/?api=1&query=${latitude}%2C${longitude}".toUri()
    )

@Composable
fun LocationMap(modifier: Modifier = Modifier, device: Device) {
    val context = LocalContext.current

    val coordinates = LatLng(device.latitude, device.longitude)
    val cameraPosition = CameraPosition.fromLatLngZoom(coordinates, 16f)

    val cameraPositionState = rememberCameraPositionState {
        position = cameraPosition
    }

    GoogleMap(
        modifier = modifier,
        mergeDescendants = true,
        cameraPositionState = cameraPositionState,
        contentDescription = stringResource(R.string.open_maps),
        properties = MapProperties(
            mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, R.raw.empty_map_style)
        ),
        uiSettings = MapUiSettings(
            compassEnabled = false,
            indoorLevelPickerEnabled = false,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false,
            rotationGesturesEnabled = false,
            scrollGesturesEnabled = false,
            scrollGesturesEnabledDuringRotateOrZoom = false,
            tiltGesturesEnabled = false,
            zoomControlsEnabled = false,
            zoomGesturesEnabled = false,
        ),
        onMapClick = {
            context.startActivity(locationIntent(device.latitude, device.longitude))
        },
        mapColorScheme = ComposeMapColorScheme.FOLLOW_SYSTEM
    ) {
        LaunchedEffect(cameraPositionState, cameraPosition) {
            cameraPositionState.animate(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }

        val markerState = rememberUpdatedMarkerState(position = coordinates)
        Marker(state = markerState)
    }
}

@Composable
fun DeviceInfo(modifier: Modifier = Modifier, device: Device, updateDevice: (Device) -> Unit, removeDevice: () -> Unit) {
    var openDialog by rememberSaveable { mutableStateOf(false) }

    if (openDialog) {
        val focusRequester = remember { FocusRequester() }
        val nameState = rememberTextFieldState(initialText = device.name)

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        AlertDialog(
            onDismissRequest = { openDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        openDialog = false

                        val displayName = nameState.text.ifBlank { device.originalName }.toString()
                        val newDevice = device.toBuilder().setName(displayName).build()
                        updateDevice(newDevice)
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { openDialog = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = {
                Text(stringResource(R.string.rename_device))
            },
            text = {
                TextField(
                    state = nameState,
                    modifier = Modifier.focusRequester(focusRequester),
                    placeholder = { Text(text = device.originalName) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    lineLimits = TextFieldLineLimits.SingleLine
                )
            }
        )
    }

    val context = LocalContext.current
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    ListItem(
        modifier = modifier,
        headlineContent = {
            Text(
                text = device.name,
                style = MaterialTheme.typography.headlineMedium
            )
        },
        supportingContent = {
            if (device.time > 0) {
                Text(text = timeAgo(device.time))
            } else {
                Text(text = stringResource(R.string.no_position_saved))
            }
        },
        trailingContent = {
            IconButton(
                onClick = { menuExpanded = !menuExpanded }
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.requiredSizeIn(minWidth = 168.dp)
            ) {
                DropdownMenuItem(
                    text = {
                        Text(stringResource(R.string.rename_device))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DriveFileRenameOutline,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        openDialog = true
                    }
                )

                if (device.hasLocation && ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.add_shortcut))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AppShortcut,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false

                            val shortcut = buildShortcut(context, device)
                            val successCallback = PendingIntent.getBroadcast(
                                context,
                                0,
                                ShortcutManagerCompat.createShortcutResultIntent(context, shortcut),
                                PendingIntent.FLAG_IMMUTABLE
                            )

                            ShortcutManagerCompat.requestPinShortcut(
                                context,
                                shortcut,
                                successCallback.intentSender
                            )
                        }
                    )
                }

                HorizontalDivider()

                DropdownMenuItem(
                    text = {
                        Text(stringResource(R.string.remove_device))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        removeDevice()
                    }
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    )
}

@Composable
fun timeAgo(time: Long): String {
    var currentTimeMillis by remember {
        mutableLongStateOf(System.currentTimeMillis())
    }

    LaunchedEffect(currentTimeMillis) {
        while (true) {
            delay(1000)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    return DateUtils.getRelativeTimeSpanString(
        time, currentTimeMillis, DateUtils.MINUTE_IN_MILLIS
    ).toString()
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.secondaryContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.empty_state_title),
            style = MaterialTheme.typography.displaySmall
        )
        Text(
            text = stringResource(R.string.empty_state_body),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FindCar(modifier: Modifier = Modifier, devices: List<Device>, eventSink: (Event) -> Unit) {
    var openDialog by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    var selectedDevice by remember { mutableStateOf<SavedDevice?>(null) }

    LaunchedEffect(openDialog) { selectedDevice = null }

    if (openDialog) {
        AlertDialog(
            onDismissRequest = { openDialog = false },
            confirmButton = {
                TextButton(
                    enabled = selectedDevice != null,
                    onClick = {
                        openDialog = false

                        selectedDevice?.let {
                            val device = Device.newBuilder()
                                .setAddress(it.address)
                                .setOriginalName(it.name)
                                .setName(it.name)
                                .build()
                            eventSink(Event.AddDevice(device))
                        }
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { openDialog = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null
                )
            },
            title = {
                Text(stringResource(R.string.find_car))
            },
            text = {
                val connectedDevices = getConnectedBluetoothDevices(context).filter {
                    devices.none { d -> d.address == it.address }
                }
                LazyColumn(Modifier.selectableGroup()) {
                    items(connectedDevices, key = { it.address }) {
                        ListItem(
                            modifier = Modifier.selectable(
                                selected = selectedDevice == it,
                                onClick = { selectedDevice = it },
                                role = Role.RadioButton
                            ),
                            headlineContent = {
                                Text(it.name)
                            },
                            supportingContent = {
                                if (it.connected) {
                                    Text(stringResource(R.string.device_connected))
                                }
                            },
                            leadingContent = {
                                RadioButton(
                                    selected = selectedDevice == it,
                                    onClick = null
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = AlertDialogDefaults.containerColor,
                            )
                        )
                    }
                }
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        openDialog = isGranted
    }

    MediumFloatingActionButton(
        modifier = modifier.navigationBarsPadding(),
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
        },
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        elevation = FloatingActionButtonDefaults.loweredElevation()
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(FloatingActionButtonDefaults.MediumIconSize)
        )
    }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun getConnectedBluetoothDevices(context: Context): List<SavedDevice> {
    return context.getSystemService(BluetoothManager::class.java)?.adapter?.bondedDevices?.filterNotNull().orEmpty().map {
        SavedDevice(it.name, it.address, IsConnected.invoke(it) as Boolean)
    }
}

val IsConnected: Method = BluetoothDevice::class.java.getMethod("isConnected")

data class SavedDevice(
    val name: String,
    val address: String,
    val connected: Boolean
)

@Composable
fun Onboarding(eventSink: (Event) -> Unit) {
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
                                if (pagerState.currentPage == pagerState.pageCount - 1) {
                                    eventSink(Event.CompleteOnboarding)
                                } else {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
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
            userScrollEnabled = false,
            key = { it }
        ) {
            onboardingPages[it]()
        }
    }
}

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
            imageVector = Icons.Default.DirectionsCar,
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
            imageVector = Icons.Default.BluetoothConnected,
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
                color = MaterialTheme.colorScheme.primary
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
                ),
                button = {
                    Button(onClick = it) {
                        Text(text = stringResource(R.string.grant_additional_permissions))
                    }
                }
            ) {
                Text(
                    text = stringResource(R.string.permissions_granted),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
