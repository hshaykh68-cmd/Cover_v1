package com.cover.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cover.app.data.local.dao.HiddenAppDao
import com.cover.app.data.local.dao.HiddenItemDao
import com.cover.app.data.local.dao.IntruderLogDao
import com.cover.app.data.local.dao.PremiumStatusDao
import com.cover.app.data.local.dao.VaultDao
import com.cover.app.data.local.entity.HiddenAppEntity
import com.cover.app.data.local.entity.HiddenItemEntity
import com.cover.app.data.local.entity.IntruderLogEntity
import com.cover.app.data.local.entity.PremiumStatusEntity
import com.cover.app.data.local.entity.VaultEntity
import com.cover.app.data.security.EncryptionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Inject
import javax.inject.Singleton

@Database(
    entities = [
        VaultEntity::class,
        HiddenItemEntity::class,
        IntruderLogEntity::class,
        PremiumStatusEntity::class,
        HiddenAppEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class CoverDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao
    abstract fun hiddenItemDao(): HiddenItemDao
    abstract fun intruderLogDao(): IntruderLogDao
    abstract fun premiumStatusDao(): PremiumStatusDao
    abstract fun hiddenAppDao(): HiddenAppDao
}

@Singleton
class CoverDatabaseProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionManager: EncryptionManager
) {
    companion object {
        private const val DATABASE_NAME = "cover.db"
        private const val DB_KEY_ALIAS = "cover_db_key"

        /**
         * Migration from version 1 to 2
         * Add HiddenAppEntity table
         */
        private val MIGRATION_1_2 = androidx.room.migration.Migration(1, 2) { database ->
            database.execSQL(
                """CREATE TABLE IF NOT EXISTS hidden_apps (
                    id TEXT PRIMARY KEY NOT NULL,
                    vaultId TEXT NOT NULL,
                    packageName TEXT NOT NULL,
                    appName TEXT NOT NULL,
                    isHidden INTEGER NOT NULL DEFAULT 1,
                    createdAt INTEGER NOT NULL DEFAULT 0
                )"""
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS index_hidden_apps_vaultId ON hidden_apps(vaultId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_hidden_apps_packageName ON hidden_apps(packageName)")
        }
    }

    private val dbInstance: CoverDatabase by lazy { buildDatabase() }

    fun getDatabase(): CoverDatabase = dbInstance

    private fun buildDatabase(): CoverDatabase {
        // Derive database key from master key via Android Keystore
        val dbKey = encryptionManager.retrieveSecureString(DB_KEY_ALIAS) ?: run {
            // Generate new random key and store securely
            val newKey = generateSecureRandomKey()
            encryptionManager.storeSecureString(DB_KEY_ALIAS, newKey)
            newKey
        }

        val factory = SupportFactory(SQLiteDatabase.getBytes(dbKey.toCharArray()))

        return Room.databaseBuilder(
            context.applicationContext,
            CoverDatabase::class.java,
            DATABASE_NAME
        )
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    private fun generateSecureRandomKey(): String {
        val random = java.security.SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
