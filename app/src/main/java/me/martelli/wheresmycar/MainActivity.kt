package me.martelli.wheresmycar

import android.Manifest
import android.app.PendingIntent
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
import androidx.annotation.RequiresPermission
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.checkSelfPermission
import androidx.core.content.getSystemService
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.content.pm.ShortcutManagerCompat.FLAG_MATCH_PINNED
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.martelli.wheresmycar.proto.Configs
import me.martelli.wheresmycar.proto.Device
import me.martelli.wheresmycar.proto.Devices
import me.martelli.wheresmycar.ui.theme.DarkGreen
import me.martelli.wheresmycar.ui.theme.WheresMyCarTheme
import java.lang.reflect.Method
import kotlin.math.absoluteValue
import kotlin.math.sign

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val keepSplashscreen = MutableStateFlow(true)
        installSplashScreen().setKeepOnScreenCondition { keepSplashscreen.value }

        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        setContent {
            WheresMyCarTheme {
                val context = LocalContext.current
                val configs by remember { context.configs }.collectAsStateWithLifecycle(
                    initialValue = Configs.getDefaultInstance()
                )
                val devices by remember { context.devices }.collectAsStateWithLifecycle(
                    initialValue = Devices.getDefaultInstance()
                )

                LaunchedEffect(Unit) {
                    delay(400)
                    keepSplashscreen.value = false
                }

                AnimatedContent(
                    targetState = configs.onboardingCompleted,
                    label = "main_content"
                ) { onboardingCompleted ->
                    if (onboardingCompleted) {
                        AppContent(devices.devicesList)
                    } else {
                        Onboarding()
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppContent(devices: List<Device>) {
    val context = LocalContext.current
    LaunchedEffect(devices) {
        if (ShortcutManagerCompat.getDynamicShortcuts(context).size == 0) {
            if (ShortcutManagerCompat.getShortcuts(context, FLAG_MATCH_PINNED).size > 0) {
                devices.forEach {
                    if (it.hasLocation) {
                        pushDynamicShortcut(context, it)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
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
                                        imageVector = Icons.Filled.LocationOn,
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
                                            imageVector = Icons.Filled.LocationOn,
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
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(devices, key = { it.address }) {
                Card(
                    modifier = Modifier.fillParentMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(),
                    elevation = CardDefaults.elevatedCardElevation(),
                ) {
                    if (it.hasLocation) {
                        LocationMap(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9, matchHeightConstraintsFirst = true)
                                .clip(CardDefaults.shape),
                            device = it
                        )
                    }

                    DeviceInfo(device = it)
                }
            }
            if (devices.isEmpty()) {
                item("empty_state") {
                    EmptyState(
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .aspectRatio(1f)
                            .clip(CardDefaults.shape)
                    )
                }
            }
            item("find_car") {
                Box(
                    modifier = Modifier.fillParentMaxWidth()
                ) {
                    FindCar(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(vertical = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceInfo(device: Device) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    var openDialog by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var name by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(device.name, selection = TextRange(device.name.length)))
    }

    if (openDialog) {
        AlertDialog(
            onDismissRequest = { openDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        openDialog = false
                        coroutineScope.launch {
                            val displayName = name.text.ifBlank { device.originalName }
                            val newDevice = device.toBuilder().setName(displayName).build()
                            context.devicesDataStore.updateData { devices ->
                                val index = devices.devicesList.indexOfFirst { it.address == device.address }
                                devices.toBuilder().setDevices(index, newDevice).build()
                            }

                            pushDynamicShortcut(context, newDevice)
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
            title = {
                Text(stringResource(R.string.rename_device))
            },
            text = {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.focusRequester(focusRequester),
                    placeholder = {
                        Text(text = device.originalName)
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
        )
    }

    ListItem(
        headlineContent = {
            Text(
                text = device.name,
                style = MaterialTheme.typography.headlineMedium
            )
        },
        supportingContent = {
            if (device.time > 0) {
                Text(text = stringResource(R.string.position_saved, timeAgo(device.time)))
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
            ) {
                DropdownMenuItem(
                    text = {
                        Text(stringResource(R.string.rename_device))
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.drive_file_rename),
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
                                painter = painterResource(id = R.drawable.app_shortcut),
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false

                            val shortcut = buildShortcut(context, device)
                            val shortcutResultIntent = ShortcutManagerCompat.createShortcutResultIntent(context, shortcut)
                            val successCallback = PendingIntent.getBroadcast(context, 0, shortcutResultIntent, PendingIntent.FLAG_IMMUTABLE)

                            ShortcutManagerCompat.requestPinShortcut(context, shortcut, successCallback.intentSender)
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
                        coroutineScope.launch {
                            context.devicesDataStore.updateData { devices ->
                                val index = devices.devicesList.indexOfFirst { it.address == device.address }
                                devices.toBuilder().removeDevices(index).build()
                            }
                        }
                    }
                )
            }
        }
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
                                    context.devicesDataStore.updateData { devices ->
                                        devices.toBuilder().addDevices(
                                            Device.newBuilder()
                                                .setAddress(it.address)
                                                .setOriginalName(it.name)
                                                .setName(it.name)
                                        ).build()
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

    OutlinedButton(
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

@Composable
fun Onboarding() {
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
                                        context.configsDataStore.updateData { configs ->
                                            configs.toBuilder().setOnboardingCompleted(true).build()
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
                color = DarkGreen
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
            imageVector = Icons.Filled.LocationOn,
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
                    color = DarkGreen
                )
            }
        }
    }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun getConnectedBluetoothDevices(context: Context): List<SavedDevice> {
    return context.getSystemService<BluetoothManager>()?.adapter?.bondedDevices?.filterNotNull().orEmpty().map {
        SavedDevice(it.name, it.address, IsConnected.invoke(it) as Boolean)
    }
}

val IsConnected: Method = BluetoothDevice::class.java.getMethod("isConnected")

data class SavedDevice(
    val name: String,
    val address: String,
    val connected: Boolean
)

fun locationIntent(latitude: Double, longitude: Double) =
    Intent(
        Intent.ACTION_VIEW,
        "https://www.google.com/maps/search/?api=1&query=${latitude}%2C${longitude}".toUri()
    )

fun buildShortcut(context: Context, device: Device) =
    ShortcutInfoCompat.Builder(context, "navigate_to_${device.address}")
        .setShortLabel(context.getString(R.string.shortcut_short_description, device.name))
        .setLongLabel(context.getString(R.string.shortcut_long_description, device.name))
        .setIcon(IconCompat.createWithResource(context, R.drawable.location_pin))
        .addCapabilityBinding(
            "actions.intent.OPEN_APP_FEATURE",
            "feature",
            context.resources.getStringArray(R.array.shortcut_feature_name).asList()
        )
        .setIntent(locationIntent(device.latitude, device.longitude))
        .build()

fun pushDynamicShortcut(context: Context, device: Device) {
    val shortcut = buildShortcut(context, device)
    ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
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
            imageVector = Icons.Filled.LocationOn,
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

@Composable
fun LocationMap(modifier: Modifier = Modifier, device: Device) {
    val context = LocalContext.current

    val coordinates = LatLng(device.latitude, device.longitude)
    val cameraPosition = CameraPosition.fromLatLngZoom(coordinates, 16f)

    val cameraPositionState = rememberCameraPositionState {
        position = cameraPosition
    }

    LaunchedEffect(cameraPosition) {
        cameraPositionState.animate(CameraUpdateFactory.newCameraPosition(cameraPosition))
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
            rotationGesturesEnabled = false,
            scrollGesturesEnabled = false,
            scrollGesturesEnabledDuringRotateOrZoom = false,
            tiltGesturesEnabled = false,
            zoomControlsEnabled = false,
            zoomGesturesEnabled = false
        ),
        onMapClick = {
            context.startActivity(locationIntent(device.latitude, device.longitude))
        },
        mapColorScheme = ComposeMapColorScheme.FOLLOW_SYSTEM
    ) {
        val markerState = rememberUpdatedMarkerState(position = coordinates)
        Marker(state = markerState)
    }
}
