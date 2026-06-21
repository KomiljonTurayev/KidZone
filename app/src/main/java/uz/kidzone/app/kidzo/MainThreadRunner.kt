package uz.kidzone.app.kidzo

fun interface MainThreadRunner {
    fun run(action: Runnable)
}
