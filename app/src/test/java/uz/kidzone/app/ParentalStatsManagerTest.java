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
public class ParentalStatsManagerTest {

    private SharedPreferences prefs;
    private ParentalStatsManager mgr;

    @Before
    public void setUp() {
        prefs = RuntimeEnvironment.getApplication()
                .getSharedPreferences("test_prefs", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        mgr = new ParentalStatsManager(prefs);
    }

    @Test
    public void getTodayMinutes_noData_returnsZero() {
        assertEquals(0, mgr.getTodayMinutes());
    }

    @Test
    public void getTodayMinutes_savedData_returnsSaved() {
        prefs.edit().putInt(todayKey(), 20).commit();
        assertEquals(20, mgr.getTodayMinutes());
    }

    @Test
    public void isTimeLimitReached_limitZero_alwaysFalse() {
        mgr.setTimeLimitMinutes(0);
        prefs.edit().putInt(todayKey(), 999).commit();
        assertFalse(mgr.isTimeLimitReached());
    }

    @Test
    public void isTimeLimitReached_belowLimit_false() {
        mgr.setTimeLimitMinutes(30);
        prefs.edit().putInt(todayKey(), 29).commit();
        assertFalse(mgr.isTimeLimitReached());
    }

    @Test
    public void isTimeLimitReached_atLimit_true() {
        mgr.setTimeLimitMinutes(30);
        prefs.edit().putInt(todayKey(), 30).commit();
        assertTrue(mgr.isTimeLimitReached());
    }

    @Test
    public void onGameLaunched_noDuplicatesWithinDay() {
        mgr.onGameLaunched("story-001");
        mgr.onGameLaunched("story-001");
        assertEquals(1, mgr.getTodayGames().size());
    }

    @Test
    public void onGameLaunched_multipleDistinct_countsAll() {
        mgr.onGameLaunched("story-001");
        mgr.onGameLaunched("song-001");
        assertEquals(2, mgr.getTodayGames().size());
    }

    @Test
    public void getWeeklyMinutes_returns7Elements() {
        assertEquals(7, mgr.getWeeklyMinutes().length);
    }

    @Test
    public void getWeeklyMinutes_todayAtIndex6() {
        prefs.edit().putInt(todayKey(), 45).commit();
        assertEquals(45, mgr.getWeeklyMinutes()[6]);
    }

    @Test
    public void setTimeLimitMinutes_negativeClampedToZero() {
        mgr.setTimeLimitMinutes(-10);
        assertEquals(0, mgr.getTimeLimitMinutes());
    }

    @Test
    public void onSessionEnd_calledBeforeStart_doesNothing() {
        mgr.onSessionEnd();
        assertEquals(0, mgr.getTodayMinutes());
    }

    private static String todayKey() {
        return "kz_pt_" + new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
                .format(new java.util.Date());
    }
}
