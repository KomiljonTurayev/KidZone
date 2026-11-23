package uz.kidzone.app

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DeviceWipeTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    private fun file(path: String) = File(context.dataDir, path).apply {
        parentFile!!.mkdirs()
        writeText("x")
    }

    @Test
    fun `a requested wipe erases everything but the age answer, once`() {
        val age = file("shared_prefs/age_gate.xml")
        val gone = listOf("shared_prefs/kz_prefs.xml", "databases/kidzone.db", "files/profiles/a.jpg", "app_webview/Default/x")
            .map(::file)

        DeviceWipe.runIfPending(context)
        assertTrue(gone.all { it.exists() })

        DeviceWipe.request(context)
        DeviceWipe.runIfPending(context)
        assertTrue(age.exists())
        assertFalse(gone.any { it.exists() })

        val later = file("shared_prefs/kz_prefs.xml")
        DeviceWipe.runIfPending(context)
        assertTrue(later.exists())
    }
}
