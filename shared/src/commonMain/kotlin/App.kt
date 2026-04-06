import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import screens.LoginScreen
import screens.ProfileScreen

@Composable
fun App() {
    MaterialTheme {
        val initialScreen = if (AppSettings.sessionCookies != null) ProfileScreen() else LoginScreen()
        Navigator(initialScreen)
    }
}

expect fun getPlatformName(): String