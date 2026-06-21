package uz.kidzone.app

import android.animation.ObjectAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object PinDialogHelper {

    fun interface OnPinResult {
        fun onResult(pin: String) // 4-digit string, or "" if skipped
    }

    fun interface OnPinVerified {
        fun onVerified()
    }

    /** Show "Create PIN" flow: enter, confirm, skip option. Calls cb.onResult(pin) or cb.onResult("") on skip. */
    @JvmStatic
    fun showCreate(ctx: Context, cb: OnPinResult) {
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_pin, null)
        val tvTitle = view.findViewById<TextView>(R.id.pin_title)
        tvTitle.setText("Create a parent PIN")

        val entered = StringBuilder()
        val firstPin = arrayOf<String?>(null)

        val dialog: AlertDialog = MaterialAlertDialogBuilder(ctx)
            .setView(view).setCancelable(false).create()

        wireKeypad(view, entered) {
            val pin = entered.toString()
            if (firstPin[0] == null) {
                firstPin[0] = pin
                entered.setLength(0)
                updateDots(view, 0)
                tvTitle.setText("Confirm PIN")
            } else if (pin == firstPin[0]) {
                dialog.dismiss()
                cb.onResult(pin)
            } else {
                shakeDots(view)
                entered.setLength(0)
                firstPin[0] = null
                updateDots(view, 0)
                tvTitle.setText("Create a parent PIN")
            }
        }

        view.findViewById<View>(R.id.pin_skip).visibility = View.VISIBLE
        view.findViewById<View>(R.id.pin_skip).setOnClickListener {
            dialog.dismiss()
            cb.onResult("")
        }

        dialog.show()
    }

    /** Show "Enter PIN" flow — no skip button. Calls cb.onVerified() on correct PIN. */
    @JvmStatic
    fun showEnter(ctx: Context, expectedHash: String, cb: OnPinVerified) {
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_pin, null)
        val tvTitle = view.findViewById<TextView>(R.id.pin_title)
        tvTitle.setText("Enter parent PIN")
        view.findViewById<View>(R.id.pin_skip).visibility = View.GONE

        val entered = StringBuilder()

        val dialog: AlertDialog = MaterialAlertDialogBuilder(ctx)
            .setView(view).setCancelable(true).create()

        wireKeypad(view, entered) {
            if (PinUtil.matches(entered.toString(), expectedHash)) {
                dialog.dismiss()
                cb.onVerified()
            } else {
                shakeDots(view)
                entered.setLength(0)
                updateDots(view, 0)
            }
        }

        dialog.show()
    }

    private fun wireKeypad(view: View, entered: StringBuilder, onFourDigits: Runnable) {
        val keyIds = intArrayOf(
            R.id.pin_key_1, R.id.pin_key_2, R.id.pin_key_3,
            R.id.pin_key_4, R.id.pin_key_5, R.id.pin_key_6,
            R.id.pin_key_7, R.id.pin_key_8, R.id.pin_key_9,
            R.id.pin_key_0
        )
        val digits = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")

        for (i in keyIds.indices) {
            val d = digits[i]
            view.findViewById<View>(keyIds[i]).setOnClickListener {
                if (entered.length >= 4) return@setOnClickListener
                entered.append(d)
                updateDots(view, entered.length)
                if (entered.length == 4) onFourDigits.run()
            }
        }

        view.findViewById<View>(R.id.pin_backspace).setOnClickListener {
            if (entered.isNotEmpty()) {
                entered.deleteCharAt(entered.length - 1)
                updateDots(view, entered.length)
            }
        }
    }

    @JvmStatic
    fun updateDots(root: View, count: Int) {
        val dotIds = intArrayOf(R.id.dot1, R.id.dot2, R.id.dot3, R.id.dot4)
        for (i in dotIds.indices) {
            root.findViewById<View>(dotIds[i]).setBackgroundResource(
                if (i < count) R.drawable.pin_dot_filled else R.drawable.pin_dot_empty
            )
        }
    }

    @JvmStatic
    fun shakeDots(root: View) {
        val container = root.findViewById<View>(R.id.pin_dots)
        val anim = ObjectAnimator.ofFloat(
            container, "translationX",
            0f, -10f, 10f, -10f, 10f, -5f, 5f, 0f
        )
        anim.duration = 400
        anim.start()
    }
}
