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
        mgr = new ParentalStatsManager(prefs, "default");
    }

    @Test
    public void getTodayMinutes_noData_returnsZero() {
        assertEquals(0, mgr.getTodayMinutes());
    }

    @Test
    public void getTodayMinutes_savedData_returnsSaved() {
        prefs.edit().putInt(ParentalStatsManager.todayPtKey("default"), 20).commit();
        assertEquals(20, mgr.getTodayMinutes());
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
        prefs.edit().putInt(ParentalStatsManager.todayPtKey("default"), 45).commit();
        assertEquals(45, mgr.getWeeklyMinutes()[6]);
    }

    @Test
    public void onSessionEnd_calledBeforeStart_doesNothing() {
        mgr.onSessionEnd();
        assertEquals(0, mgr.getTodayMinutes());
    }

    // --- Session tracking ---

    @Test
    public void getSessionGames_initiallyEmpty() {
        assertTrue(mgr.getSessionGames().isEmpty());
    }

    @Test
    public void getSessionGames_afterOnGameLaunched_containsGame() {
        mgr.onGameLaunched("math_counting");
        assertTrue(mgr.getSessionGames().contains("math_counting"));
    }

    @Test
    public void getSessionGames_deduplicatesWithinSession() {
        mgr.onGameLaunched("math_counting");
        mgr.onGameLaunched("math_counting");
        assertEquals(1, mgr.getSessionGames().size());
    }

    @Test
    public void onSessionStart_clearsSessionGames() {
        mgr.onGameLaunched("math_counting");
        mgr.onSessionStart();
        assertTrue(mgr.getSessionGames().isEmpty());
    }

    @Test
    public void getSessionMinutes_beforeSessionStart_returnsZero() {
        assertEquals(0, mgr.getSessionMinutes());
    }

    @Test
    public void getSessionMinutes_afterSessionStart_subMinuteIsZero() {
        mgr.onSessionStart();
        assertEquals(0, mgr.getSessionMinutes()); // sub-minute elapsed = 0
    }

    // --- getTodaySeconds ---

    @Test
    public void getTodaySeconds_noData_returnsZero() {
        assertEquals(0L, mgr.getTodaySeconds());
    }

    @Test
    public void getTodaySeconds_savedMinutesOnly_convertsToSeconds() {
        prefs.edit().putInt(ParentalStatsManager.todayPtKey("default"), 5).commit();
        assertEquals(300L, mgr.getTodaySeconds());
    }

    @Test
    public void getTodaySeconds_beforeSessionStart_ignoresElapsedTime() {
        assertEquals(0L, mgr.getTodaySeconds());
    }
}
