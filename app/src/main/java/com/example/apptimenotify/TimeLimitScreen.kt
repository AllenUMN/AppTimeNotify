package com.example.apptimenotify

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun TimeLimitScreen(
    packageName: String,
    appName: String,
    onConfirmed: () -> Unit,
    onBack: () -> Unit
) {
    var hours by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Set limit for $appName",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = hours,
                onValueChange = { if (it.length <= 2) hours = it.filter { char -> char.isDigit() } },
                label = { Text("Hours") },
                modifier = Modifier.weight(1f).testTag("hours_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = minutes,
                onValueChange = { if (it.length <= 2) minutes = it.filter { char -> char.isDigit() } },
                label = { Text("Minutes") },
                modifier = Modifier.weight(1f).testTag("minutes_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val h = hours.toIntOrNull() ?: 0
                val m = minutes.toIntOrNull() ?: 0
                if (h > 0 || m > 0) {
                    scope.launch {
                        UsagePrefs.saveLimit(context, packageName, appName, h, m)
                        onConfirmed()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("confirm_button")
        ) {
            Text("Confirm")
        }

        TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
            Text("Back")
        }
    }
}
