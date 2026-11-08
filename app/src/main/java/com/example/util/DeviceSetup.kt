package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Helpers that keep the widget alive on aggressive phone brands:
 * battery-optimisation status and best-effort links to each brand's autostart screen.
 * Brand screens are not part of the public Android API, so every link is tried inside
 * try/catch and we always fall back to the standard "App info" page.
 */
object DeviceSetup {

    fun isBatteryUnrestricted(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Shows the system "allow unrestricted battery?" dialog, falling back to the App info page. */
    fun requestBatteryExemption(context: Context) {
        val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:${context.packageName}"))
        launchFirstThatWorks(context, listOf(request, appDetails(context)))
    }

    /** Opens the brand-specific autostart page if we know it, otherwise the App info page. */
    fun openAutostartSettings(context: Context): Boolean =
        launchFirstThatWorks(context, oemIntents() + appDetails(context))

    fun autostartHint(): String = when (Build.MANUFACTURER.lowercase()) {
        "xiaomi", "redmi", "poco" ->
            "Xiaomi / Redmi / POCO: turn Autostart ON for this app and set Battery saver to \"No restrictions\"."
        "oppo", "realme" ->
            "Oppo / Realme: allow Auto-launch and Background activity for this app in App info → Battery usage."
        "vivo", "iqoo" ->
            "Vivo / iQOO: allow Autostart and set Background power consumption to \"High\" or \"Allow\"."
        "oneplus" ->
            "OnePlus: allow Auto-launch and set Battery optimisation to \"Don't optimise\" for this app."
        "samsung" ->
            "Samsung: add this app to Settings → Battery → Background usage limits → Never sleeping apps."
        "huawei", "honor" ->
            "Huawei / Honor: App launch → this app → Manage manually → allow Auto-launch and Run in background."
        else ->
            "In Settings → Apps → this app → Battery, choose \"Unrestricted\" so updates are not paused."
    }

    private fun appDetails(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:${context.packageName}"))

    private fun component(pkg: String, cls: String): Intent =
        Intent().setComponent(ComponentName(pkg, cls))

    private fun oemIntents(): List<Intent> = when (Build.MANUFACTURER.lowercase()) {
        "xiaomi", "redmi", "poco" -> listOf(
            component("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
        )
        "oppo", "realme" -> listOf(
            component("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            component("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            component("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
        )
        "vivo", "iqoo" -> listOf(
            component("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            component("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
        )
        "oneplus" -> listOf(
            component("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
            component("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
        )
        "samsung" -> listOf(
            component("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")
        )
        "huawei", "honor" -> listOf(
            component("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
        )
        else -> emptyList()
    }

    private fun launchFirstThatWorks(context: Context, intents: List<Intent>): Boolean {
        for (intent in intents) {
            try {
                if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (_: Exception) {
                // try the next candidate
            }
        }
        return false
    }
}
