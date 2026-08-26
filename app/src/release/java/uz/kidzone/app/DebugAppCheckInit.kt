package uz.kidzone.app

/**
 * Release counterpart of the debug-only App Check provider installer — a
 * no-op. Release builds don't yet have a production App Check provider
 * (Play Integrity) wired up; see the phase-2 plan's "Out of scope" note.
 */
object DebugAppCheckInit {
    fun install() {
        // Intentionally empty.
    }
}
