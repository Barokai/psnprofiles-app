import io.ktor.client.HttpClient
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders

object NetworkClient {
    var globalCookies: String? = AppSettings.sessionCookies
    var userAgent: String? = null

    val client: HttpClient by lazy {
        HttpClient().apply {
            requestPipeline.intercept(HttpRequestPipeline.State) {
                globalCookies?.let { cookies ->
                    context.header(HttpHeaders.Cookie, cookies)
                }
                userAgent?.let { ua ->
                    context.header(HttpHeaders.UserAgent, ua)
                }
            }
        }
    }
}
