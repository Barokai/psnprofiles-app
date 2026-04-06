package screens

import LoginWebView
import NetworkClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material.*

class LoginScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        var sessionCookies by remember { mutableStateOf<String?>(null) }
        
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF1A1A1A)
        ) {
            if (sessionCookies == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colors.primary
                    )
                    LoginWebView(onCookiesReceived = { cookies ->
                        sessionCookies = cookies
                        NetworkClient.globalCookies = cookies
                        AppSettings.sessionCookies = cookies
                        navigator?.replace(ProfileScreen())
                    })
                }
            }
        }
    }
}
