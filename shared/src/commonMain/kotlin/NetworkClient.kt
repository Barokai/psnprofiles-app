import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders

object NetworkClient {
    var globalCookies: String? = null
    var userAgent: String? = null

    val client: HttpClient by lazy {
        HttpClient {
            install(DefaultRequest) {
                globalCookies?.let { cookies ->
                    header(HttpHeaders.Cookie, cookies)
                }
                userAgent?.let { ua ->
                    header(HttpHeaders.UserAgent, ua)
                }
            }
        }
    }
}
