package com.umair.smarttodo.ui.home.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.umair.smarttodo.R
import com.umair.smarttodo.ui.theme.NeonPurple

/**
 * Microphone button that dictates into a text field.
 *
 * WHY THE INTENT, NOT THE SpeechRecognizer API. This launches the system speech
 * recognizer through [RecognizerIntent.ACTION_RECOGNIZE_SPEECH] and
 * `StartActivityForResult`, rather than driving `android.speech.SpeechRecognizer`
 * directly. The difference matters: the intent route records inside the system UI, in the
 * system process, so this app needs **no RECORD_AUDIO permission in the manifest** and no
 * runtime permission flow at all. Driving `SpeechRecognizer` ourselves would require
 * holding RECORD_AUDIO, asking for it, handling denial and handling "denied forever" -
 * all of that for a feature that is a convenience, not the point of the app. Do not
 * "upgrade" this to the direct API without also accepting that permission cost.
 *
 * LANGUAGE IS DELIBERATELY NOT FORCED. Only [RecognizerIntent.EXTRA_LANGUAGE_MODEL] is
 * set, to free-form. No EXTRA_LANGUAGE is passed, so the device default applies. The users
 * of this app mix English, Hinglish and Urdulish in the same list; pinning one locale
 * would make the other two worse.
 *
 * KNOWN CAVEAT, LEFT UNSOLVED ON PURPOSE. If the device transcribes Hindi or Urdu speech
 * into Devanagari or Nastaliq script, the offline rule-based categorizer will not match
 * anything - it is Roman-script only by design - and the task lands in OTHER. The LLM
 * enrichment layer still categorizes and translates it correctly once it runs, and the
 * user can always edit the transcription before saving. Transliterating scripts here is
 * out of scope.
 *
 * The button renders nothing at all when the device has no activity that handles the
 * recognizer intent (some ROMs genuinely ship without one), and still catches
 * [ActivityNotFoundException] as a belt-and-braces fallback in case the handler
 * disappears between the check and the tap.
 */
@Composable
internal fun VoiceInputButton(
    onTextRecognized: (String) -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 36.dp,
    iconSize: Dp = 19.dp,
    tint: Color = NeonPurple,
) {
    val context = LocalContext.current
    val available = remember(context) { isSpeechInputAvailable(context) }
    val unavailableMessage = stringResource(R.string.voice_input_unavailable)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
        if (spoken.isNotEmpty()) onTextRecognized(spoken)
    }

    if (!available) return

    IconButton(
        onClick = {
            try {
                launcher.launch(speechRecognitionIntent())
            } catch (notFound: ActivityNotFoundException) {
                Toast.makeText(context, unavailableMessage, Toast.LENGTH_SHORT).show()
            }
        },
        modifier = modifier.size(buttonSize),
    ) {
        Icon(
            imageVector = Icons.Rounded.Mic,
            contentDescription = stringResource(R.string.cd_voice_input),
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * How a dictation result joins text that is already in the field.
 *
 * Append, never replace. Replacing would silently destroy something the user typed by
 * hand, and there is no undo in this app; appending is always recoverable with backspace.
 * It also matches how dictation is actually used here - say one more item, then another -
 * rather than restating the whole task. A blank field is simply replaced wholesale, which
 * is the same thing as appending to nothing.
 */
internal fun appendSpokenText(current: String, spoken: String): String =
    if (current.isBlank()) spoken else current.trimEnd() + " " + spoken

/**
 * Free-form dictation, device default language. See [VoiceInputButton] for why neither of
 * those is negotiable.
 */
private fun speechRecognitionIntent(): Intent =
    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
        )
    }

/**
 * True when something on this device can handle the recognizer intent.
 *
 * Needs the `<queries>` block in AndroidManifest.xml to see past Android 11 package
 * visibility filtering - without it this returns false on every modern device and the mic
 * button would never appear. `<queries>` is a visibility declaration, not a permission.
 */
@Suppress("DEPRECATION")
private fun isSpeechInputAvailable(context: Context): Boolean =
    context.packageManager
        .queryIntentActivities(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0)
        .isNotEmpty()
