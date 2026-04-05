import androidx.compose.runtime.Composable
import androidx.compose.material.Text

@Composable
actual fun LoginWebView(onCookiesReceived: (String) -> Unit) {
    // iOS implementation pending. We can show a placeholder.
    Text("Login on iOS is not fully implemented yet.")
}
