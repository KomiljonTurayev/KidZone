package uz.kidzone.app

import android.content.Context
import io.ktor.server.engine.embeddedServer
import io.ktor.server.cio.CIO
import io.ktor.server.routing.routing
import io.ktor.server.routing.get
import io.ktor.server.response.respondBytes
import io.ktor.http.HttpStatusCode
import io.ktor.http.ContentType
import io.ktor.server.response.respond
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

actual object GameServer {
    private var isStarted = false
    private const val PORT = 8080
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    actual fun start() {
        if (isStarted) return
        isStarted = true

        CoroutineScope(Dispatchers.IO).launch {
            embeddedServer(CIO, port = PORT) {
                routing {
                    get("/{path...}") {
                        val path = call.parameters.getAll("path")?.joinToString("/") ?: "index.html"
                        try {
                            val context = appContext ?: throw IllegalStateException("Context not initialized")
                            val bytes = context.assets.open("www/$path").use { it.readBytes() }
                            
                            val contentType = when {
                                path.endsWith(".html") -> ContentType.Text.Html
                                path.endsWith(".css") -> ContentType.Text.CSS
                                path.endsWith(".js") -> ContentType.Application.JavaScript
                                path.endsWith(".png") -> ContentType.Image.PNG
                                path.endsWith(".svg") -> ContentType.Image.SVG
                                else -> ContentType.Application.OctetStream
                            }
                            
                            call.respondBytes(bytes, contentType)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                }
            }.start(wait = false)
        }
    }

    actual fun getGameUrl(gameId: String): String {
        return "http://localhost:$PORT/$gameId.html"
    }
}
