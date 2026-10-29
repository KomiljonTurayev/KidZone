package uz.kidzone.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyApiParsingTest {

    @Test
    fun `an owner's family lists its members`() {
        val family = parseFamily(
            """{"ownerUid":"owner","role":"owner","members":[{"uid":"second","joinedAt":"2026-10-29T09:00:00Z"}]}""",
        )

        assertEquals("owner", family.ownerUid)
        assertTrue(family.isOwner)
        assertEquals(listOf(FamilyMemberInfo("second", "2026-10-29T09:00:00Z")), family.members)
    }

    @Test
    fun `a member is not the owner`() {
        val family = parseFamily("""{"ownerUid":"owner","role":"member","members":[]}""")

        assertFalse(family.isOwner)
        assertTrue(family.members.isEmpty())
    }

    @Test
    fun `http errors map to what the parent is told`() {
        assertEquals(FamilyError.NOT_FOUND, errorFor(404))
        assertEquals(FamilyError.NOT_FOUND, errorFor(400))
        assertEquals(FamilyError.FORBIDDEN, errorFor(403))
        assertEquals(FamilyError.TOO_MANY_ATTEMPTS, errorFor(429))
        assertEquals(FamilyError.NETWORK, errorFor(503))
    }
}
