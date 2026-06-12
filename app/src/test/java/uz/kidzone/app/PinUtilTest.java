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

    @Test
    public void hash_returns64CharHex() {
        String h = PinUtil.hash("1234");
        assertEquals(64, h.length());
        assertTrue(h.matches("[0-9a-f]+"));
    }

    @Test
    public void hash_isDeterministic() {
        assertEquals(PinUtil.hash("1234"), PinUtil.hash("1234"));
    }

    @Test
    public void hash_differentInputsDifferentHashes() {
        assertNotEquals(PinUtil.hash("1234"), PinUtil.hash("4321"));
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
        assertEquals(PinUtil.hash("1234"), hash);
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
