package com.vishala.engagingcalculator

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : Activity() {

    private lateinit var display: TextView
    private lateinit var expression: TextView
    private lateinit var historyContainer: LinearLayout

    private var current = "0"
    private var stored = 0.0
    private var operator: String? = null
    private var waitingForOperand = false
    private var expressionText = ""

    private val history = ArrayDeque<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(9, 10, 15)
        window.navigationBarColor = Color.rgb(9, 10, 15)

        buildInterface()
    }

    private fun buildInterface() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(9, 10, 15))
            setPadding(dp(14), dp(10), dp(14), dp(8))
        }

        val title = TextView(this).apply {
            text = "CALCULATOR"
            textSize = 14f
            setTextColor(Color.rgb(180, 170, 255))
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.15f
            gravity = Gravity.CENTER
        }

        root.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(34)
            )
        )

        expression = TextView(this).apply {
            text = ""
            textSize = 16f
            setTextColor(Color.rgb(130, 135, 155))
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(12), 0)
            maxLines = 1
        }

        root.addView(
            expression,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(34)
            )
        )

        display = TextView(this).apply {
            text = "0"
            textSize = 46f
            setTextColor(Color.WHITE)
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(dp(12), 0, dp(12), 0)
            maxLines = 1
        }

        root.addView(
            display,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(76)
            )
        )

        val copyButton = makeButton("COPY", 0xFF20222C.toInt())

        copyButton.setOnClickListener {
            copyResult()
            vibrate()
        }

        root.addView(
            copyButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44)
            ).apply {
                topMargin = dp(4)
                bottomMargin = dp(8)
            }
        )

        val calculator = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val rows = listOf(
            listOf("AC", "⌫", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("±", "0", ".", "=")
        )

        rows.forEach { rowItems ->

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            rowItems.forEach { item ->

                val color = when {
                    item == "=" -> 0xFF7C4DFF.toInt()
                    item in listOf("÷", "×", "−", "+") -> 0xFF29213D.toInt()
                    item in listOf("AC", "⌫", "%", "±") -> 0xFF20222C.toInt()
                    else -> 0xFF15171E.toInt()
                }

                val button = makeButton(item, color)

                button.setOnClickListener {
                    vibrate()
                    handleInput(item)
                }

                row.addView(
                    button,
                    LinearLayout.LayoutParams(
                        0,
                        dp(62),
                        1f
                    ).apply {
                        setMargins(dp(4), dp(4), dp(4), dp(4))
                    }
                )
            }

            calculator.addView(
                row,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(70)
                )
            )
        }

        val advancedRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        listOf("√", "x²", "1/x", "HISTORY").forEach { item ->

            val button = makeButton(
                item,
                0xFF20222C.toInt()
            )

            button.setOnClickListener {
                vibrate()

                when (item) {
                    "√" -> squareRoot()
                    "x²" -> square()
                    "1/x" -> reciprocal()
                    "HISTORY" -> toggleHistory()
                }
            }

            advancedRow.addView(
                button,
                LinearLayout.LayoutParams(
                    0,
                    dp(52),
                    1f
                ).apply {
                    setMargins(dp(4), dp(3), dp(4), dp(3))
                }
            )
        }

        calculator.addView(advancedRow)

        root.addView(
            calculator,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        historyContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
            visibility = View.GONE
        }

        val historyScroll = ScrollView(this)

        historyScroll.addView(
            historyContainer,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            historyScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(130)
            )
        )

        val ad = createAdWebView()

        root.addView(
            ad,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(72)
            ).apply {
                topMargin = dp(4)
            }
        )

        setContentView(root)
    }

    private fun createAdWebView(): WebView {

        return WebView(this).apply {

            setBackgroundColor(Color.rgb(9, 10, 15))

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }

            webViewClient = WebViewClient()

            loadUrl("file:///android_asset/ads.html")
        }
    }

    private fun makeButton(
        text: String,
        background: Int
    ): TextView {

        return TextView(this).apply {
            this.text = text
            textSize = if (text == "HISTORY") 12f else 21f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundColor(background)

            isClickable = true
            isFocusable = true

            setOnTouchListener { view, event ->

                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        view.alpha = 0.65f
                        view.scaleX = 0.96f
                        view.scaleY = 0.96f
                    }

                    android.view.MotionEvent.ACTION_UP,
                    android.view.MotionEvent.ACTION_CANCEL -> {
                        view.alpha = 1f
                        view.scaleX = 1f
                        view.scaleY = 1f
                    }
                }

                false
            }
        }
    }

    private fun handleInput(input: String) {

        when {
            input == "AC" -> clearAll()

            input == "⌫" -> backspace()

            input == "." -> decimal()

            input == "%" -> percentage()

            input == "±" -> toggleSign()

            input in listOf("+", "−", "×", "÷") -> chooseOperator(input)

            input == "=" -> calculate()

            input.all { it.isDigit() } -> digit(input)
        }
    }

    private fun digit(digit: String) {

        if (waitingForOperand || current == "0") {
            current = digit
            waitingForOperand = false
        } else {
            if (current.length < 18) {
                current += digit
            }
        }

        updateDisplay()
    }

    private fun decimal() {

        if (waitingForOperand) {
            current = "0."
            waitingForOperand = false
        } else if (!current.contains(".")) {
            current += "."
        }

        updateDisplay()
    }

    private fun percentage() {

        val value = current.toDoubleOrNull() ?: return
        current = formatNumber(value / 100.0)
        updateDisplay()
    }

    private fun toggleSign() {

        if (current == "0") return

        current = if (current.startsWith("-")) {
            current.substring(1)
        } else {
            "-$current"
        }

        updateDisplay()
    }

    private fun chooseOperator(newOperator: String) {

        val value = current.toDoubleOrNull() ?: 0.0

        if (operator != null && !waitingForOperand) {
            stored = performOperation(
                stored,
                value,
                operator!!
            )

            current = formatNumber(stored)
        } else {
            stored = value
        }

        operator = newOperator
        waitingForOperand = true

        expressionText =
            "${formatNumber(stored)} $newOperator"

        expression.text = expressionText
        updateDisplay()
    }

    private fun calculate() {

        val op = operator ?: return
        val right = current.toDoubleOrNull() ?: 0.0

        val left = stored

        val result = performOperation(left, right, op)

        val equation =
            "${formatNumber(left)} $op ${formatNumber(right)} = ${formatNumber(result)}"

        addHistory(equation)

        current = formatNumber(result)
        stored = result
        operator = null
        waitingForOperand = true

        expression.text = equation.substringBefore("=")

        updateDisplay()
    }

    private fun performOperation(
        left: Double,
        right: Double,
        op: String
    ): Double {

        return when (op) {
            "+" -> left + right
            "−" -> left - right
            "×" -> left * right
            "÷" -> if (right == 0.0) Double.NaN else left / right
            else -> right
        }
    }

    private fun squareRoot() {

        val value = current.toDoubleOrNull() ?: return

        if (value < 0) {
            current = "Error"
        } else {
            val result = sqrt(value)
            addHistory("√${formatNumber(value)} = ${formatNumber(result)}")
            current = formatNumber(result)
        }

        waitingForOperand = true
        updateDisplay()
    }

    private fun square() {

        val value = current.toDoubleOrNull() ?: return

        val result = value.pow(2)

        addHistory(
            "${formatNumber(value)}² = ${formatNumber(result)}"
        )

        current = formatNumber(result)
        waitingForOperand = true

        updateDisplay()
    }

    private fun reciprocal() {

        val value = current.toDoubleOrNull() ?: return

        if (value == 0.0) {
            current = "Error"
        } else {
            val result = 1.0 / value

            addHistory(
                "1/${formatNumber(value)} = ${formatNumber(result)}"
            )

            current = formatNumber(result)
        }

        waitingForOperand = true
        updateDisplay()
    }

    private fun clearAll() {

        current = "0"
        stored = 0.0
        operator = null
        waitingForOperand = false
        expressionText = ""

        expression.text = ""
        updateDisplay()
    }

    private fun backspace() {

        if (waitingForOperand) return

        current = when {
            current.length <= 1 -> "0"
            current.startsWith("-") && current.length == 2 -> "0"
            else -> current.dropLast(1)
        }

        updateDisplay()
    }

    private fun updateDisplay() {

        display.text = current

        display.animate()
            .scaleX(1.02f)
            .scaleY(1.02f)
            .setDuration(70)
            .withEndAction {
                display.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(70)
                    .start()
            }
            .start()
    }

    private fun addHistory(item: String) {

        history.addFirst(item)

        while (history.size > 20) {
            history.removeLast()
        }

        refreshHistory()
    }

    private fun refreshHistory() {

        historyContainer.removeAllViews()

        history.forEach { item ->

            val row = TextView(this).apply {
                text = item
                textSize = 14f
                setTextColor(Color.rgb(210, 210, 220))
                setPadding(
                    dp(8),
                    dp(7),
                    dp(8),
                    dp(7)
                )
                gravity = Gravity.CENTER_VERTICAL
            }

            row.setOnClickListener {

                val result = item.substringAfterLast("=")
                    .trim()

                if (result.toDoubleOrNull() != null) {
                    current = result
                    waitingForOperand = true
                    updateDisplay()
                }
            }

            historyContainer.addView(
                row,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(36)
                )
            )
        }
    }

    private fun toggleHistory() {

        historyContainer.visibility =
            if (historyContainer.visibility == View.VISIBLE) {
                View.GONE
            } else {
                View.VISIBLE
            }
    }

    private fun copyResult() {

        val clipboard =
            getSystemService(Context.CLIPBOARD_SERVICE)
                    as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "Calculator result",
                current
            )
        )
    }

    private fun formatNumber(value: Double): String {

        if (value.isNaN() || value.isInfinite()) {
            return "Error"
        }

        if (value == 0.0) return "0"

        return if (value % 1.0 == 0.0) {
            String.format(
                Locale.US,
                "%.0f",
                value
            )
        } else {
            String.format(
                Locale.US,
                "%.10f",
                value
            ).trimEnd('0').trimEnd('.')
        }
    }

    private fun vibrate() {

        val vibrator =
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    18,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(18)
        }
    }

    private fun dp(value: Int): Int {

        return (value * resources.displayMetrics.density)
            .toInt()
    }

    override fun onBackPressed() {

        if (historyContainer.visibility == View.VISIBLE) {
            historyContainer.visibility = View.GONE
        } else {
            super.onBackPressed()
        }
    }
}
