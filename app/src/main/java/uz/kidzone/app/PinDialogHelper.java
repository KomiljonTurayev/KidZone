package uz.kidzone.app;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class PinDialogHelper {

    public interface OnPinResult {
        void onResult(String pin); // 4-digit string, or "" if skipped
    }

    public interface OnPinVerified {
        void onVerified();
    }

    /** Show "Create PIN" flow: enter, confirm, skip option. Calls cb.onResult(pin) or cb.onResult("") on skip. */
    public static void showCreate(Context ctx, OnPinResult cb) {
        View view = LayoutInflater.from(ctx).inflate(R.layout.dialog_pin, null);
        TextView tvTitle = view.findViewById(R.id.pin_title);
        tvTitle.setText("Create a parent PIN");

        final StringBuilder entered = new StringBuilder();
        final String[] firstPin = {null};

        AlertDialog dialog = new MaterialAlertDialogBuilder(ctx)
                .setView(view).setCancelable(false).create();

        wireKeypad(view, entered, () -> {
            String pin = entered.toString();
            if (firstPin[0] == null) {
                firstPin[0] = pin;
                entered.setLength(0);
                updateDots(view, 0);
                tvTitle.setText("Confirm PIN");
            } else if (pin.equals(firstPin[0])) {
                dialog.dismiss();
                cb.onResult(pin);
            } else {
                shakeDots(view);
                entered.setLength(0);
                firstPin[0] = null;
                updateDots(view, 0);
                tvTitle.setText("Create a parent PIN");
            }
        });

        view.findViewById(R.id.pin_skip).setVisibility(View.VISIBLE);
        view.findViewById(R.id.pin_skip).setOnClickListener(v -> {
            dialog.dismiss();
            cb.onResult("");
        });

        dialog.show();
    }

    /** Show "Enter PIN" flow — no skip button. Calls cb.onVerified() on correct PIN. */
    public static void showEnter(Context ctx, String expectedPin, OnPinVerified cb) {
        View view = LayoutInflater.from(ctx).inflate(R.layout.dialog_pin, null);
        TextView tvTitle = view.findViewById(R.id.pin_title);
        tvTitle.setText("Enter parent PIN");
        view.findViewById(R.id.pin_skip).setVisibility(View.GONE);

        final StringBuilder entered = new StringBuilder();

        AlertDialog dialog = new MaterialAlertDialogBuilder(ctx)
                .setView(view).setCancelable(true).create();

        wireKeypad(view, entered, () -> {
            if (entered.toString().equals(expectedPin)) {
                dialog.dismiss();
                cb.onVerified();
            } else {
                shakeDots(view);
                entered.setLength(0);
                updateDots(view, 0);
            }
        });

        dialog.show();
    }

    private static void wireKeypad(View view, StringBuilder entered, Runnable onFourDigits) {
        int[] keyIds = {R.id.pin_key_1, R.id.pin_key_2, R.id.pin_key_3,
                        R.id.pin_key_4, R.id.pin_key_5, R.id.pin_key_6,
                        R.id.pin_key_7, R.id.pin_key_8, R.id.pin_key_9,
                        R.id.pin_key_0};
        String[] digits = {"1","2","3","4","5","6","7","8","9","0"};

        for (int i = 0; i < keyIds.length; i++) {
            final String d = digits[i];
            view.findViewById(keyIds[i]).setOnClickListener(v -> {
                if (entered.length() >= 4) return;
                entered.append(d);
                updateDots(view, entered.length());
                if (entered.length() == 4) onFourDigits.run();
            });
        }

        view.findViewById(R.id.pin_backspace).setOnClickListener(v -> {
            if (entered.length() > 0) {
                entered.deleteCharAt(entered.length() - 1);
                updateDots(view, entered.length());
            }
        });
    }

    static void updateDots(View root, int count) {
        int[] dotIds = {R.id.dot1, R.id.dot2, R.id.dot3, R.id.dot4};
        for (int i = 0; i < dotIds.length; i++) {
            root.findViewById(dotIds[i]).setBackgroundResource(
                    i < count ? R.drawable.pin_dot_filled : R.drawable.pin_dot_empty);
        }
    }

    static void shakeDots(View root) {
        View container = root.findViewById(R.id.pin_dots);
        ObjectAnimator anim = ObjectAnimator.ofFloat(container, "translationX",
                0f, -10f, 10f, -10f, 10f, -5f, 5f, 0f);
        anim.setDuration(400);
        anim.start();
    }
}
