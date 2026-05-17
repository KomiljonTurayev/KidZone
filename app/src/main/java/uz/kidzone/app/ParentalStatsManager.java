package uz.kidzone.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ParentalStatsManager {

    private static final String KEY_TIME_LIMIT = "kz_time_limit";
    private final SharedPreferences prefs;
    private long sessionStartMs = 0;

    public ParentalStatsManager(Context ctx) {
        this(ctx.getSharedPreferences("kz_prefs", Context.MODE_PRIVATE));
    }

    ParentalStatsManager(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public void onSessionStart() {
        sessionStartMs = System.currentTimeMillis();
    }

    public void onSessionEnd() {
        if (sessionStartMs == 0) return;
        int elapsed = (int) ((System.currentTimeMillis() - sessionStartMs) / 60_000L);
        sessionStartMs = 0;
        if (elapsed <= 0) return;
        String key = todayPtKey();
        prefs.edit().putInt(key, prefs.getInt(key, 0) + elapsed).apply();
    }

    public void onGameLaunched(String gameId) {
        if (gameId == null || gameId.isEmpty()) return;
        String key = todayGlKey();
        String existing = prefs.getString(key, "");
        List<String> list = parseList(existing);
        if (!list.contains(gameId)) {
            list.add(gameId);
            prefs.edit().putString(key, joinList(list)).apply();
        }
    }

    public int getTodayMinutes() {
        int saved = prefs.getInt(todayPtKey(), 0);
        int current = sessionStartMs > 0
                ? (int) ((System.currentTimeMillis() - sessionStartMs) / 60_000L) : 0;
        return saved + current;
    }

    public int[] getWeeklyMinutes() {
        int[] result = new int[7];
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US);
        Calendar cal = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            String key = "kz_pt_" + sdf.format(cal.getTime());
            result[i] = prefs.getInt(key, 0);
            if (i == 6 && sessionStartMs > 0)
                result[i] += (int) ((System.currentTimeMillis() - sessionStartMs) / 60_000L);
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        return result;
    }

    public List<String> getTodayGames() {
        return parseList(prefs.getString(todayGlKey(), ""));
    }

    public int getTimeLimitMinutes() {
        return prefs.getInt(KEY_TIME_LIMIT, 0);
    }

    public void setTimeLimitMinutes(int minutes) {
        prefs.edit().putInt(KEY_TIME_LIMIT, Math.max(0, minutes)).apply();
    }

    public boolean isTimeLimitReached() {
        int limit = getTimeLimitMinutes();
        return limit > 0 && getTodayMinutes() >= limit;
    }

    private static String todayPtKey() {
        return "kz_pt_" + new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
    }

    private static String todayGlKey() {
        return "kz_gl_" + new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
    }

    private static List<String> parseList(String csv) {
        if (csv == null || csv.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(csv.split(",")));
    }

    private static String joinList(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(list.get(i));
        }
        return sb.toString();
    }
}
