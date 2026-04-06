import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.CookieManager
import android.annotation.SuppressLint
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun LoginWebView(onCookiesReceived: (String) -> Unit) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.parseColor("#1a1a1a"))
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                NetworkClient.userAgent = settings.userAgentString
                
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        view.setBackgroundColor(android.graphics.Color.parseColor("#1a1a1a"))
                        if (url.trimEnd('/') == "https://psnprofiles.com" || url == "https://psnprofiles.com/") {
                            // After success, it redirects to the front page
                            view.visibility = android.view.View.INVISIBLE
                        }
                    }
                
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    
                    view.evaluateJavascript("""
                        (function() {
                            var buttons = document.querySelectorAll('button, a, input[type="submit"], input[type="button"], div.button');
                            var targetBtn = null;
                            for (var i = 0; i < buttons.length; i++) {
                                var text = (buttons[i].innerText || buttons[i].value || '').trim();
                                if (text.toLowerCase().includes('continue as ')) {
                                    targetBtn = buttons[i];
                                    break;
                                }
                            }
                            
                            if (targetBtn) {
                                targetBtn.style.transition = "all 0.3s ease";
                                targetBtn.style.backgroundColor = "#ff9800"; // Visual indicator
                                var countdown = 3;
                                function updateText() {
                                    if (countdown > 0) {
                                        var newStr = "Auto Login in " + countdown + "...";
                                        if (targetBtn.tagName.toLowerCase() === 'input') {
                                            targetBtn.value = newStr;
                                        } else {
                                            targetBtn.innerText = newStr;
                                        }
                                        countdown--;
                                        setTimeout(updateText, 1000);
                                    } else {
                                        targetBtn.click();
                                    }
                                }
                                updateText();
                            }
                        })();
                    """.trimIndent(), null)

                    val cookies = CookieManager.getInstance().getCookie(url)
                    // Wait for the session token after successful login.
                    if (cookies != null && cookies.contains("psnprofiles_session")) {
                        onCookiesReceived(cookies)
                    }
                }
            }
            loadUrl("https://psnprofiles.com/login")
        }
    })
}
