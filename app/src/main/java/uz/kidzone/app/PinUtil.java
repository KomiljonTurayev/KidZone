package uz.kidzone.app;

import android.content.SharedPreferences;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PinUtil {

    private PinUtil() {}

    public static String hash(String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static boolean matches(String pin, String storedHash) {
        return storedHash != null && storedHash.equals(hash(pin));
    }

    public static boolean isPlainPin(String stored) {
        if (stored == null || stored.length() != 4) return false;
        for (int i = 0; i < stored.length(); i++) {
            if (!Character.isDigit(stored.charAt(i))) return false;
        }
        return true;
    }

    public static String getOrMigrateHash(SharedPreferences prefs, String key) {
        String stored = prefs.getString(key, null);
        if (stored == null || stored.isEmpty()) return stored;
        if (isPlainPin(stored)) {
            String hashed = hash(stored);
            prefs.edit().putString(key, hashed).apply();
            return hashed;
        }
        return stored;
    }
}
