package com.reelockapp.blocker

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tracks the streak (consecutive days the service has been active) and
 * simple counters (blocks stopped, minutes saved), all in SharedPreferences.
 *
 * "Minutes saved" is a rough motivating estimate (assumes each interruption
 * would otherwise have turned into ~90 seconds of scrolling) — not a
 * measured value.
 */
object StatsManager {
    private const val PREFS_NAME = "reels_blocker_stats"
    private const val KEY_STREAK = "streak_days"
    private const val KEY_LAST_ACTIVE_DATE = "last_active_date"
    private const val KEY_TOTAL_BLOCKS = "total_blocks"
    private const val KEY_TODAY_BLOCKS = "today_blocks"
    private const val KEY_TODAY_DATE = "today_blocks_date"

    private const val ESTIMATED_SECONDS_SAVED_PER_BLOCK = 90

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private fun todayString(): String = dateFormat.format(Date())
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Call once when the app opens, to roll the streak forward or reset it. */
    fun refreshStreakForToday(context: Context) {
        val p = prefs(context)
        val today = todayString()
        val lastActive = p.getString(KEY_LAST_ACTIVE_DATE, null)

        if (lastActive == null) {
            p.edit().putString(KEY_LAST_ACTIVE_DATE, today).putInt(KEY_STREAK, 1).apply()
            return
        }
        if (lastActive == today) return // already counted today

        val daysBetween = daysBetween(lastActive, today)
        val newStreak = if (daysBetween == 1) getStreak(context) + 1 else 1
        p.edit().putString(KEY_LAST_ACTIVE_DATE, today).putInt(KEY_STREAK, newStreak).apply()
    }

    private fun daysBetween(startDate: String, endDate: String): Int {
        val start = dateFormat.parse(startDate) ?: return 999
        val end = dateFormat.parse(endDate) ?: return 999
        return ((end.time - start.time) / (1000 * 60 * 60 * 24)).toInt()
    }

    fun getStreak(context: Context): Int = prefs(context).getInt(KEY_STREAK, 1)

    fun getTotalBlocks(context: Context): Int = prefs(context).getInt(KEY_TOTAL_BLOCKS, 0)

    fun getTodayBlocks(context: Context): Int {
        val p = prefs(context)
        return if (p.getString(KEY_TODAY_DATE, null) == todayString()) p.getInt(KEY_TODAY_BLOCKS, 0) else 0
    }

    fun getMinutesSavedToday(context: Context): Int =
        (getTodayBlocks(context) * ESTIMATED_SECONDS_SAVED_PER_BLOCK) / 60

    /** Called by the accessibility service every time it backs out of Reels/Shorts. */
    fun recordBlock(context: Context) {
        val p = prefs(context)
        val today = todayString()
        val todayCount = if (p.getString(KEY_TODAY_DATE, null) == today) p.getInt(KEY_TODAY_BLOCKS, 0) + 1 else 1
        p.edit()
            .putInt(KEY_TOTAL_BLOCKS, getTotalBlocks(context) + 1)
            .putString(KEY_TODAY_DATE, today)
            .putInt(KEY_TODAY_BLOCKS, todayCount)
            .apply()
    }

    /** Growth stage: 0 = seed, 1 = sprout, 2 = sapling, 3 = tree, based on streak length. */
    fun getGrowthStage(context: Context): Int = when (val streak = getStreak(context)) {
        in 0..2 -> 0
        in 3..9 -> 1
        in 10..29 -> 2
        else -> 3
    }

    fun getDaysUntilNextStage(context: Context): Int = when (val streak = getStreak(context)) {
        in 0..2 -> 3 - streak
        in 3..9 -> 10 - streak
        in 10..29 -> 30 - streak
        else -> 0
    }

    /** Streak value at which the current stage started, for progress-ring math. */
    fun getStageBounds(context: Context): Pair<Int, Int> = when (getGrowthStage(context)) {
        0 -> 0 to 3
        1 -> 3 to 10
        2 -> 10 to 30
        else -> 30 to 30
    }
}
