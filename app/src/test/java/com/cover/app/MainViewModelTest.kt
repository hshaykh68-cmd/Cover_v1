package com.cover.app

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.cover.app.data.billing.BillingManager
import com.cover.app.data.billing.BillingState
import com.cover.app.data.remoteconfig.ABTestManager
import com.cover.app.data.remoteconfig.PromotionManager
import com.cover.app.data.remoteconfig.RemoteConfigManager
import com.cover.app.presentation.main.MainViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.rules.TestRule

/**
 * Phase 9: Unit Tests for MainViewModel
 * Tests critical user flows and state management
 */
@ExperimentalCoroutinesApi
class MainViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule: TestRule = InstantTaskExecutorRule()
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var viewModel: MainViewModel
    private lateinit var promotionManager: PromotionManager
    private lateinit var tutorialManager: com.cover.app.data.remoteconfig.TutorialManager
    private lateinit var inAppMessageManager: com.cover.app.data.remoteconfig.InAppMessageManager
    
    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        
        // Mock dependencies
        promotionManager = mockk(relaxed = true)
        tutorialManager = mockk(relaxed = true)
        inAppMessageManager = mockk(relaxed = true)
        
        // Setup default mock behaviors
        every { tutorialManager.shouldShowTutorial() } returns false
        every { tutorialManager.tutorialState } returns MutableStateFlow(
            com.cover.app.data.remoteconfig.TutorialState.NotStarted
        )
        every { inAppMessageManager.messageState } returns MutableStateFlow(
            com.cover.app.data.remoteconfig.InAppMessageState.Idle
        )
        
        viewModel = MainViewModel(promotionManager, tutorialManager, inAppMessageManager)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `when app launches, should check app status`() = runTest {
        // Given
        val mockStatus = com.cover.app.data.remoteconfig.AppStatus.OK
        every { promotionManager.checkAppStatus(any()) } returns mockStatus
        
        // When - ViewModel is initialized
        advanceUntilIdle()
        
        // Then
        verify { promotionManager.checkAppStatus(any()) }
        Assert.assertEquals(mockStatus, viewModel.appStatus.value)
    }
    
    @Test
    fun `when tutorial should show, trigger tutorial flow`() = runTest {
        // Given
        every { tutorialManager.shouldShowTutorial() } returns true
        
        // When
        val viewModel = MainViewModel(promotionManager, tutorialManager, inAppMessageManager)
        advanceUntilIdle()
        
        // Then
        verify { tutorialManager.startTutorial() }
        Assert.assertTrue(viewModel.showTutorial.value)
    }
    
    @Test
    fun `when tutorial completes, hide tutorial UI`() = runTest {
        // Given
        val tutorialState = MutableStateFlow<com.cover.app.data.remoteconfig.TutorialState>(
            com.cover.app.data.remoteconfig.TutorialState.InProgress(1, 3)
        )
        every { tutorialManager.tutorialState } returns tutorialState
        
        // When
        viewModel.onTutorialComplete()
        tutorialState.value = com.cover.app.data.remoteconfig.TutorialState.Completed
        advanceUntilIdle()
        
        // Then
        Assert.assertFalse(viewModel.showTutorial.value)
    }
    
    @Test
    fun `when paywall should show, emit show paywall state`() = runTest {
        // Given
        val promoState = MutableStateFlow(
            com.cover.app.data.remoteconfig.PromotionState(
                showPaywall = true,
                isPremium = false
            )
        )
        every { promotionManager.promotionState } returns promoState
        
        // When
        advanceUntilIdle()
        
        // Then
        Assert.assertTrue(viewModel.showPaywall.value)
    }
    
    @Test
    fun `when user is premium, do not show paywall`() = runTest {
        // Given
        val promoState = MutableStateFlow(
            com.cover.app.data.remoteconfig.PromotionState(
                showPaywall = true,
                isPremium = true
            )
        )
        every { promotionManager.promotionState } returns promoState
        
        // When
        advanceUntilIdle()
        
        // Then
        Assert.assertFalse(viewModel.showPaywall.value)
    }
    
    @Test
    fun `when paywall dismissed, mark as shown`() = runTest {
        // When
        viewModel.dismissPaywall()
        
        // Then
        verify { promotionManager.markPaywallShown() }
        Assert.assertFalse(viewModel.showPaywall.value)
    }
    
    @Test
    fun `when limited offer triggered, show offer UI`() = runTest {
        // Given
        val promoState = MutableStateFlow(
            com.cover.app.data.remoteconfig.PromotionState(
                showLimitedTimeOffer = true,
                isPremium = false
            )
        )
        every { promotionManager.promotionState } returns promoState
        
        // When
        advanceUntilIdle()
        
        // Then
        Assert.assertTrue(viewModel.showLimitedOffer.value)
    }
    
    @Test
    fun `when app is killed, emit killed status`() = runTest {
        // Given
        val killedStatus = com.cover.app.data.remoteconfig.AppStatus.KILLED(
            "App is no longer available"
        )
        every { promotionManager.checkAppStatus(any()) } returns killedStatus
        
        // When
        advanceUntilIdle()
        
        // Then
        Assert.assertTrue(viewModel.appStatus.value is com.cover.app.data.remoteconfig.AppStatus.KILLED)
    }
    
    @Test
    fun `when in-app message shown, update message state`() = runTest {
        // Given
        val messageState = MutableStateFlow(
            com.cover.app.data.remoteconfig.InAppMessageState.Showing(
                type = com.cover.app.data.remoteconfig.MessageType.UPSELL,
                title = "Special Offer",
                body = "Upgrade now!",
                actionText = "Upgrade",
                campaignId = "test_campaign"
            )
        )
        every { inAppMessageManager.messageState } returns messageState
        
        // When
        advanceUntilIdle()
        
        // Then
        val currentState = viewModel.inAppMessage.value
        Assert.assertTrue(currentState is com.cover.app.data.remoteconfig.InAppMessageState.Showing)
    }
}

/**
 * MainDispatcherRule for coroutine testing
 */
@ExperimentalCoroutinesApi
class MainDispatcherRule : TestWatcher() {
    private val testDispatcher = StandardTestDispatcher()
    
    override fun starting(description: Description?) {
        Dispatchers.setMain(testDispatcher)
    }
    
    override fun finished(description: Description?) {
        Dispatchers.resetMain()
    }
}
