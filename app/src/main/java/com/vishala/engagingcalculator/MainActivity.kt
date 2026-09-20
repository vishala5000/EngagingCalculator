package com.vishala.engagingcalculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.ScaleAnimation
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.sqrt

class MainActivity : android.app.Activity() {

    private lateinit var display: TextView
    private lateinit var expression: TextView
    private lateinit var historyScroll: ScrollView
    private lateinit var historyContainer: LinearLayout
    private lateinit var historyOverlay: FrameLayout

    private var currentInput = "0"
    private var storedValue = 0.0
    private var pendingOperator: String? = null
    private var resetInput = false

    private val history = mutableListOf<String>()

    private val purple = Color.rgb(156, 107, 255)
    private val cyan = Color.rgb(66, 220, 255)
    private val pink = Color.rgb(255, 92, 180)
    private val green = Color.rgb(86, 230, 150)
    private val orange = Color.rgb(255, 171, 77)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(8, 9, 18)
        window.navigationBarColor = Color.rgb(8, 9, 18)

        val root = FrameLayout(this)
        root.setBackgroundColor(Color.rgb(8, 9, 18))

        createCalculator(root)
        createHistoryPanel(root)

        setContentView(root)
    }

    private fun createCalculator(root: FrameLayout) {
        val main = LinearLayout(this)
        main.orientation = LinearLayout.VERTICAL
        main.setPadding(dp(12), dp(10), dp(12), dp(8))

        root.addView(
            main,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val topBar = LinearLayout(this)
        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.CENTER_VERTICAL

        val title = TextView(this)
        title.text = "ENGAGING CALCULATOR"
        title.setTextColor(Color.WHITE)
        title.textSize = 18f
        title.typeface = Typeface.DEFAULT_BOLD
        title.letterSpacing = 0.04f

        topBar.addView(
            title,
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )

        val historyButton = makeButton("HIS", purple, 15f)
        historyButton.setOnClickListener {
            haptic()
            toggleHistory()
        }

        topBar.addView(
            historyButton,
            LinearLayout.LayoutParams(dp(64), dp(44))
        )

        main.addView(topBar)

        expression = TextView(this)
        expression.text = ""
        expression.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        expression.setTextColor(Color.rgb(150, 153, 170))
        expression.textSize = 17f
        expression.maxLines = 1

        main.addView(
            expression,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(38)
            )
        )

        display = TextView(this)
        display.text = "0"
        display.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        display.setTextColor(Color.WHITE)
        display.textSize = 43f
        display.typeface = Typeface.DEFAULT_BOLD
        display.maxLines = 1
        display.setPadding(dp(4), 0, dp(4), 0)

        main.addView(
            display,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(70)
            )
        )

        val adWebView = createAdWebView()
        main.addView(
            adWebView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(64)
            ).apply {
                topMargin = dp(4)
                bottomMargin = dp(6)
            }
        )

        val buttons = LinearLayout(this)
        buttons.orientation = LinearLayout.VERTICAL

        addButtonRow(
            buttons,
            listOf(
                "AC" to pink,
                "⌫" to orange,
                "%" to cyan,
                "÷" to purple
            )
        )

        addButtonRow(
            buttons,
            listOf(
                "7" to Color.rgb(28, 31, 48),
                "8" to Color.rgb(28, 31, 48),
                "9" to Color.rgb(28, 31, 48),
                "×" to purple
            )
        )

        addButtonRow(
            buttons,
            listOf(
                "4" to Color.rgb(28, 31, 48),
                "5" to Color.rgb(28, 31, 48),
                "6" to Color.rgb(28, 31, 48),
                "−" to purple
            )
        )

        addButtonRow(
            buttons,
            listOf(
                "1" to Color.rgb(28, 31, 48),
                "2" to Color.rgb(28, 31, 48),
                "3" to Color.rgb(28, 31, 48),
                "+" to purple
            )
        )

        addButtonRow(
            buttons,
            listOf(
                "±" to Color.rgb(42, 45, 65),
                "0" to Color.rgb(28, 31, 48),
                "." to Color.rgb(42, 45, 65),
                "=" to green
            )
        )

        main.addView(
            buttons,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val scientificScroll = HorizontalScrollView(this)
        scientificScroll.isHorizontalScrollBarEnabled = false

        val scientific = LinearLayout(this)
        scientific.orientation = LinearLayout.HORIZONTAL

        listOf(
            "√" to cyan,
            "x²" to cyan,
            "1/x" to cyan,
            "COPY" to pink
        ).forEach { (label, color) ->
            val button = makeButton(label, color, 14f)
            button.setOnClickListener {
                haptic()
                when (label) {
                    "√" -> squareRoot()
                    "x²" -> square()
                    "1/x" -> reciprocal()
                    "COPY" -> copyResult()
                }
            }

            scientific.addView(
                button,
                LinearLayout.LayoutParams(dp(88), dp(50)).apply {
                    marginStart = dp(4)
                    marginEnd = dp(4)
                }
            )
        }

        scientificScroll.addView(scientific)

        main.addView(
            scientificScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58)
            )
        )
    }

    private fun createAdWebView(): WebView {
        val webView = WebView(this)

        webView.setBackgroundColor(Color.rgb(13, 15, 27))
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        val settings = webView.settings

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadsImagesAutomatically = true
        settings.blockNetworkImage = false
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.setSupportMultipleWindows(false)
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mediaPlaybackRequiresUserGesture = true

        webView.loadUrl("file:///android_asset/ads.html")

        return webView
    }

    private fun addButtonRow(
        parent: LinearLayout,
        items: List<Pair<String, Int>>
    ) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL

        parent.addView(
            row,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        items.forEach { (label, color) ->
            val button = makeButton(label, color, 21f)

            button.setOnClickListener {
                haptic()
                animatePress(button)
                handleInput(label)
            }

            row.addView(
                button,
                LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    1f
                ).apply {
                    setMargins(dp(4), dp(4), dp(4), dp(4))
                }
            )
        }
    }

    private fun makeButton(
        text: String,
        backgroundColor: Int,
        size: Float
    ): Button {
        return Button(this).apply {
            this.text = text
            this.textSize = size
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setAllCaps(false)
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(backgroundColor)
            }
            stateListAnimator = null
            elevation = dp(3).toFloat()
            minHeight = 0
            minimumHeight = 0
            minWidth = 0
            minimumWidth = 0
            includeFontPadding = true
        }
    }

    private fun handleInput(value: String) {
        when {
            value in "0123456789" -> inputDigit(value)
            value == "." -> inputDecimal()
            value == "%" -> percentage()
            value == "±" -> toggleSign()
            value in listOf("+", "−", "×", "÷") -> operator(value)
            value == "=" -> calculate()
            value == "AC" -> clearAll()
            value == "⌫" -> backspace()
        }
    }

    private fun inputDigit(digit: String) {
        if (resetInput || currentInput == "0") {
            currentInput = digit
            resetInput = false
        } else {
            if (currentInput.length < 30) {
                currentInput += digit
            }
        }

        updateDisplay()
    }

    private fun inputDecimal() {
        if (resetInput) {
            currentInput = "0."
            resetInput = false
        } else if (!currentInput.contains(".")) {
            currentInput += "."
        }

        updateDisplay()
    }

    private fun operator(operator: String) {
        val number = currentInput.toDoubleOrNull() ?: return

        if (pendingOperator != null && !resetInput) {
            performPending(number)
        } else {
            storedValue = number
        }

        pendingOperator = operator
        resetInput = true

        expression.text = "${formatNumber(storedValue)} $operator"
    }

    private fun performPending(number: Double) {
        storedValue = when (pendingOperator) {
            "+" -> storedValue + number
            "−" -> storedValue - number
            "×" -> storedValue * number
            "÷" -> if (number == 0.0) Double.NaN else storedValue / number
            else -> number
        }

        if (storedValue.isNaN() || storedValue.isInfinite()) {
            currentInput = "Error"
            storedValue = 0.0
            pendingOperator = null
            resetInput = true
        } else {
            currentInput = formatNumber(storedValue)
        }
    }

    private fun calculate() {
        val number = currentInput.toDoubleOrNull() ?: return
        val operator = pendingOperator ?: return

        val left = storedValue
        val result = when (operator) {
            "+" -> left + number
            "−" -> left - number
            "×" -> left * number
            "÷" -> if (number == 0.0) Double.NaN else left / number
            else -> number
        }

        if (result.isNaN() || result.isInfinite()) {
            currentInput = "Error"
            expression.text = "Cannot calculate"
            pendingOperator = null
            resetInput = true
            updateDisplay()
            return
        }

        val calculation =
            "${formatNumber(left)} $operator ${formatNumber(number)} = ${formatNumber(result)}"

        history.add(0, calculation)

        while (history.size > 50) {
            history.removeAt(history.lastIndex)
        }

        currentInput = formatNumber(result)
        storedValue = result
        pendingOperator = null
        resetInput = true

        expression.text = calculation
        updateDisplay()
    }

    private fun percentage() {
        val number = currentInput.toDoubleOrNull() ?: return
        currentInput = formatNumber(number / 100.0)
        updateDisplay()
    }

    private fun toggleSign() {
        val number = currentInput.toDoubleOrNull() ?: return
        currentInput = formatNumber(-number)
        updateDisplay()
    }

    private fun squareRoot() {
        val number = currentInput.toDoubleOrNull() ?: return

        if (number < 0) {
            currentInput = "Error"
            expression.text = "√ of negative number"
            resetInput = true
        } else {
            val result = sqrt(number)
            history.add(0, "√(${formatNumber(number)}) = ${formatNumber(result)}")
            currentInput = formatNumber(result)
            expression.text = "√(${formatNumber(number)})"
            resetInput = true
        }

        trimHistory()
        updateDisplay()
    }

    private fun square() {
        val number = currentInput.toDoubleOrNull() ?: return
        val result = number * number

        history.add(
            0,
            "${formatNumber(number)}² = ${formatNumber(result)}"
        )

        currentInput = formatNumber(result)
        expression.text = "${formatNumber(number)}²"
        resetInput = true

        trimHistory()
        updateDisplay()
    }

    private fun reciprocal() {
        val number = currentInput.toDoubleOrNull() ?: return

        if (number == 0.0) {
            currentInput = "Error"
            expression.text = "Cannot divide by zero"
            resetInput = true
        } else {
            val result = 1.0 / number

            history.add(
                0,
                "1/${formatNumber(number)} = ${formatNumber(result)}"
            )

            currentInput = formatNumber(result)
            expression.text = "1/${formatNumber(number)}"
            resetInput = true
        }

        trimHistory()
        updateDisplay()
    }

    private fun clearAll() {
        currentInput = "0"
        storedValue = 0.0
        pendingOperator = null
        resetInput = false
        expression.text = ""
        updateDisplay()
    }

    private fun backspace() {
        if (resetInput) return

        if (currentInput.length > 1) {
            currentInput = currentInput.dropLast(1)
        } else {
            currentInput = "0"
        }

        updateDisplay()
    }

    private fun copyResult() {
        val clipboard =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "Calculator result",
                currentInput
            )
        )

        animateDisplay()
    }

    private fun updateDisplay() {
        display.text = currentInput
        animateDisplay()
    }

    private fun animateDisplay() {
        val animation = AlphaAnimation(0.75f, 1f)
        animation.duration = 120
        display.startAnimation(animation)
    }

    private fun animatePress(view: View) {
        val animation = ScaleAnimation(
            1f,
            0.94f,
            1f,
            0.94f,
            AnimationPivot.CENTER,
            AnimationPivot.CENTER
        )

        animation.duration = 70
        animation.repeatCount = 1
        animation.repeatMode = android.view.animation.Animation.REVERSE

        view.startAnimation(animation)
    }

    private object AnimationPivot {
        const val CENTER = 1f
    }

    private fun haptic() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                val vibratorManager =
                    getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(
                        18,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator =
                    getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

                @Suppress("DEPRECATION")
                vibrator.vibrate(18)
            }
        } catch (_: Exception) {
        }
    }

    private fun createHistoryPanel(root: FrameLayout) {
        historyOverlay = FrameLayout(this)
        historyOverlay.setBackgroundColor(Color.argb(245, 8, 9, 18))
        historyOverlay.visibility = View.GONE

        val panel = LinearLayout(this)
        panel.orientation = LinearLayout.VERTICAL
        panel.setPadding(dp(18), dp(20), dp(18), dp(18))

        historyOverlay.addView(
            panel,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val header = LinearLayout(this)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL

        val title = TextView(this)
        title.text = "CALCULATION HISTORY"
        title.textSize = 20f
        title.typeface = Typeface.DEFAULT_BOLD
        title.setTextColor(Color.WHITE)

        header.addView(
            title,
            LinearLayout.LayoutParams(0, dp(55), 1f)
        )

        val clear = makeButton("CLEAR", pink, 13f)
        clear.setOnClickListener {
            haptic()
            history.clear()
            refreshHistory()
        }

        header.addView(
            clear,
            LinearLayout.LayoutParams(dp(78), dp(45))
        )

        val close = makeButton("CLOSE", purple, 13f)
        close.setOnClickListener {
            haptic()
            hideHistory()
        }

        header.addView(
            close,
            LinearLayout.LayoutParams(dp(78), dp(45)).apply {
                marginStart = dp(6)
            }
        )

        panel.addView(header)

        historyScroll = ScrollView(this)
        historyScroll.isFillViewport = true

        historyContainer = LinearLayout(this)
        historyContainer.orientation = LinearLayout.VERTICAL

        historyScroll.addView(
            historyContainer,
            ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        panel.addView(
            historyScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                topMargin = dp(12)
            }
        )

        root.addView(
            historyOverlay,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        refreshHistory()
    }

    private fun refreshHistory() {
        if (!::historyContainer.isInitialized) return

        historyContainer.removeAllViews()

        if (history.isEmpty()) {
            val empty = TextView(this)
            empty.text = "No calculations yet"
            empty.textSize = 17f
            empty.gravity = Gravity.CENTER
            empty.setTextColor(Color.rgb(145, 149, 170))

            historyContainer.addView(
                empty,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(120)
                )
            )

            return
        }

        history.forEach { item ->
            val card = TextView(this)
            card.text = item
            card.textSize = 16f
            card.setTextColor(Color.WHITE)
            card.setPadding(dp(16), dp(15), dp(16), dp(15))
            card.gravity = Gravity.CENTER_VERTICAL

            card.background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(Color.rgb(25, 28, 45))
                setStroke(dp(1), Color.rgb(57, 61, 85))
            }

            card.setOnClickListener {
                haptic()
                currentInput = item.substringAfter("=").trim()
                resetInput = true
                updateDisplay()
                hideHistory()
            }

            historyContainer.addView(
                card,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(8)
                }
            )
        }
    }

    private fun trimHistory() {
        while (history.size > 50) {
            history.removeAt(history.lastIndex)
        }
    }

    private fun toggleHistory() {
        if (historyOverlay.visibility == View.VISIBLE) {
            hideHistory()
        } else {
            showHistory()
        }
    }

    private fun showHistory() {
        refreshHistory()
        historyOverlay.visibility = View.VISIBLE

        val animation = AlphaAnimation(0f, 1f)
        animation.duration = 180
        historyOverlay.startAnimation(animation)
    }

    private fun hideHistory() {
        val animation = AlphaAnimation(1f, 0f)
        animation.duration = 150

        animation.setAnimationListener(
            object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(
                    animation: android.view.animation.Animation?
                ) {
                }

                override fun onAnimationEnd(
                    animation: android.view.animation.Animation?
                ) {
                    historyOverlay.visibility = View.GONE
                }

                override fun onAnimationRepeat(
                    animation: android.view.animation.Animation?
                ) {
                }
            }
        )

        historyOverlay.startAnimation(animation)
    }

    private fun formatNumber(number: Double): String {
        if (number == 0.0) return "0"

        val abs = kotlin.math.abs(number)

        if (abs >= 1e12 || abs < 1e-9) {
            return String.format(Locale.US, "%.10g", number)
        }

        val formatter = DecimalFormat("#,##0.##########")
        return formatter.format(number)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onBackPressed() {
        if (::historyOverlay.isInitialized &&
            historyOverlay.visibility == View.VISIBLE
        ) {
            hideHistory()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (::historyOverlay.isInitialized) {
            historyOverlay.removeAllViews()
        }

        super.onDestroy()
    }
}
