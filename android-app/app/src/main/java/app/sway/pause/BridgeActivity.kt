package app.sway.pause

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class BridgeActivity : Activity() {
    private var targetPackage: String = ""
    private var targetLabel: String = "your app"
    private lateinit var phaseLabel: TextView
    private lateinit var countLabel: TextView
    private lateinit var continueButton: TextView
    private var phaseIndex = 0
    private var secondsLeft = 4
    private val phases = listOf("BREATHE IN" to 4, "HOLD SOFTLY" to 2, "BREATHE OUT" to 6)
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            if (isFinishing) return
            secondsLeft -= 1
            if (secondsLeft <= 0) {
                phaseIndex = (phaseIndex + 1) % phases.size
                secondsLeft = phases[phaseIndex].second
            }
            phaseLabel.text = phases[phaseIndex].first
            countLabel.text = secondsLeft.toString()
            handler.postDelayed(this, 1_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFFEEEFE3.toInt()
        window.navigationBarColor = 0xFFEEEFE3.toInt()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE).orEmpty()
        targetLabel = intent.getStringExtra(EXTRA_TARGET_LABEL) ?: "your app"
        setContentView(buildBridge())
        handler.postDelayed(tick, 1_000L)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE).orEmpty()
        targetLabel = intent.getStringExtra(EXTRA_TARGET_LABEL) ?: "your app"
        updateContinueButton()
    }

    private fun buildBridge(): View {
        val scroll = android.widget.ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(0xFFEEEFE3.toInt())
        }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(22), dp(24), dp(18))
            setBackgroundColor(0xFFEEEFE3.toInt())
        }
        scroll.addView(page)
        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val mark = TextView(this).apply {
            text = "∿"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(0xFF496337.toInt())
            background = bg(0xFFE0EBCF.toInt(), 12)
        }
        brand.addView(mark, LinearLayout.LayoutParams(dp(38), dp(38)))
        brand.addView(text("sway.", 21, 0xFF242831.toInt(), true).apply { setPadding(dp(9), 0, 0, 0) })
        page.addView(brand, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)))

        val middle = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(23), 0, dp(10))
        }
        val overline = text("✳   A MOMENT FOR YOU", 10, 0xFF6C8157.toInt(), true).apply { letterSpacing = .12f }
        middle.addView(overline, centerParams(bottom = 18))

        val orb = FrameLayout(this).apply { contentDescription = "A slowly breathing circle. Breathe in, hold, then breathe out." }
        orb.addView(BreathingOrb(this), FrameLayout.LayoutParams(dp(164), dp(164), Gravity.CENTER))
        val orbText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        phaseLabel = text(phases[0].first, 9, 0xFF405341.toInt(), true).apply { gravity = Gravity.CENTER; letterSpacing = .1f }
        countLabel = text("4", 39, 0xFF34443B.toInt(), true).apply { gravity = Gravity.CENTER; setPadding(0, dp(1), 0, dp(1)) }
        orbText.addView(phaseLabel)
        orbText.addView(countLabel)
        orbText.addView(text("seconds for you", 9, 0xFF667461.toInt(), false).apply { gravity = Gravity.CENTER })
        orb.addView(orbText, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val pulseX = ObjectAnimator.ofFloat(orb, View.SCALE_X, .94f, 1.04f).apply {
            duration = 4_000L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val pulseY = ObjectAnimator.ofFloat(orb, View.SCALE_Y, .94f, 1.04f).apply {
            duration = 4_000L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        pulseX.start(); pulseY.start()
        middle.addView(orb, centerParams(bottom = 22))
        middle.addView(text("Before the scroll,\ncome back to you.", 26, 0xFF28312E.toInt(), true).apply {
            gravity = Gravity.CENTER
            letterSpacing = -.025f
            setLineSpacing(dp(2).toFloat(), 1f)
        }, centerParams())
        middle.addView(text("Take one slow breath. What were you hoping to find?", 13, 0xFF777F78.toInt(), false).apply {
            gravity = Gravity.CENTER
            setLineSpacing(dp(3).toFloat(), 1f)
        }, centerParams(top = 10, bottom = 20))
        page.addView(middle, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        continueButton = TextView(this).apply {
            textSize = 14f
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = bg(0xFF263531.toInt(), 14)
            setPadding(dp(16), 0, dp(16), 0)
            setOnClickListener { continueToTarget() }
            contentDescription = "Continue to $targetLabel"
        }
        continueButton.text = "Continue to $targetLabel     ↗"
        page.addView(continueButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)))

        val stepAway = TextView(this).apply {
            text = "Not now — I choose me"
            textSize = 13f
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            setTextColor(0xFF5F6C5E.toInt())
            gravity = Gravity.CENTER
            background = bg(0xFFFFFFFF.toInt(), 14, 0xFFD7DDD0.toInt())
            setOnClickListener { chooseMe() }
        }
        page.addView(stepAway, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(9) })
        page.addView(text("No wrong answer. Just your choice.", 10, 0xFF8D9489.toInt(), false).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(37)))
        return scroll
    }

    private fun updateContinueButton() {
        if (!::continueButton.isInitialized) return
        continueButton.text = "Continue to $targetLabel     ↗"
        continueButton.contentDescription = "Continue to $targetLabel"
    }

    private fun continueToTarget() {
        if (targetPackage.isBlank()) {
            finish()
            return
        }
        val launch = packageManager.getLaunchIntentForPackage(targetPackage)
        if (launch == null) {
            finish()
            return
        }
        PauseAccessibilityService.allowOneLaunch(targetPackage)
        runCatching {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            startActivity(launch)
        }
        finish()
    }

    private fun chooseMe() {
        runCatching {
            startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        finish()
    }

    @Deprecated("Handled as a deliberate 'not now' choice")
    override fun onBackPressed() {
        chooseMe()
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean): TextView = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        typeface = android.graphics.Typeface.create("sans-serif${if (bold) "-medium" else ""}", if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        includeFontPadding = true
    }

    private fun centerParams(top: Int = 0, bottom: Int = 0) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        gravity = Gravity.CENTER_HORIZONTAL
        topMargin = dp(top)
        bottomMargin = dp(bottom)
    }

    private fun bg(color: Int, radius: Int, stroke: Int? = null) = android.graphics.drawable.GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private class BreathingOrb(context: android.content.Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val radius = minOf(width, height) * .43f
            paint.shader = null
            paint.color = 0x1B8DA65D
            canvas.drawCircle(cx, cy, radius + dp(17), paint)
            paint.color = 0x238DA65D
            canvas.drawCircle(cx, cy, radius + dp(8), paint)
            paint.shader = RadialGradient(cx - radius * .24f, cy - radius * .28f, radius * 1.8f,
                intArrayOf(0xFFF5F8D9.toInt(), 0xFFCDE4A2.toInt(), 0xFF97B67B.toInt()),
                floatArrayOf(0f, .52f, 1f), Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, radius, paint)
            paint.shader = null
        }
        private fun dp(value: Int): Float = value * resources.displayMetrics.density
    }

    companion object {
        const val EXTRA_TARGET_PACKAGE = "target_package"
        const val EXTRA_TARGET_LABEL = "target_label"
    }
}
