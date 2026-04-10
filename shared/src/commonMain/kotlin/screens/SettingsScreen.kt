package screens

import AppSettings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator

class SettingsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        var hideEarnedTrophies by remember { mutableStateOf(AppSettings.hideEarnedTrophies) }

        Column(Modifier.fillMaxWidth()) {
            TopAppBar(
                title = { Text("PSNP Configure") },
                navigationIcon = {
                    IconButton(onClick = { navigator?.pop() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Hide Earned Trophies (In Guides)", modifier = Modifier.weight(1f))
                Switch(
                    checked = hideEarnedTrophies,
                    onCheckedChange = { newState ->
                        hideEarnedTrophies = newState
                        AppSettings.hideEarnedTrophies = newState
                    }
                )
            }
        }
    }
}
