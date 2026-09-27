package com.reelockapp.blocker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var enableButton: Button
    private lateinit var batteryWarningCard: LinearLayout
    private lateinit var progressRing: CircularProgressView
    private lateinit var growthImage: ImageView
    private lateinit var stageLabel: TextView
    private lateinit var streakText: TextView
    private lateinit var nextStageText: TextView
    private lateinit var streakStatValue: TextView
    private lateinit var blocksStatValue: TextView
    private lateinit var minutesStatValue: TextView
    private lateinit var levelChip: TextView
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        enableButton = findViewById(R.id.enableButton)
        batteryWarningCard = findViewById(R.id.batteryWarningCard)
        progressRing = findViewById(R.id.progressRing)
        growthImage = findViewById(R.id.growthImage)
        stageLabel = findViewById(R.id.stageLabel)
        streakText = findViewById(R.id.streakText)
        nextStageText = findViewById(R.id.nextStageText)
        streakStatValue = findViewById(R.id.streakStatValue)
        blocksStatValue = findViewById(R.id.blocksStatValue)
        minutesStatValue = findViewById(R.id.minutesStatValue)
        levelChip = findViewById(R.id.levelChip)
        bottomNav = findViewById(R.id.bottomNav)

        enableButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        batteryWarningCard.setOnClickListener {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }

        // "Stats" currently lives on this same Home screen; a dedicated
        // history/journal screen is a natural next step to build later.
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_stats -> true
                R.id.nav_settings -> {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    false
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        StatsManager.refreshStreakForToday(this)
        updateStatus()
        updateBatteryWarning()
        updateGrowth()
        updateStats()
    }

    private fun updateStatus() {
        val enabled = isAccessibilityServiceEnabled()
        statusText.text = if (enabled) getString(R.string.status_active) else getString(R.string.status_inactive)
        enableButton.text = if (enabled) getString(R.string.button_open_settings) else getString(R.string.button_enable)
    }

    private fun updateBatteryWarning() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        val ignoringOptimizations = pm.isIgnoringBatteryOptimizations(packageName)
        batteryWarningCard.visibility = if (ignoringOptimizations) View.GONE else View.VISIBLE
    }

    private fun updateGrowth() {
        val streak = StatsManager.getStreak(this)
        val stage = StatsManager.getGrowthStage(this)
        val daysToNext = StatsManager.getDaysUntilNextStage(this)
        val stageNames = resources.getStringArray(R.array.growth_stage_names)

        levelChip.text = getString(R.string.level_format, stage + 1)
        stageLabel.text = stageNames.getOrElse(stage) { stageNames.last() }

        val stageDrawables = intArrayOf(
            R.drawable.ic_stage_seed,
            R.drawable.ic_stage_sprout,
            R.drawable.ic_stage_sapling,
            R.drawable.ic_stage_tree
        )
        growthImage.setImageResource(stageDrawables.getOrElse(stage) { stageDrawables.last() })

        streakText.text = resources.getQuantityString(R.plurals.streak_days, streak, streak)

        nextStageText.text = if (daysToNext > 0) {
            val nextName = stageNames.getOrElse(stage + 1) { stageNames.last() }
            resources.getQuantityString(R.plurals.grows_into_in_days, daysToNext, nextName, daysToNext)
        } else {
            getString(R.string.fully_grown)
        }

        val (stageStart, stageEnd) = StatsManager.getStageBounds(this)
        progressRing.progress = if (stageEnd == stageStart) 1f else
            ((streak - stageStart).toFloat() / (stageEnd - stageStart).toFloat()).coerceIn(0f, 1f)
    }

    private fun updateStats() {
        streakStatValue.text = StatsManager.getStreak(this).toString()
        blocksStatValue.text = StatsManager.getTodayBlocks(this).toString()
        minutesStatValue.text = "${StatsManager.getMinutesSavedToday(this)}m"
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = "$packageName/${ReelsBlockerService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            if (colonSplitter.next().equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}
