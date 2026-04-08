package com.cover.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cover.app.data.local.CoverDatabase
import com.cover.app.data.local.CoverDatabaseProvider
import com.cover.app.data.local.entity.HiddenItemEntity
import com.cover.app.data.local.entity.VaultEntity
import com.cover.app.data.security.EncryptionManager
import com.cover.app.data.storage.SecureStorageManager
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith

/**
 * Phase 9: Integration Tests
 * Tests database, encryption, and storage integration
 */
@RunWith(AndroidJUnit4::class)
class IntegrationTest {
    
    private lateinit var database: CoverDatabase
    private lateinit var encryptionManager: EncryptionManager
    private lateinit var secureStorage: SecureStorageManager
    
    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Create components
        encryptionManager = EncryptionManager(context)
        val dbProvider = CoverDatabaseProvider(context, encryptionManager)
        database = dbProvider.getDatabase()
        secureStorage = SecureStorageManager(context, encryptionManager)
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun testDatabaseOperations() = runBlocking {
        // Given
        val vaultDao = database.vaultDao()
        val itemDao = database.hiddenItemDao()
        
        val vaultSalt = encryptionManager.generateSalt()
        val vault = VaultEntity(
            id = "test_vault",
            name = "Test Vault",
            salt = vaultSalt,
            isDecoy = false,
            createdAt = System.currentTimeMillis()
        )
        
        val item = HiddenItemEntity(
            id = "test_item",
            vaultId = "test_vault",
            encryptedPath = "/test/path",
            originalName = "test.jpg",
            type = HiddenItemEntity.ItemType.PHOTO,
            size = 1024,
            thumbnailEncryptedPath = null,
            createdAt = System.currentTimeMillis()
        )
        
        // When
        vaultDao.insertVault(vault)
        itemDao.insertItem(item)
        
        // Then
        val retrievedVault = vaultDao.getVaultById("test_vault")
        Assert.assertNotNull(retrievedVault)
        Assert.assertEquals("Test Vault", retrievedVault?.name)
        
        val items = itemDao.getItemsByVaultOnce("test_vault")
        Assert.assertEquals(1, items.size)
        Assert.assertEquals("test_item", items[0].id)
    }
    
    @Test
    fun testEncryptionDecryption() = runBlocking {
        // Given
        val plaintext = "Hello, Secret World!"
        val salt = encryptionManager.generateSalt()
        val pin = "1234"
        
        // When
        val encryptedData = encryptionManager.encryptWithPin(plaintext.toByteArray(), pin, salt)
        val decrypted = encryptionManager.decryptWithPin(encryptedData, pin, salt)
        
        // Then
        Assert.assertEquals(plaintext, String(decrypted))
    }
    
    @Test
    fun testDifferentDataYieldsDifferentCiphertext() = runBlocking {
        // Given
        val data1 = "Data 1".toByteArray()
        val data2 = "Data 2".toByteArray()
        val salt = encryptionManager.generateSalt()
        val pin = "1234"
        
        // When
        val encrypted1 = encryptionManager.encryptWithPin(data1, pin, salt)
        val encrypted2 = encryptionManager.encryptWithPin(data2, pin, salt)
        
        // Then
        Assert.assertFalse(encrypted1.toByteArray().contentEquals(encrypted2.toByteArray()))
    }
    
    @Test
    fun testSecureFileStorage() = runBlocking {
        // Given
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testFile = java.io.File(context.cacheDir, "test_file.txt")
        testFile.writeText("Secret content")
        
        val vaultSalt = encryptionManager.generateSalt()
        val pin = "1234"
        
        // When
        val fileId = secureStorage.storeFile(testFile.absolutePath, pin, vaultSalt)
        
        // Then
        Assert.assertNotNull(fileId)
        Assert.assertTrue(fileId.isNotEmpty())
        
        // Verify we can retrieve and decrypt
        val decryptedBytes = secureStorage.retrieveFile(fileId, pin, vaultSalt)
        Assert.assertNotNull(decryptedBytes)
        Assert.assertEquals("Secret content", String(decryptedBytes!!))
    }
    
    @Test
    fun testVaultCount() = runBlocking {
        // Given
        val vaultDao = database.vaultDao()
        val salt1 = encryptionManager.generateSalt()
        val salt2 = encryptionManager.generateSalt()
        
        // When
        vaultDao.insertVault(VaultEntity("1", "Vault 1", salt1, false, System.currentTimeMillis()))
        vaultDao.insertVault(VaultEntity("2", "Vault 2", salt2, false, System.currentTimeMillis()))
        
        // Then
        val count = vaultDao.getVaultCount()
        Assert.assertTrue(count >= 2)
    }
    
    @Test
    fun testItemCountByVault() = runBlocking {
        // Given
        val itemDao = database.hiddenItemDao()
        val vaultId = "test_vault_count"
        
        // When
        itemDao.insertItem(HiddenItemEntity(
            "1", vaultId, "/path1", "orig1.jpg", 
            HiddenItemEntity.ItemType.PHOTO, 100, null, System.currentTimeMillis()
        ))
        itemDao.insertItem(HiddenItemEntity(
            "2", vaultId, "/path2", "orig2.jpg", 
            HiddenItemEntity.ItemType.PHOTO, 100, null, System.currentTimeMillis()
        ))
        
        // Then
        val count = itemDao.getItemCountByVault(vaultId)
        Assert.assertTrue(count >= 2)
    }
    
    @Test
    fun testDeleteItem() = runBlocking {
        // Given
        val itemDao = database.hiddenItemDao()
        val item = HiddenItemEntity(
            "delete_test",
            "vault",
            "/path",
            "orig.jpg",
            HiddenItemEntity.ItemType.PHOTO,
            100,
            null,
            System.currentTimeMillis()
        )
        
        // When
        itemDao.insertItem(item)
        itemDao.deleteItem(item)
        
        // Then
        val retrieved = itemDao.getItemById("delete_test")
        Assert.assertNull(retrieved)
    }
}
