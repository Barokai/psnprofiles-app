import com.russhwolf.settings.Settings

object AppSettings {
    val settings: Settings by lazy { Settings() }
    
    var hideEarnedTrophies: Boolean
        get() = settings.getBoolean("hide_earned_trophies", false)
        set(value) = settings.putBoolean("hide_earned_trophies", value)

    var sessionCookies: String?
        get() = settings.getStringOrNull("session_cookies")
        set(value) {
            if (value != null) settings.putString("session_cookies", value)
            else settings.remove("session_cookies")
        }
}
