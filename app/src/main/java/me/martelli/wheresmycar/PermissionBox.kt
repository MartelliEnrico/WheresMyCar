package me.martelli.wheresmycar

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@Composable
fun PermissionBox(
    modifier: Modifier = Modifier,
    permission: String,
    rationale: String,
    contentAlignment: Alignment = Alignment.TopStart,
    onGranted: @Composable BoxScope.() -> Unit,
) {
    PermissionBox(
        modifier,
        permissions = listOf(permission),
        requiredPermissions = listOf(permission),
        rationale,
        contentAlignment,
    ) { onGranted() }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionBox(
    modifier: Modifier = Modifier,
    permissions: List<String>,
    requiredPermissions: List<String> = permissions,
    rationale: String,
    contentAlignment: Alignment = Alignment.TopStart,
    onGranted: @Composable BoxScope.(List<String>) -> Unit,
) {
    val permissionState = rememberMultiplePermissionsState(permissions = permissions)
    val allRequiredPermissionsGranted =
        permissionState.revokedPermissions.none { it.permission in requiredPermissions }

    Box(
        modifier = modifier,
        contentAlignment = contentAlignment,
    ) {
        if (allRequiredPermissionsGranted) {
            onGranted(
                permissionState.permissions
                    .filter { it.status.isGranted }
                    .map { it.permission },
            )
        } else {
            var showRationale by remember(permissionState) {
                mutableStateOf(false)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Button(
                    onClick = {
                        if (permissionState.shouldShowRationale) {
                            showRationale = true
                        } else {
                            permissionState.launchMultiplePermissionRequest()
                        }
                    },
                ) {
                    Text(text = stringResource(id = R.string.grant_permissions))
                }
            }

            if (showRationale) {
                AlertDialog(
                    onDismissRequest = {
                        showRationale = false
                    },
                    title = {
                        Text(text = stringResource(id = R.string.permissions_title))
                    },
                    text = {
                        Text(text = rationale)
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showRationale = false
                                permissionState.launchMultiplePermissionRequest()
                            },
                        ) {
                            Text(text = stringResource(id = R.string.continue_button))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showRationale = false
                            },
                        ) {
                            Text(text = stringResource(id = R.string.dismiss_button))
                        }
                    },
                )
            }
        }
    }
}
