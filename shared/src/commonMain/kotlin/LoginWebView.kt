import androidx.compose.runtime.Composable

@Composable
expect fun LoginWebView(onCookiesReceived: (String) -> Unit)
