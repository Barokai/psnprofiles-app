package screens

import UpdateProfileWebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator

class UpdateProfileScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current

        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Update Profile") },
                navigationIcon = {
                    IconButton(onClick = { navigator?.pop() }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )

            Box(Modifier.fillMaxSize()) {
                UpdateProfileWebView(onFinished = {
                    navigator?.pop()
                })
            }
        }
    }
}
