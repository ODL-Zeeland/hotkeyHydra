package com.example.hotkeyhydra

import android.annotation.SuppressLint
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import androidx.core.content.edit

/**
 * Hotkey Hydra custom input method.
 *
 * Default view: numeric keypad (on fresh install) with a key to jump to QWERTY.
 * Secondary view: standard QWERTY letters with a key to jump back to numeric.
 * Last used layout is remembered across input sessions.
 * Continuous deletion on delete key long-press.
 * Shift single tap (single letter uppercase) / double tap (CAPS LOCK) support.
 */
@Suppress("DEPRECATION")
class HotkeyHydraService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private enum class ShiftState {
        OFF,
        SHIFT_ONCE,
        CAPS_LOCK
    }

    private lateinit var keyboardView: KeyboardView
    private lateinit var numericKeyboard: Keyboard
    private lateinit var qwertyKeyboard: Keyboard

    private var shiftState = ShiftState.OFF
    private var lastShiftTapTime = 0L

    private val handler = Handler(Looper.getMainLooper())
    private var isDeletePressed = false

    private val deleteRunnable = object : Runnable {
        override fun run() {
            if (isDeletePressed) {
                val ic = currentInputConnection
                ic?.deleteSurroundingText(1, 0)
                handler.postDelayed(this, DELETE_REPEAT_INTERVAL)
            }
        }
    }

    companion object {
        const val CODE_SWITCH_TO_QWERTY = -101
        const val CODE_SWITCH_TO_NUMERIC = -102
        const val CODE_F1 = -111
        const val CODE_F2 = -112
        const val CODE_F3 = -113
        const val CODE_F4 = -114
        const val CODE_F5 = -115

        private const val DOUBLE_TAP_TIMEOUT = 300L
        private const val DELETE_INITIAL_DELAY = 400L
        private const val DELETE_REPEAT_INTERVAL = 50L

        private const val PREFS_NAME = "hotkey_hydra_prefs"
        private const val KEY_ACTIVE_LAYOUT = "active_layout"
        private const val LAYOUT_NUMERIC = "NUMERIC"
        private const val LAYOUT_QWERTY = "QWERTY"
    }

    @SuppressLint("InflateParams")
    override fun onCreateInputView(): View {
        val root = layoutInflater.inflate(R.layout.keyboard_view_layout, null)
        keyboardView = root.findViewById(R.id.keyboard_view)
        keyboardView.isPreviewEnabled = false
        numericKeyboard = Keyboard(this, R.xml.keyboard_numeric)
        qwertyKeyboard = Keyboard(this, R.xml.keyboard_qwerty)

        val activeLayout = loadActiveLayout()
        keyboardView.keyboard = if (activeLayout == LAYOUT_QWERTY) qwertyKeyboard else numericKeyboard
        keyboardView.setOnKeyboardActionListener(this)

        root.setOnApplyWindowInsetsListener { view, insets ->
            val navBarInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.navigationBars()).bottom
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetBottom
            }
            val bufferView = view.findViewById<View>(R.id.bottom_buffer_space)
            if (bufferView != null) {
                val minBufferPx = (48 * resources.displayMetrics.density).toInt()
                val params = bufferView.layoutParams
                params.height = maxOf(navBarInset, minBufferPx)
                bufferView.layoutParams = params
            }
            insets
        }

        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val activeLayout = loadActiveLayout()
        keyboardView.keyboard = if (activeLayout == LAYOUT_QWERTY) qwertyKeyboard else numericKeyboard

        shiftState = ShiftState.OFF
        updateShiftAndLetterLabels()
        keyboardView.invalidateAllKeys()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        stopDeleteRepeat()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopDeleteRepeat()
    }

    @Deprecated("Deprecated in Java", ReplaceWith("onKey"))
    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return

        when (primaryCode) {
            CODE_SWITCH_TO_QWERTY -> {
                keyboardView.keyboard = qwertyKeyboard
                saveActiveLayout(LAYOUT_QWERTY)
                shiftState = ShiftState.OFF
                updateShiftAndLetterLabels()
                keyboardView.invalidateAllKeys()
            }

            CODE_SWITCH_TO_NUMERIC -> {
                keyboardView.keyboard = numericKeyboard
                saveActiveLayout(LAYOUT_NUMERIC)
            }

            Keyboard.KEYCODE_SHIFT -> {
                handleShiftTap()
            }

            Keyboard.KEYCODE_DELETE -> {
                ic.deleteSurroundingText(1, 0)
            }

            10 -> { // Tab key
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_TAB))
            }

            CODE_F1 -> sendFunctionKey(KeyEvent.KEYCODE_F1)
            CODE_F2 -> sendFunctionKey(KeyEvent.KEYCODE_F2)
            CODE_F3 -> sendFunctionKey(KeyEvent.KEYCODE_F3)
            CODE_F4 -> sendFunctionKey(KeyEvent.KEYCODE_F4)
            CODE_F5 -> sendFunctionKey(KeyEvent.KEYCODE_F5)

            else -> {
                var code = primaryCode.toChar()
                if ((shiftState != ShiftState.OFF) && Character.isLetter(code)) {
                    code = Character.toUpperCase(code)
                }
                ic.commitText(code.toString(), 1)

                if ((shiftState == ShiftState.SHIFT_ONCE) && Character.isLetter(primaryCode.toChar())) {
                    shiftState = ShiftState.OFF
                    updateShiftAndLetterLabels()
                    keyboardView.invalidateAllKeys()
                }
            }
        }
    }

    private fun handleShiftTap() {
        val now = System.currentTimeMillis()
        shiftState = if (shiftState == ShiftState.CAPS_LOCK) {
            ShiftState.OFF
        } else if (shiftState == ShiftState.SHIFT_ONCE) {
            if (now - lastShiftTapTime < DOUBLE_TAP_TIMEOUT) {
                ShiftState.CAPS_LOCK
            } else {
                ShiftState.OFF
            }
        } else {
            if (now - lastShiftTapTime < DOUBLE_TAP_TIMEOUT) {
                ShiftState.CAPS_LOCK
            } else {
                ShiftState.SHIFT_ONCE
            }
        }
        lastShiftTapTime = now
        updateShiftAndLetterLabels()
        keyboardView.invalidateAllKeys()
    }

    private fun sendFunctionKey(keyCode: Int) {
        val ic = currentInputConnection ?: return
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun updateShiftAndLetterLabels() {
        val isUpper = shiftState != ShiftState.OFF
        qwertyKeyboard.isShifted = isUpper
        for (key in qwertyKeyboard.keys) {
            val code = key.codes.firstOrNull() ?: continue
            if (code == Keyboard.KEYCODE_SHIFT) {
                key.label = when (shiftState) {
                    ShiftState.OFF -> "⇧"
                    ShiftState.SHIFT_ONCE -> "⇧"
                    ShiftState.CAPS_LOCK -> "⇪"
                }
            } else if (code in 97..122) { // a-z
                key.label = if (isUpper) {
                    key.label.toString().uppercase()
                } else {
                    key.label.toString().lowercase()
                }
            }
        }
    }

    private fun loadActiveLayout(): String {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return prefs.getString(KEY_ACTIVE_LAYOUT, LAYOUT_NUMERIC) ?: LAYOUT_NUMERIC
    }

    private fun saveActiveLayout(layout: String) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit {
            putString(KEY_ACTIVE_LAYOUT, layout)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onPress(primaryCode: Int) {
        if (primaryCode == Keyboard.KEYCODE_DELETE) {
            isDeletePressed = true
            handler.removeCallbacks(deleteRunnable)
            handler.postDelayed(deleteRunnable, DELETE_INITIAL_DELAY)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRelease(primaryCode: Int) {
        if (primaryCode == Keyboard.KEYCODE_DELETE) {
            stopDeleteRepeat()
        }
    }

    private fun stopDeleteRepeat() {
        isDeletePressed = false
        handler.removeCallbacks(deleteRunnable)
    }

    override fun onText(text: CharSequence?) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}
}
