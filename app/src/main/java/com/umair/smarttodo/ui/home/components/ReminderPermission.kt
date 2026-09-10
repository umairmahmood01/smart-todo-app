package com.umair.smarttodo.ui.home.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * State for the one-time `POST_NOTIFICATIONS` runtime request (API 33+ only). Reminders
 * still get saved when the permission is denied or skipped below API 33 - the data layer
 * simply will not be able to post the notification, which is expected and already
 * handled server-side; this only controls whether we ask and whether we show the small
 * explanatory hint.
 */
@Composable
internal fun rememberReminderPermissionState(): ReminderPermissionState {
    val context = LocalContext.current
    var deniedOnce by remember { mutableStateOf(false) }
    var askedOnce by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        askedOnce = true
        if (!granted) deniedOnce = true
    }

    return remember(context) {
        ReminderPermissionState(
            requestIfNeeded = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted && !askedOnce) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            },
            showDeniedHint = { deniedOnce },
        )
    }
}

internal class ReminderPermissionState(
    val requestIfNeeded: () -> Unit,
    val showDeniedHint: () -> Boolean,
)
