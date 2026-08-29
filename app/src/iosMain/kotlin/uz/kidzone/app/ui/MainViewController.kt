package uz.kidzone.app.ui

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

// iOS (Swift) loyihasi aynan shu funksiyani chaqirib, butun Android/KMP UI'ni Apple ekraniga chizadi
fun MainViewController(): UIViewController = ComposeUIViewController {
    KidZoneApp(
        prefs = uz.kidzone.app.arch.AppPreferences() // iOS Native xotira
    )
}
