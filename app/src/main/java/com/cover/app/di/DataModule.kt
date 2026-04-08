package com.cover.app.di

import android.content.Context
import com.cover.app.data.admob.AdMobManager
import com.cover.app.data.billing.BillingManager
import com.cover.app.data.local.CoverDatabase
import com.cover.app.data.local.CoverDatabaseProvider
import com.cover.app.data.local.dao.HiddenAppDao
import com.cover.app.data.local.dao.HiddenItemDao
import com.cover.app.data.local.dao.IntruderLogDao
import com.cover.app.data.local.dao.PremiumStatusDao
import com.cover.app.data.local.dao.VaultDao
import com.cover.app.data.remoteconfig.RemoteConfigManager
import com.cover.app.data.remoteconfig.PromotionManager
import com.cover.app.data.remoteconfig.ABTestManager
import com.cover.app.data.remoteconfig.ThemeManager
import com.cover.app.data.remoteconfig.TutorialManager
import com.cover.app.data.remoteconfig.InAppMessageManager
import com.cover.app.data.security.EncryptionManager
import com.cover.app.core.util.PerformanceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(provider: CoverDatabaseProvider): CoverDatabase {
        return provider.getDatabase()
    }

    @Provides
    fun provideVaultDao(database: CoverDatabase): VaultDao {
        return database.vaultDao()
    }

    @Provides
    fun provideHiddenItemDao(database: CoverDatabase): HiddenItemDao {
        return database.hiddenItemDao()
    }

    @Provides
    fun provideIntruderLogDao(database: CoverDatabase): IntruderLogDao {
        return database.intruderLogDao()
    }

    @Provides
    fun providePremiumStatusDao(database: CoverDatabase): PremiumStatusDao {
        return database.premiumStatusDao()
    }

    @Provides
    fun provideHiddenAppDao(database: CoverDatabase): HiddenAppDao {
        return database.hiddenAppDao()
    }

    @Provides
    @Singleton
    fun provideBillingManager(@ApplicationContext context: Context): BillingManager {
        return BillingManager(context)
    }

    @Provides
    @Singleton
    fun provideAdMobManager(@ApplicationContext context: Context): AdMobManager {
        return AdMobManager(context)
    }

    @Provides
    @Singleton
    fun provideRemoteConfigManager(@ApplicationContext context: Context): RemoteConfigManager {
        return RemoteConfigManager(context)
    }

    @Provides
    @Singleton
    fun providePromotionManager(
        @ApplicationContext context: Context,
        remoteConfigManager: RemoteConfigManager,
        premiumRepository: com.cover.app.data.repository.PremiumRepository,
        adMobManager: AdMobManager
    ): PromotionManager {
        return PromotionManager(context, remoteConfigManager, premiumRepository, adMobManager)
    }

    @Provides
    @Singleton
    fun provideEncryptionManager(@ApplicationContext context: Context): EncryptionManager {
        return EncryptionManager(context)
    }

    // Note: SecureDataStore (data.security) has @Inject constructor - Hilt auto-provides it
    // SecureStorageManager (data.storage) is also auto-injected via @Inject constructor
    // No @Provides needed for either - prevents binding conflicts

    @Provides
    @Singleton
    fun provideABTestManager(
        @ApplicationContext context: Context,
        remoteConfigManager: RemoteConfigManager
    ): ABTestManager {
        return ABTestManager(context, remoteConfigManager)
    }

    @Provides
    @Singleton
    fun provideThemeManager(
        @ApplicationContext context: Context,
        remoteConfigManager: RemoteConfigManager
    ): ThemeManager {
        return ThemeManager(context, remoteConfigManager)
    }

    @Provides
    @Singleton
    fun provideInAppMessageManager(
        @ApplicationContext context: Context,
        remoteConfigManager: RemoteConfigManager
    ): InAppMessageManager {
        return InAppMessageManager(context, remoteConfigManager)
    }

    @Provides
    @Singleton
    fun provideTutorialManager(
        @ApplicationContext context: Context,
        remoteConfigManager: RemoteConfigManager,
        inAppMessageManager: InAppMessageManager
    ): TutorialManager {
        return TutorialManager(context, remoteConfigManager, inAppMessageManager)
    }

    @Provides
    @Singleton
    fun providePerformanceManager(
        @ApplicationContext context: Context,
        encryptionManager: com.cover.app.data.security.EncryptionManager
    ): PerformanceManager {
        return PerformanceManager(context, encryptionManager)
    }
}
