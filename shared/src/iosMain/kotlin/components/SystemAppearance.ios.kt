package components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
actual fun SystemAppearance(statusBarColor: Color) {
    // iOS handling for status bar is usually different (UIVisualEffectView or similar)
    // No-op for now as requested fix is primarily for Android visual parity.
}
