package app.sway.pause

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextWatcher

private const val PAPER = 0xFFF7F7F3.toInt()
private const val INK = 0xFF242831.toInt()
private const val MUTED = 0xFF777E7C.toInt()
private const val GREEN = 0xFFC8F36C.toInt()
private const val GREEN_DARK = 0xFF496337.toInt()
private const val LINE = 0xFFE7EAE2.toInt()
private const val PREF_TARGET = "selected-target-packages"

class MainActivity : Activity() {
    private data class AppEntry(val packageName: String, val label: String, val icon: android.graphics.drawable.Drawable)

    private lateinit var appList: LinearLayout
    private lateinit var searchField: EditText
    private lateinit var countLabel: TextView
    private lateinit var serviceButton: Button
    private lateinit var serviceStatus: TextView
    private var apps: List<AppEntry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = PAPER
        window.navigationBarColor = PAPER
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        apps = loadLaunchableApps()
        setContentView(buildScreen())
        renderApps()
        updateServiceStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::serviceStatus.isInitialized) updateServiceStatus()
        PauseAccessibilityService.refreshTargetFilter()
    }

    private fun buildScreen(): View {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(PAPER)
            isFillViewport = true
            clipToPadding = false
        }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(25), dp(22), dp(32))
        }
        scroll.addView(page)

        val brandRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val mark = TextView(this).apply {
            text = "∿"
            textSize = 27f
            setTextColor(GREEN_DARK)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            background = rounded(0xFFEAF2DF.toInt(), 14)
        }
        brandRow.addView(mark, LinearLayout.LayoutParams(dp(42), dp(42)))
        val brand = text("sway.", 23, INK, true).apply { setPadding(dp(10), 0, 0, 0) }
        brandRow.addView(brand)
        val free = text("FREE · NO APP LIMIT", 9, GREEN_DARK, true).apply {
            gravity = Gravity.CENTER
            background = rounded(0xFFEAF2DF.toInt(), 18)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        brandRow.addView(free, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.CENTER_VERTICAL; leftMargin = dp(12) })
        page.addView(brandRow)

        page.addView(kicker("YOUR ATTENTION, ON YOUR TERMS"), margin(top = 34, bottom = 9))
        page.addView(text("A small pause.\nA more you choice.", 31, INK, true).apply { letterSpacing = -0.035f })
        page.addView(text("Choose the apps that pull you into autopilot. Sway will offer a breath before they open—not lock you out.", 14, MUTED, false).apply { setLineSpacing(dp(4).toFloat(), 1f) }, margin(top = 10, bottom = 19))

        val serviceCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(0xFF202C36.toInt(), 22)
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        serviceCard.addView(kicker("PAUSE DETECTION", 0xFFC8E49C.toInt()))
        serviceStatus = text("Not connected yet", 18, Color.WHITE, true).apply { setPadding(0, dp(9), 0, dp(5)) }
        serviceCard.addView(serviceStatus)
        serviceCard.addView(text("Android asks you to enable Sway's accessibility service in Settings. You can turn it off whenever you want.", 12, 0xFFC6CDCC.toInt(), false).apply { setLineSpacing(dp(3).toFloat(), 1f) })
        serviceButton = Button(this).apply {
            text = "Enable pause detection"
            textSize = 13f
            isAllCaps = false
            setTextColor(0xFF263226.toInt())
            background = rounded(GREEN, 13)
            setPadding(dp(15), 0, dp(15), 0)
            setOnClickListener { explainAndOpenAccessibilitySettings() }
        }
        serviceCard.addView(serviceButton, margin(top = 15))
        page.addView(serviceCard)

        val disclosure = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            background = rounded(0xFFEFF3E8.toInt(), 14)
            setPadding(dp(13), dp(13), dp(13), dp(13))
        }
        disclosure.addView(text("ⓘ", 16, GREEN_DARK, true), LinearLayout.LayoutParams(dp(25), ViewGroup.LayoutParams.WRAP_CONTENT))
        disclosure.addView(text("Privacy, plainly: Sway checks only the package name of the app opening, and only to compare it with your selected list. It does not read screen text, capture content, or send data anywhere. Your choices stay on this phone.", 11, 0xFF687663.toInt(), false).apply { setLineSpacing(dp(3).toFloat(), 1f) })
        page.addView(disclosure, margin(top = 13, bottom = 25))

        val sectionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val sectionHeading = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        sectionHeading.addView(kicker("YOUR LITTLE LINEUP", 0xFF87927F.toInt()))
        sectionHeading.addView(text("Choose your apps", 21, INK, true).apply { setPadding(0, dp(5), 0, 0) })
        sectionRow.addView(sectionHeading, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        countLabel = text("0 selected", 11, GREEN_DARK, true).apply {
            background = rounded(0xFFEAF2DF.toInt(), 18)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        sectionRow.addView(countLabel)
        page.addView(sectionRow, margin(bottom = 13))

        searchField = EditText(this).apply {
            hint = "Search installed apps"
            textSize = 14f
            setTextColor(INK)
            setHintTextColor(0xFF9A9F99.toInt())
            setSingleLine(true)
            background = rounded(Color.WHITE, 13, LINE)
            setPadding(dp(14), 0, dp(14), 0)
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }
        page.addView(searchField, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply { bottomMargin = dp(10) })
        appList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(appList)

        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = renderApps()
            override fun afterTextChanged(s: Editable?) = Unit
        })

        page.addView(text("A gentle pause, not a hard block. You stay in control of every choice.", 11, MUTED, false).apply {
            gravity = Gravity.CENTER
            setLineSpacing(dp(3).toFloat(), 1f)
        }, margin(top = 20, bottom = 8))
        return scroll
    }

    private fun loadLaunchableApps(): List<AppEntry> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val entries = packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .asSequence()
            .filter { resolve ->
                val candidate = resolve.activityInfo?.packageName.orEmpty()
                candidate != packageName && candidate !in setOf(
                    "com.android.settings",
                    "com.android.systemui",
                    "com.android.permissioncontroller",
                    "com.google.android.permissioncontroller"
                )
            }
            .distinctBy { it.activityInfo?.packageName }
            .mapNotNull { resolve ->
                val info = resolve.activityInfo ?: return@mapNotNull null
                runCatching {
                    AppEntry(info.packageName, resolve.loadLabel(packageManager).toString(), resolve.loadIcon(packageManager))
                }.getOrNull()
            }
            .sortedBy { it.label.lowercase() }
            .toList()
        return entries
    }

    private fun renderApps() {
        if (!::appList.isInitialized || !::searchField.isInitialized) return
        appList.removeAllViews()
        val selected = AppPrefs.targets(this)
        countLabel.text = "${selected.size} selected"
        val query = searchField.text?.toString()?.trim()?.lowercase().orEmpty()
        val visible = apps.filter { it.label.lowercase().contains(query) }
        if (visible.isEmpty()) {
            appList.addView(text("No matching apps found.", 13, MUTED, false).apply { gravity = Gravity.CENTER; setPadding(0, dp(22), 0, dp(22)) })
            return
        }
        visible.forEachIndexed { index, app ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(Color.WHITE, 15, LINE)
                setPadding(dp(12), dp(10), dp(10), dp(10))
                contentDescription = app.label
            }
            val appIcon = ImageView(this).apply {
                setImageDrawable(app.icon)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            row.addView(appIcon, LinearLayout.LayoutParams(dp(42), dp(42)))
            val copy = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(11), 0, dp(8), 0)
            }
            copy.addView(text(app.label, 14, INK, true))
            copy.addView(text("Pause before opening", 11, MUTED, false).apply { setPadding(0, dp(4), 0, 0) })
            row.addView(copy, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val toggle = Switch(this).apply {
                isChecked = app.packageName in selected
                contentDescription = "Pause before opening ${app.label}"
                buttonTintList = ColorStateList.valueOf(if (isChecked) GREEN_DARK else 0xFFB8BDB4.toInt())
                setOnCheckedChangeListener { _, checked ->
                    AppPrefs.setTarget(this@MainActivity, app.packageName, checked)
                    countLabel.text = "${AppPrefs.targets(this@MainActivity).size} selected"
                }
            }
            row.addView(toggle)
            appList.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = if (index == visible.lastIndex) 0 else dp(8)
            })
        }
    }

    private fun explainAndOpenAccessibilitySettings() {
        if (isPauseServiceEnabled()) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        val proceed = {
            AppPrefs.acceptDisclosure(this)
            runCatching { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                .onFailure { AlertDialog.Builder(this).setMessage("Open Android Settings → Accessibility → Sway app pause, then turn it on.").setPositiveButton("OK", null).show() }
        }
        if (AppPrefs.disclosureAccepted(this)) {
            proceed()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("How Sway uses Accessibility")
            .setMessage("Sway needs Android's Accessibility Service so it can notice when an app you selected comes to the foreground and show your pause screen. It reads only the app's package name. Sway does not read screen text, capture content, collect app activity, or send data to a server. The service is optional and you can switch it off in Android Settings at any time.")
            .setNegativeButton("Not now", null)
            .setPositiveButton("I understand · Continue") { _, _ -> proceed() }
            .show()
    }

    private fun isPauseServiceEnabled(): Boolean {
        val expected = ComponentName(this, PauseAccessibilityService::class.java)
        val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val active = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return active.any { it.resolveInfo.serviceInfo.let { info -> ComponentName(info.packageName, info.name) == expected } }
    }

    private fun updateServiceStatus() {
        if (!::serviceStatus.isInitialized) return
        val enabled = isPauseServiceEnabled()
        serviceStatus.text = if (enabled) "Pause detection is on" else "Not connected yet"
        serviceButton.text = if (enabled) "Manage in Accessibility settings" else "Enable pause detection"
        serviceStatus.setTextColor(if (enabled) GREEN else Color.WHITE)
    }

    private fun kicker(label: String, color: Int = GREEN_DARK): TextView = text(label, 10, color, true).apply { letterSpacing = 0.12f }

    private fun text(value: String, size: Int, color: Int, bold: Boolean): TextView = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        typeface = Typeface.create("sans-serif${if (bold) "-medium" else ""}", if (bold) Typeface.BOLD else Typeface.NORMAL)
        includeFontPadding = true
    }

    private fun rounded(fill: Int, radius: Int, stroke: Int? = null): GradientDrawable = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun margin(top: Int = 0, bottom: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(top)
            bottomMargin = dp(bottom)
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
