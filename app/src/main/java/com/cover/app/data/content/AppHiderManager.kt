package com.cover.app.data.content

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import com.cover.app.data.repository.VaultRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App hider functionality for the custom launcher.
 * 
 * This class manages which apps are visible in the custom launcher.
 * Apps are NOT actually disabled - they are simply filtered from the launcher view
 * based on whether the vault is locked or unlocked.
 * 
 * When the vault is locked, hidden apps don't appear in the launcher grid.
 * When the vault is unlocked (via PIN), all apps become visible.
 */
@Singleton
class AppHiderManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultRepository: VaultRepository
) {
    private val packageManager = context.packageManager

    /**
     * Get list of all installed apps that can be launched (excluding system apps).
     * These are apps that appear in the launcher.
     */
    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        packageManager.queryIntentActivities(launcherIntent, 0)
            .filter { resolveInfo ->
                // Exclude our own app to avoid recursion
                resolveInfo.activityInfo.packageName != context.packageName
            }
            .map { resolveInfo ->
                AppInfo(
                    packageName = resolveInfo.activityInfo.packageName,
                    name = resolveInfo.loadLabel(packageManager).toString(),
                    icon = resolveInfo.loadIcon(packageManager),
                    activityName = resolveInfo.activityInfo.name,
                    isSystemApp = isSystemApp(resolveInfo.activityInfo.packageName)
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    /**
     * Get filtered apps based on vault lock state.
     * When vault is locked, hidden apps are filtered out.
     * When vault is unlocked, all apps are shown.
     */
    suspend fun getVisibleApps(
        vaultId: String,
        isVaultLocked: Boolean
    ): List<AppInfo> = withContext(Dispatchers.IO) {
        val allApps = getInstalledApps()
        
        if (!isVaultLocked) {
            // Vault unlocked - show all apps
            return@withContext allApps
        }
        
        // Vault locked - filter out hidden apps
        val hiddenPackages = vaultRepository.getHiddenPackageNames(vaultId).toSet()
        return@withContext allApps.filter { app ->
            app.packageName !in hiddenPackages
        }
    }

    /**
     * Hide an app from the launcher (when vault is locked).
     * This adds the app to the hidden apps list in the database.
     */
    suspend fun hideApp(vaultId: String, app: AppInfo): Result<Unit> {
        return vaultRepository.hideApp(vaultId, app.packageName, app.name)
    }

    /**
     * Unhide an app (make it visible even when vault is locked).
     */
    suspend fun unhideApp(packageName: String) {
        vaultRepository.unhideApp(packageName)
    }

    /**
     * Get the list of currently hidden apps for a vault.
     */
    fun getHiddenApps(vaultId: String): Flow<List<HiddenApp>> {
        return vaultRepository.getHiddenAppsByVault(vaultId).map { entities ->
            entities.map { entity ->
                HiddenApp(
                    id = entity.id,
                    packageName = entity.packageName,
                    appName = entity.appName
                )
            }
        }
    }

    /**
     * Check if app hiding is supported on this device.
     * This is always true since we use the custom launcher approach.
     */
    fun isAppHidingSupported(): Boolean {
        return true
    }

    /**
     * Launch a specific app by package name.
     */
    fun launchApp(packageName: String, activityName: String) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(packageName)
            setClassName(packageName, activityName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Check if an app is a system app.
     */
    private fun isSystemApp(packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    data class AppInfo(
        val packageName: String,
        val name: String,
        val icon: Drawable,
        val activityName: String,
        val isSystemApp: Boolean
    )

    data class HiddenApp(
        val id: String,
        val packageName: String,
        val appName: String
    )
}
