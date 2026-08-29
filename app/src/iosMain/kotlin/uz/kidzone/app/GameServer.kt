package uz.kidzone.app

actual object GameServer {
    actual fun start() {
        // iOS uchun Ktor yoki GCDWebServer (Swift) qismi bu yerda ishga tushadi
        // Hozircha bo'sh qoldiramiz, chunki Xcode tomonida Native ulanish qilinishi kerak.
    }

    actual fun getGameUrl(gameId: String): String {
        // iOS WKWebView odatda bundle resource larni to'g'ridan-to'g'ri (local file URL) ochadi
        // Lekin Ktor ulasak "http://localhost:8080/$gameId.html" bo'ladi.
        return "http://localhost:8080/$gameId.html"
    }
}
