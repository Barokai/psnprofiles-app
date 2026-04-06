import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import android.annotation.SuppressLint
import android.webkit.CookieManager
import NetworkClient

class WebAppInterface(private val onFinished: () -> Unit) {
    @android.webkit.JavascriptInterface
    fun success() {
        onFinished()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun UpdateProfileWebView(onFinished: () -> Unit) {
    AndroidView(factory = { context ->
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            
            // Register bridge to receive AJAX update success messages
            addJavascriptInterface(WebAppInterface {
                post { onFinished() }
            }, "AndroidBridge")
            
            // Sync current in-memory Ktor cookies to CookieManager, though usually they already exist from LoginWebView
            NetworkClient.globalCookies?.let {
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                // Split multi-cookies if necessary, but typically the standard setCookie is domain-based natively.
                cookieManager.setCookie("https://psnprofiles.com", it)
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    
                    val js = """
                        (function() {
                            var viewport = document.querySelector('meta[name=viewport]');
                            if(!viewport) {
                                viewport = document.createElement('meta');
                                viewport.name = 'viewport';
                                document.head.appendChild(viewport);
                            }
                            viewport.content = 'width=device-width, initial-scale=1.0';

                            // The update happens via AJAX, observe the screen for the success growl notification
                            setInterval(function() {
                                if (document.body.innerText.indexOf('has been updated') !== -1 || document.body.innerText.indexOf('Updated') !== -1) {
                                    window.AndroidBridge && window.AndroidBridge.success();
                                }
                            }, 500);

                            // Auto-center the dropdown if it is present
                            var dropdown = document.querySelector('.dropdown-menu');
                            if (dropdown) {
                                dropdown.style.display = 'block';
                                dropdown.style.position = 'fixed';
                                dropdown.style.top = '20%';
                                dropdown.style.left = '50%';
                                dropdown.style.transform = 'translate(-50%, 0)';
                                dropdown.style.zIndex = '9999';
                                dropdown.style.background = '#1a1a1a';
                                dropdown.style.padding = '16px';
                                dropdown.style.borderRadius = '8px';
                            }
                            
                            var growls = document.getElementById('growls');
                            if (growls) {
                                growls.style.position = 'fixed';
                                growls.style.top = '30%';
                                growls.style.left = '50%';
                                growls.style.transform = 'translate(-50%, 0)';
                                growls.style.zIndex = '99999';
                            }
                        })();
                    """.trimIndent()
                    
                    view.evaluateJavascript(js, null)
                    
                    if (!url.contains("update") && !url.contains("recaptcha") && url.length > 25 && !url.endsWith("/")) {
                        // Very likely that we bounced back to user profile successfully via non-AJAX redirect
                        onFinished()
                    }
                }
            }
            loadUrl("https://psnprofiles.com/?update")
        }
    })
}
