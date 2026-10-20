package com.movtery.zalithlauncher.feature.performance

import android.content.Context
import android.widget.Toast
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.dialog.TipDialog
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.prefs.LauncherPreferences

object PerformanceManager {
    private val OPTIMIZED_G1GC_FLAGS = listOf(
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:+UseG1GC",
        "-XX:+ParallelRefProcEnabled",
        "-XX:MaxGCPauseMillis=35",
        "-XX:+DisableExplicitGC",
        "-XX:+AlwaysPreTouch",
        "-XX:G1NewSizePercent=20",
        "-XX:G1MaxNewSizePercent=40",
        "-XX:G1HeapRegionSize=16M",
        "-XX:G1ReservePercent=15",
        "-XX:InitiatingHeapOccupancyPercent=20",
        "-XX:G1MixedGCCountTarget=4",
        "-XX:+PerfDisableSharedMem"
    )

    @JvmStatic
    fun getRecommendedJvmArgs(existingArgs: String): String {
        val current = existingArgs.trim()
        val builder = StringBuilder(current)
        for (flag in OPTIMIZED_G1GC_FLAGS) {
            val prefix = flag.substringBefore("=")
            if (!current.contains(prefix)) {
                if (builder.isNotEmpty() && !builder.endsWith(" ")) {
                    builder.append(" ")
                }
                builder.append(flag)
            }
        }
        return builder.toString().trim()
    }

    @JvmStatic
    fun getRecommendedResolutionRatio(context: Context): Int {
        val totalRam = Tools.getTotalDeviceMemory(context)
        return when {
            totalRam < 3072 -> 75
            totalRam < 6144 -> 85
            else -> 100
        }
    }

    @JvmStatic
    fun applyLagFreePreset(context: Context): String {
        val bestRam = LauncherPreferences.findBestRAMAllocation(context)
        val bestRes = getRecommendedResolutionRatio(context)
        val mergedJvmArgs = getRecommendedJvmArgs(AllSettings.javaArgs.getValue())

        AllSettings.ramAllocation.value.put(bestRam).save()
        AllSettings.resolutionRatio.put(bestRes).save()
        AllSettings.javaArgs.put(mergedJvmArgs).save()
        AllSettings.bigCoreAffinity.put(true).save()
        AllSettings.sustainedPerformance.put(true).save()
        AllSettings.animationSpeed.put(280).save()

        return context.getString(
            R.string.furina_boost_applied_summary,
            bestRam,
            bestRes
        )
    }

    @JvmStatic
    fun showOptimizationDialog(context: Context, onOpenModHub: Runnable) {
        val bestRam = LauncherPreferences.findBestRAMAllocation(context)
        val bestRes = getRecommendedResolutionRatio(context)
        TipDialog.Builder(context)
            .setTitle(R.string.furina_boost_title)
            .setMessage(context.getString(R.string.furina_boost_dialog_msg, bestRam, bestRes))
            .setConfirm(R.string.furina_boost_apply_btn)
            .setCancel(R.string.furina_boost_open_mods_btn)
            .setConfirmClickListener {
                val summary = applyLagFreePreset(context)
                Toast.makeText(context, summary, Toast.LENGTH_LONG).show()
            }
            .setCancelClickListener {
                val summary = applyLagFreePreset(context)
                Toast.makeText(context, summary, Toast.LENGTH_SHORT).show()
                onOpenModHub.run()
            }
            .showDialog()
    }
}
