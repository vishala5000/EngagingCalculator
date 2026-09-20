package com.vishala.engagingcalculator

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.round
import kotlin.math.sqrt

class MainActivity : android.app.Activity() {

    private lateinit var expressionText: TextView
    private lateinit var resultText: TextView

    private lateinit var historyScroll: ScrollView
    private lateinit var historyContainer: LinearLayout

    private lateinit var adWebView: WebView

    private val history = mutableListOf<String>()

    private var currentInput = "0"
    private var storedValue = 0.0
    private var pendingOperator: String? = null
    private var waitingForOperand = false
    private var justCalculated = false

    private val backgroundColor = Color.rgb(8, 9, 18)
    private val cardColor = Color.rgb(17, 19, 34)

    private val numberColor = Color.rgb(30, 34, 54)
    private val numberPressed = Color.rgb(47, 51, 78)

    private val operatorColor = Color.rgb(116, 72, 255)
    private val operatorPressed = Color.rgb(145, 102, 255)

    private val functionColor = Color.rgb(22, 112, 120)
    private val functionPressed = Color.rgb(31, 146, 154)

    private val specialColor = Color.rgb(190, 56, 110)
    private val specialPressed = Color.rgb(220, 77, 135)

    private val equalsColor = Color.rgb(255, 105, 45)
    private val equalsPressed = Color.rgb(255, 137, 67)

    private val white = Color.rgb(245, 247, 255)
    private val secondaryText = Color.rgb(160, 166, 190)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor

        buildInterface()
    }

    private fun buildInterface() {

        val root = FrameLayout(this).apply {
            setBackgroundColor(backgroundColor)
            fitsSystemWindows = true
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
        }

        mainLayout.addView(
            createTopBar(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(64)
            )
        )

        mainLayout.addView(
            createDisplayCard(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                setMargins(dp(12), dp(4), dp(12), dp(6))
            }
        )

        mainLayout.addView(
            createAdCard(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(64)
            ).apply {
                setMargins(dp(12), 0, dp(12), dp(5))
            }
        )

        mainLayout.addView(
            createKeyboard(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                2.7f
            ).apply {
                setMargins(dp(10), 0, dp(10), dp(8))
            }
        )

        root.addView(
            mainLayout,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        /*
         * IMPORTANT:
         * The history panel is now created and initialized BEFORE
         * any HISTORY/HIS button can use it.
         */
        createHistoryPanel(root)

        setContentView(root)
    }

    private fun createTopBar(): LinearLayout {

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(5), dp(16), dp(2))
        }

        val titleContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val title = TextView(this).apply {
            text = "ENGAGING"
            textSize = 20f
            typeface = Typeface.create(
                "sans-serif-black",
                Typeface.BOLD
            )
            setTextColor(Color.rgb(255, 112, 65))
        }

        val subtitle = TextView(this).apply {
            text = "CALCULATOR"
            textSize = 10f
            letterSpacing = 0.18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(174, 128, 255))
        }

        titleContainer.addView(title)
        titleContainer.addView(subtitle)

        bar.addView(
            titleContainer,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val historyButton =
            createSmallButton("HISTORY", functionColor)

        historyButton.setOnClickListener {
            vibrate()
            toggleHistory()
        }

        bar.addView(
            historyButton,
            LinearLayout.LayoutParams(
                dp(82),
                dp(38)
            ).apply {
                setMargins(0, 0, dp(7), 0)
            }
        )

        val copyButton =
            createSmallButton("COPY", operatorColor)

        copyButton.setOnClickListener {
            vibrate()
            copyResult()
        }

        bar.addView(
            copyButton,
            LinearLayout.LayoutParams(
                dp(68),
                dp(38)
            )
        )

        return bar
    }

    private fun createDisplayCard(): LinearLayout {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM

            setPadding(
                dp(18),
                dp(12),
                dp(18),
                dp(14)
            )

            background = roundedBackground(
                cardColor,
                dp(24).toFloat()
            )
        }

        val label = TextView(this).apply {
            text = "READY"
            textSize = 9f
            letterSpacing = 0.25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(123, 210, 255))
        }

        expressionText = TextView(this).apply {
            text = ""
            textSize = 18f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setTextColor(secondaryText)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.START
        }

        resultText = TextView(this).apply {
            text = "0"
            textSize = 46f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL

            typeface = Typeface.create(
                "sans-serif",
                Typeface.BOLD
            )

            setTextColor(white)

            maxLines = 1
            ellipsize = TextUtils.TruncateAt.START
        }

        card.addView(
            label,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(24)
            )
        )

        card.addView(
            expressionText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(42)
            )
        )

        card.addView(
            resultText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(70)
            )
        )

        return card
    }

    private fun createAdCard(): LinearLayout {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER

            background = roundedBackground(
                Color.rgb(13, 15, 27),
                dp(16).toFloat()
            )
        }

        adWebView = WebView(this).apply {

            setBackgroundColor(Color.TRANSPARENT)

            webViewClient = WebViewClient()

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true

                cacheMode = WebSettings.LOAD_DEFAULT

                loadWithOverviewMode = true
                useWideViewPort = true
            }

            loadUrl("file:///android_asset/ads.html")
        }

        card.addView(
            adWebView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        return card
    }

    /*
     * FIX:
     * Properly creates the history panel and initializes both
     * historyScroll and historyContainer.
     */
    private fun createHistoryPanel(root: FrameLayout) {

        historyScroll = ScrollView(this).apply {

            visibility = View.GONE
            alpha = 0f

            setBackgroundColor(
                Color.argb(245, 8, 9, 18)
            )

            isFillViewport = true

            isVerticalScrollBarEnabled = false
        }

        val panel = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(14),
                dp(18),
                dp(14),
                dp(18)
            )

            background = roundedBackground(
                Color.rgb(15, 17, 31),
                dp(22).toFloat()
            )
        }

        val header = LinearLayout(this).apply {

            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val heading = TextView(this).apply {

            text = "CALCULATION HISTORY"
            textSize = 18f

            typeface = Typeface.create(
                "sans-serif-black",
                Typeface.BOLD
            )

            setTextColor(white)
        }

        header.addView(
            heading,
            LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            )
        )

        val clearButton = createSmallButton(
            "CLEAR",
            specialColor
        )

        clearButton.setOnClickListener {

            vibrate()

            history.clear()

            refreshHistory()
        }

        header.addView(
            clearButton,
            LinearLayout.LayoutParams(
                dp(70),
                dp(38)
            )
        )

        panel.addView(header)

        val closeButton = createSmallButton(
            "CLOSE",
            functionColor
        )

        closeButton.setOnClickListener {

            vibrate()

            hideHistory()
        }

        panel.addView(
            closeButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(42)
            ).apply {
                setMargins(0, dp(10), 0, dp(8))
            }
        )

        historyContainer = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL
        }

        panel.addView(
            historyContainer,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        historyScroll.addView(
            panel,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            historyScroll,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(
                    dp(8),
                    dp(68),
                    dp(8),
                    dp(8)
                )
            }
        )

        refreshHistory()
    }

    private fun createKeyboard(): LinearLayout {

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scientific = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 4f
        }

        addButton(
            scientific,
            "√",
            functionColor
        ) {
            squareRoot()
        }

        addButton(
            scientific,
            "x²",
            functionColor
        ) {
            square()
        }

        addButton(
            scientific,
            "1/x",
            functionColor
        ) {
            reciprocal()
        }

        addButton(
            scientific,
            "HIS",
            functionColor
        ) {
            toggleHistory()
        }

        container.addView(
            scientific,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                0.75f
            )
        )

        addKeyboardRow(
            container,
            arrayOf("AC", "⌫", "%", "÷")
        ) { button ->

            when (button) {

                "AC" -> clearAll()

                "⌫" -> backspace()

                "%" -> percentage()

                "÷" -> chooseOperator("/")
            }
        }

        addKeyboardRow(
            container,
            arrayOf("7", "8", "9", "×")
        ) { button ->

            if (button == "×") {
                chooseOperator("*")
            } else {
                digit(button)
            }
        }

        addKeyboardRow(
            container,
            arrayOf("4", "5", "6", "−")
        ) { button ->

            if (button == "−") {
                chooseOperator("-")
            } else {
                digit(button)
            }
        }

        addKeyboardRow(
            container,
            arrayOf("1", "2", "3", "+")
        ) { button ->

            if (button == "+") {
                chooseOperator("+")
            } else {
                digit(button)
            }
        }

        addKeyboardRow(
            container,
            arrayOf("±", "0", ".", "=")
        ) { button ->

            when (button) {

                "±" -> toggleSign()

                "0" -> digit("0")

                "." -> decimal()

                "=" -> calculate()
            }
        }

        return container
    }

    private fun addKeyboardRow(
        parent: LinearLayout,
        buttons: Array<String>,
        action: (String) -> Unit
    ) {

        val row = LinearLayout(this).apply {

            orientation = LinearLayout.HORIZONTAL

            weightSum = buttons.size.toFloat()
        }

        for (text in buttons) {

            val color = when {

                text == "=" ->
                    equalsColor

                text in arrayOf(
                    "+",
                    "−",
                    "×",
                    "÷"
                ) ->
                    operatorColor

                text in arrayOf(
                    "AC",
                    "⌫",
                    "%",
                    "±"
                ) ->
                    specialColor

                else ->
                    numberColor
            }

            addButton(
                row,
                text,
                color
            ) {
                action(text)
            }
        }

        parent.addView(
            row,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
    }

    private fun addButton(
        row: LinearLayout,
        text: String,
        color: Int,
        action: () -> Unit
    ) {

        val button = TextView(this).apply {

            this.text = text

            textSize =
                if (text.length > 2) 14f
                else 22f

            gravity = Gravity.CENTER

            typeface = Typeface.create(
                "sans-serif",
                Typeface.BOLD
            )

            setTextColor(white)

            background = roundedBackground(
                color,
                dp(18).toFloat()
            )

            isClickable = true
            isFocusable = true

            setOnClickListener {

                animateButton(this)

                vibrate()

                action()
            }

            setOnTouchListener { view, event ->

                when (event.action) {

                    MotionEvent.ACTION_DOWN -> {

                        view.background =
                            roundedBackground(
                                pressedColor(color),
                                dp(18).toFloat()
                            )
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {

                        view.background =
                            roundedBackground(
                                color,
                                dp(18).toFloat()
                            )
                    }
                }

                false
            }
        }

        row.addView(
            button,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            ).apply {
                setMargins(
                    dp(4),
                    dp(4),
                    dp(4),
                    dp(4)
                )
            }
        )
    }

    private fun createSmallButton(
        text: String,
        color: Int
    ): TextView {

        return TextView(this).apply {

            this.text = text

            textSize = 9f

            gravity = Gravity.CENTER

            typeface = Typeface.DEFAULT_BOLD

            letterSpacing = 0.08f

            setTextColor(white)

            background = roundedBackground(
                color,
                dp(12).toFloat()
            )

            isClickable = true
            isFocusable = true

            setOnTouchListener { view, event ->

                when (event.action) {

                    MotionEvent.ACTION_DOWN -> {

                        view.alpha = 0.65f
                        view.scaleX = 0.95f
                        view.scaleY = 0.95f
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {

                        view.alpha = 1f
                        view.scaleX = 1f
                        view.scaleY = 1f
                    }
                }

                false
            }
        }
    }

    private fun digit(value: String) {

        if (waitingForOperand || justCalculated) {

            currentInput = value

            waitingForOperand = false
            justCalculated = false

        } else {

            if (currentInput == "0") {

                currentInput = value

            } else if (currentInput.length < 30) {

                currentInput += value
            }
        }

        updateDisplay()
    }

    private fun decimal() {

        if (waitingForOperand || justCalculated) {

            currentInput = "0."

            waitingForOperand = false
            justCalculated = false

        } else if (!currentInput.contains(".")) {

            currentInput += "."
        }

        updateDisplay()
    }

    private fun percentage() {

        val value =
            currentInput.toDoubleOrNull()
                ?: return

        currentInput =
            formatNumber(value / 100.0)

        updateDisplay()
    }

    private fun toggleSign() {

        if (currentInput == "0") return

        currentInput =
            if (currentInput.startsWith("-")) {

                currentInput.substring(1)

            } else {

                "-$currentInput"
            }

        updateDisplay()
    }

    private fun chooseOperator(
        operator: String
    ) {

        val input =
            currentInput.toDoubleOrNull()
                ?: return

        if (
            pendingOperator != null &&
            !waitingForOperand
        ) {

            val result =
                performOperation(
                    storedValue,
                    input,
                    pendingOperator!!
                )

            if (result == null) {

                Toast.makeText(
                    this,
                    "Cannot divide by zero",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            storedValue = result

            currentInput =
                formatNumber(result)

        } else {

            storedValue = input
        }

        pendingOperator = operator

        waitingForOperand = true
        justCalculated = false

        expressionText.text =
            "${formatNumber(storedValue)} ${displayOperator(operator)}"

        updateDisplay()
    }

    private fun calculate() {

        val operator =
            pendingOperator
                ?: return

        val input =
            currentInput.toDoubleOrNull()
                ?: return

        val result =
            performOperation(
                storedValue,
                input,
                operator
            )

        if (result == null) {

            Toast.makeText(
                this,
                "Cannot divide by zero",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val expression =
            "${formatNumber(storedValue)} " +
                    "${displayOperator(operator)} " +
                    formatNumber(input)

        val resultString =
            formatNumber(result)

        addHistory(
            "$expression = $resultString"
        )

        currentInput = resultString

        storedValue = result

        pendingOperator = null

        waitingForOperand = false

        justCalculated = true

        expressionText.text = expression

        updateDisplay()

        animateResult()
    }

    private fun performOperation(
        first: Double,
        second: Double,
        operator: String
    ): Double? {

        return when (operator) {

            "+" -> first + second

            "-" -> first - second

            "*" -> first * second

            "/" ->
                if (second == 0.0) {
                    null
                } else {
                    first / second
                }

            else -> second
        }
    }

    private fun squareRoot() {

        val value =
            currentInput.toDoubleOrNull()
                ?: return

        if (value < 0) {

            Toast.makeText(
                this,
                "Invalid number",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val result = sqrt(value)

        addHistory(
            "√${formatNumber(value)} = ${formatNumber(result)}"
        )

        currentInput =
            formatNumber(result)

        expressionText.text =
            "√${formatNumber(value)}"

        updateDisplay()

        animateResult()
    }

    private fun square() {

        val value =
            currentInput.toDoubleOrNull()
                ?: return

        val result = value * value

        addHistory(
            "${formatNumber(value)}² = ${formatNumber(result)}"
        )

        currentInput =
            formatNumber(result)

        expressionText.text =
            "${formatNumber(value)}²"

        updateDisplay()

        animateResult()
    }

    private fun reciprocal() {

        val value =
            currentInput.toDoubleOrNull()
                ?: return

        if (value == 0.0) {

            Toast.makeText(
                this,
                "Cannot divide by zero",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val result = 1.0 / value

        addHistory(
            "1/${formatNumber(value)} = ${formatNumber(result)}"
        )

        currentInput =
            formatNumber(result)

        expressionText.text =
            "1/${formatNumber(value)}"

        updateDisplay()

        animateResult()
    }

    private fun clearAll() {

        currentInput = "0"

        storedValue = 0.0

        pendingOperator = null

        waitingForOperand = false

        justCalculated = false

        expressionText.text = ""

        updateDisplay()
    }

    private fun backspace() {

        if (waitingForOperand || justCalculated) {
            return
        }

        if (
            currentInput.length <= 1 ||
            (
                currentInput.length == 2 &&
                        currentInput.startsWith("-")
                )
        ) {

            currentInput = "0"

        } else {

            currentInput =
                currentInput.dropLast(1)
        }

        updateDisplay()
    }

    private fun updateDisplay() {

        resultText.text = currentInput

        resultText.animate()
            .scaleX(1.02f)
            .scaleY(1.02f)
            .setDuration(70)
            .withEndAction {

                resultText.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(70)
                    .start()
            }
            .start()
    }

    private fun addHistory(item: String) {

        history.add(0, item)

        if (history.size > 30) {
            history.removeAt(history.lastIndex)
        }

        refreshHistory()
    }

    private fun refreshHistory() {

        /*
         * Safety check.
         * Prevents accidental crashes if this method is ever called
         * before the history panel is created.
         */
        if (!::historyContainer.isInitialized) {
            return
        }

        historyContainer.removeAllViews()

        if (history.isEmpty()) {

            val empty = TextView(this).apply {

                text = "No calculations yet"

                textSize = 15f

                setTextColor(secondaryText)

                gravity = Gravity.CENTER

                setPadding(
                    dp(20),
                    dp(30),
                    dp(20),
                    dp(30)
                )
            }

            historyContainer.addView(empty)

            return
        }

        for (item in history) {

            val row = TextView(this).apply {

                text = item

                textSize = 15f

                setTextColor(white)

                gravity = Gravity.CENTER_VERTICAL

                setPadding(
                    dp(15),
                    dp(12),
                    dp(15),
                    dp(12)
                )

                background = roundedBackground(
                    Color.rgb(27, 30, 48),
                    dp(14).toFloat()
                )

                isClickable = true

                setOnClickListener {

                    val result =
                        item.substringAfterLast("=")
                            .trim()

                    if (result.isNotEmpty()) {

                        currentInput = result

                        pendingOperator = null

                        waitingForOperand = false

                        justCalculated = true

                        updateDisplay()
                    }

                    hideHistory()
                }
            }

            historyContainer.addView(
                row,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(52)
                ).apply {
                    setMargins(
                        0,
                        dp(4),
                        0,
                        dp(4)
                    )
                }
            )
        }
    }

    private fun toggleHistory() {

        if (!::historyScroll.isInitialized) {
            return
        }

        if (historyScroll.visibility == View.VISIBLE) {

            hideHistory()

        } else {

            showHistory()
        }
    }

    private fun showHistory() {

        if (!::historyScroll.isInitialized) {
            return
        }

        refreshHistory()

        historyScroll.visibility = View.VISIBLE

        historyScroll.alpha = 0f

        historyScroll.animate()
            .alpha(1f)
            .setDuration(220)
            .start()
    }

    private fun hideHistory() {

        if (!::historyScroll.isInitialized) {
            return
        }

        historyScroll.animate()
            .alpha(0f)
            .setDuration(160)
            .withEndAction {

                historyScroll.visibility =
                    View.GONE

                historyScroll.alpha = 0f
            }
            .start()
    }

    private fun copyResult() {

        val clipboard =
            getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "Calculator result",
                currentInput
            )
        )

        Toast.makeText(
            this,
            "Result copied: $currentInput",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun animateButton(view: View) {

        val scaleDownX =
            ObjectAnimator.ofFloat(
                view,
                "scaleX",
                1f,
                0.92f,
                1f
            )

        val scaleDownY =
            ObjectAnimator.ofFloat(
                view,
                "scaleY",
                1f,
                0.92f,
                1f
            )

        AnimatorSet().apply {

            playTogether(
                scaleDownX,
                scaleDownY
            )

            duration = 130

            start()
        }
    }

    private fun animateResult() {

        resultText.animate()
            .scaleX(1.08f)
            .scaleY(1.08f)
            .setDuration(120)
            .withEndAction {

                resultText.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(180)
                    .start()
            }
            .start()
    }

    private fun vibrate() {

        try {

            val vibrator =
                getSystemService(
                    Context.VIBRATOR_SERVICE
                ) as Vibrator

            if (
                android.os.Build.VERSION.SDK_INT >= 26
            ) {

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

        } catch (_: Exception) {
            // Haptic feedback must never crash the calculator.
        }
    }

    private fun displayOperator(
        operator: String
    ): String {

        return when (operator) {

            "*" -> "×"

            "/" -> "÷"

            else -> operator
        }
    }

    private fun formatNumber(
        value: Double
    ): String {

        if (!value.isFinite()) {
            return "Error"
        }

        if (value == 0.0) {
            return "0"
        }

        val rounded =
            round(
                value * 1_000_000_000.0
            ) / 1_000_000_000.0

        return if (
            rounded % 1.0 == 0.0
        ) {

            rounded.toLong().toString()

        } else {

            rounded.toString()
        }
    }

    private fun pressedColor(
        color: Int
    ): Int {

        return when (color) {

            operatorColor ->
                operatorPressed

            functionColor ->
                functionPressed

            specialColor ->
                specialPressed

            equalsColor ->
                equalsPressed

            numberColor ->
                numberPressed

            else ->
                Color.rgb(
                    minOf(
                        Color.red(color) + 25,
                        255
                    ),
                    minOf(
                        Color.green(color) + 25,
                        255
                    ),
                    minOf(
                        Color.blue(color) + 25,
                        255
                    )
                )
        }
    }

    private fun roundedBackground(
        color: Int,
        radius: Float
    ): android.graphics.drawable.GradientDrawable {

        return android.graphics.drawable.GradientDrawable().apply {

            setColor(color)

            cornerRadius = radius
        }
    }

    private fun dp(value: Int): Int {

        return (
            value *
                    resources.displayMetrics.density
            ).toInt()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {

        if (
            ::historyScroll.isInitialized &&
            historyScroll.visibility == View.VISIBLE
        ) {

            hideHistory()

        } else {

            super.onBackPressed()
        }
    }

    override fun onDestroy() {

        if (::adWebView.isInitialized) {

            adWebView.stopLoading()
            adWebView.destroy()
        }

        super.onDestroy()
    }
}
