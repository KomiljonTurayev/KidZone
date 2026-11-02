package uz.kidzone.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class PinUtilTest {

    private SharedPreferences prefs;

    @Before
    public void setUp() {
        prefs = RuntimeEnvironment.getApplication()
                .getSharedPreferences("test_prefs", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
    }

    // Legacy format: unsalted SHA-256("1234") as hex, as stored by app versions <= 1.6.1.
    private static final String LEGACY_1234 =
            "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4";

    @Test
    public void hash_isVersionedAndFitsFirestoreLimit() {
        String h = PinUtil.hash("1234");
        assertTrue(h.startsWith("v2$"));
        // firestore.rules caps profiles.pinHash at 64 chars.
        assertTrue(h.length() <= 64);
    }

    @Test
    public void hash_isSalted() {
        assertNotEquals(PinUtil.hash("1234"), PinUtil.hash("1234"));
    }

    @Test
    public void hash_differentInputsDifferentHashes() {
        assertNotEquals(PinUtil.hash("1234"), PinUtil.hash("4321"));
    }

    @Test
    public void matches_legacyHash_stillVerifies() {
        assertTrue(PinUtil.matches("1234", LEGACY_1234));
        assertFalse(PinUtil.matches("4321", LEGACY_1234));
    }

    @Test
    public void matches_malformedHash_false() {
        assertFalse(PinUtil.matches("1234", "v2$not-base64!"));
        assertFalse(PinUtil.matches("1234", "v2$@@$@@"));
    }

    @Test
    public void needsRehash_onlyForLegacy() {
        assertTrue(PinUtil.needsRehash(LEGACY_1234));
        assertFalse(PinUtil.needsRehash(PinUtil.hash("1234")));
        assertFalse(PinUtil.needsRehash(null));
        assertFalse(PinUtil.needsRehash(""));
    }

    @Test
    public void matches_correctPin_true() {
        String hash = PinUtil.hash("1234");
        assertTrue(PinUtil.matches("1234", hash));
    }

    @Test
    public void matches_wrongPin_false() {
        String hash = PinUtil.hash("1234");
        assertFalse(PinUtil.matches("4321", hash));
    }

    @Test
    public void matches_nullHash_false() {
        assertFalse(PinUtil.matches("1234", null));
    }

    @Test
    public void isPlainPin_fourDigits_true() {
        assertTrue(PinUtil.isPlainPin("1234"));
    }

    @Test
    public void isPlainPin_hash_false() {
        assertFalse(PinUtil.isPlainPin(PinUtil.hash("1234")));
    }

    @Test
    public void isPlainPin_null_false() {
        assertFalse(PinUtil.isPlainPin(null));
    }

    @Test
    public void isPlainPin_empty_false() {
        assertFalse(PinUtil.isPlainPin(""));
    }

    @Test
    public void getOrMigrateHash_noStoredValue_returnsNull() {
        assertNull(PinUtil.getOrMigrateHash(prefs, "kz_pin"));
    }

    @Test
    public void getOrMigrateHash_plainPin_migratesAndReturnsHash() {
        prefs.edit().putString("kz_pin", "1234").commit();
        String hash = PinUtil.getOrMigrateHash(prefs, "kz_pin");
        assertTrue(PinUtil.matches("1234", hash));
        assertEquals(hash, prefs.getString("kz_pin", null));
    }

    @Test
    public void getOrMigrateHash_alreadyHashed_unchanged() {
        String hash = PinUtil.hash("1234");
        prefs.edit().putString("kz_pin", hash).commit();
        assertEquals(hash, PinUtil.getOrMigrateHash(prefs, "kz_pin"));
        assertEquals(hash, prefs.getString("kz_pin", null));
    }

    @Test
    public void getOrMigrateHash_emptyValue_returnsEmpty() {
        prefs.edit().putString("kz_pin", "").commit();
        assertEquals("", PinUtil.getOrMigrateHash(prefs, "kz_pin"));
    }
}
