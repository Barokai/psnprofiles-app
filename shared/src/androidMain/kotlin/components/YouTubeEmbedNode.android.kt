package components

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun YouTubeEmbedNode(videoId: String) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                webChromeClient = WebChromeClient() // Required for inline video rendering natively
                webViewClient = WebViewClient()
                
                // Embed URL with auto-sizing
                val htmlData = "<!DOCTYPE html><html><body style='margin:0;padding:0;'><iframe width='100%' height='100%' src='https://www.youtube.com/embed/$videoId' frameborder='0' allowfullscreen></iframe></body></html>"
                loadDataWithBaseURL("https://www.youtube.com", htmlData, "text/html", "UTF-8", null)
            }
        },
        modifier = Modifier.fillMaxWidth().height(220.dp)
    )
}
