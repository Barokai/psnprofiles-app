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

class LoginScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        var sessionCookies by remember { mutableStateOf<String?>(null) }
        
        if (sessionCookies == null) {
            LoginWebView(onCookiesReceived = { cookies ->
                sessionCookies = cookies
                NetworkClient.globalCookies = cookies
                navigator?.replace(ProfileScreen())
            })
        }
    }
}
