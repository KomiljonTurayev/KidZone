package uz.kidzone.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import uz.kidzone.app.agegate.AgeGateRepository
import java.io.File

/**
 * Erases everything the app stored on this device after the account was deleted, except the
 * neutral age screen's answer: that one is given once (Families Policy). The files are deleted at
 * the next process start, before any SDK or the WebView has them open.
 */
object DeviceWipe {

    private const val MARKER = "wipe_pending"

    /** Marks the data for deletion and restarts the app. */
    fun wipeAndRestart(activity: Activity) {
        request(activity)
        activity.packageManager.getLaunchIntentForPackage(activity.packageName)?.component?.let {
            activity.startActivity(Intent.makeRestartActivityTask(it))
        }
        Runtime.getRuntime().exit(0)
    }

    internal fun request(context: Context) {
        check(marker(context).createNewFile() || marker(context).exists())
    }

    fun runIfPending(context: Context) {
        val marker = marker(context)
        if (!marker.exists()) return
        val keptPrefs = "${AgeGateRepository.PREFS_NAME}.xml"
        context.dataDir.listFiles().orEmpty().forEach { dir ->
            when (dir.name) {
                "lib", "code_cache" -> Unit
                "shared_prefs" -> dir.listFiles().orEmpty().filter { it.name != keptPrefs }.forEach { it.delete() }
                "no_backup" -> dir.listFiles().orEmpty().filter { it != marker }.forEach { it.deleteRecursively() }
                else -> dir.deleteRecursively()
            }
        }
        // Last, so a wipe that was cut short runs again.
        marker.delete()
    }

    private fun marker(context: Context) = File(context.noBackupFilesDir, MARKER)
}
