package com.vci.vectorcamapp.main.presentation.components

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.MainActivity
import com.vci.vectorcamapp.main.presentation.MainAction
import com.vci.vectorcamapp.main.presentation.MainState
import com.vci.vectorcamapp.main.presentation.PermissionScreen
import com.vci.vectorcamapp.main.presentation.util.PermissionTestTags
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class PermissionScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    // =========================
    // Harness / Helpers
    // =========================

    private fun launchPermissionScreen(
        initialState: MainState = MainState(),
        onAction: ((MainAction) -> Unit)? = null
    ) {
        composeRule.activity.setContent {
            VectorcamappTheme {
                PermissionScreen(
                    state = initialState,
                    onAction = { a -> onAction?.invoke(a) }
                )
            }
        }
        composeRule.waitForIdle()
    }

    // =========================
    // A. Static UI Visibility
    // =========================

    @Test
    fun permUi_a01_allDenied_showsTitleAndAllButtons() {
        launchPermissionScreen(
            initialState = MainState(
                allGranted = false,
                isGpsEnabled = false,
                isAutoTimeEnabled = false
            )
        )

        composeRule.onNodeWithTag(PermissionTestTags.TITLE)
            .assertIsDisplayed()
            .assertTextEquals("Permissions Required")

        composeRule.onNodeWithTag(PermissionTestTags.GRANT_PERMISSIONS_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(PermissionTestTags.ENABLE_GPS_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(PermissionTestTags.ENABLE_AUTO_TIME_BUTTON).assertIsDisplayed()
    }

    @Test
    fun permUi_a02_onlyPermissionsDenied_showsTitleAndGrantButton() {
        launchPermissionScreen(
            initialState = MainState(
                allGranted = false,
                isGpsEnabled = true,
                isAutoTimeEnabled = true
            )
        )

        composeRule.onNodeWithTag(PermissionTestTags.TITLE)
            .assertIsDisplayed()
            .assertTextEquals("Permissions Required")

        composeRule.onNodeWithTag(PermissionTestTags.GRANT_PERMISSIONS_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(PermissionTestTags.ENABLE_GPS_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(PermissionTestTags.ENABLE_AUTO_TIME_BUTTON).assertDoesNotExist()
    }

    @Test
    fun permUi_a03_onlyGpsDisabled_showsTitleAndGpsButton() {
        launchPermissionScreen(
            initialState = MainState(
                allGranted = true,
                isGpsEnabled = false,
                isAutoTimeEnabled = true
            )
        )

        composeRule.onNodeWithTag(PermissionTestTags.TITLE)
            .assertIsDisplayed()
            .assertTextEquals("GPS Required")

        composeRule.onNodeWithTag(PermissionTestTags.GRANT_PERMISSIONS_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(PermissionTestTags.ENABLE_GPS_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(PermissionTestTags.ENABLE_AUTO_TIME_BUTTON).assertDoesNotExist()
    }

    @Test
    fun permUi_a04_onlyAutoTimeDisabled_showsTitleAndAutoTimeButton() {
        launchPermissionScreen(
            initialState = MainState(
                allGranted = true,
                isGpsEnabled = true,
                isAutoTimeEnabled = false
            )
        )

        composeRule.onNodeWithTag(PermissionTestTags.TITLE)
            .assertIsDisplayed()
            .assertTextEquals("Automatic Date & Time Required")

        composeRule.onNodeWithTag(PermissionTestTags.GRANT_PERMISSIONS_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(PermissionTestTags.ENABLE_GPS_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(PermissionTestTags.ENABLE_AUTO_TIME_BUTTON).assertIsDisplayed()
    }

    @Test
    fun permUi_a05_allGranted_showsTitleButNoButtons() {
        launchPermissionScreen(
            initialState = MainState(
                allGranted = true,
                isGpsEnabled = true,
                isAutoTimeEnabled = true
            )
        )

        composeRule.onNodeWithTag(PermissionTestTags.TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(PermissionTestTags.GRANT_PERMISSIONS_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(PermissionTestTags.ENABLE_GPS_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(PermissionTestTags.ENABLE_AUTO_TIME_BUTTON).assertDoesNotExist()
    }

    // =========================
    // B. Button Click Actions
    // =========================

    @Test
    fun permUi_b01_clickGrantPermissions_invokesCorrectAction() {
        var lastAction: MainAction? = null
        launchPermissionScreen(
            initialState = MainState(
                allGranted = false,
                isGpsEnabled = true,
                isAutoTimeEnabled = true
            ),
            onAction = { lastAction = it }
        )

        composeRule.onNodeWithTag(PermissionTestTags.GRANT_PERMISSIONS_BUTTON).performClick()
        assertThat(lastAction).isEqualTo(MainAction.OpenAppSettings)
    }

    @Test
    fun permUi_b02_clickEnableGps_invokesCorrectAction() {
        var lastAction: MainAction? = null
        launchPermissionScreen(
            initialState = MainState(
                allGranted = true,
                isGpsEnabled = false,
                isAutoTimeEnabled = true
            ),
            onAction = { lastAction = it }
        )

        composeRule.onNodeWithTag(PermissionTestTags.ENABLE_GPS_BUTTON).performClick()
        assertThat(lastAction).isEqualTo(MainAction.OpenLocationSettings)
    }

    @Test
    fun permUi_b03_clickEnableAutoTime_invokesCorrectAction() {
        var lastAction: MainAction? = null
        launchPermissionScreen(
            initialState = MainState(
                allGranted = true,
                isGpsEnabled = true,
                isAutoTimeEnabled = false
            ),
            onAction = { lastAction = it }
        )

        composeRule.onNodeWithTag(PermissionTestTags.ENABLE_AUTO_TIME_BUTTON).performClick()
        assertThat(lastAction).isEqualTo(MainAction.OpenDateSettings)
    }

    @Test
    fun permUi_b04_allButtons_areEnabledAndClickable() {
        launchPermissionScreen(
            initialState = MainState(
                allGranted = false,
                isGpsEnabled = false,
                isAutoTimeEnabled = false
            )
        )

        composeRule.onNodeWithTag(PermissionTestTags.GRANT_PERMISSIONS_BUTTON)
            .assertIsEnabled()
            .assertHasClickAction()

        composeRule.onNodeWithTag(PermissionTestTags.ENABLE_GPS_BUTTON)
            .assertIsEnabled()
            .assertHasClickAction()

        composeRule.onNodeWithTag(PermissionTestTags.ENABLE_AUTO_TIME_BUTTON)
            .assertIsEnabled()
            .assertHasClickAction()
    }

    // =========================
    // C. Edge Cases
    // =========================

    @Test
    fun permUi_c01_clickingButtons_withNoCallback_doesNotCrash() {
        launchPermissionScreen(
            initialState = MainState(
                allGranted = false,
                isGpsEnabled = false,
                isAutoTimeEnabled = false
            ),
            onAction = null
        )

        composeRule.onNodeWithTag(PermissionTestTags.GRANT_PERMISSIONS_BUTTON).performClick()
        composeRule.onNodeWithTag(PermissionTestTags.ENABLE_GPS_BUTTON).performClick()
        composeRule.onNodeWithTag(PermissionTestTags.ENABLE_AUTO_TIME_BUTTON).performClick()
    }

    @Test
    fun permUi_c02_composingWithPermissionsDenied_requestsPermissions() {
        var requestedPermissions = false
        launchPermissionScreen(
            initialState = MainState(allGranted = false),
            onAction = { if (it is MainAction.RequestPermissions) requestedPermissions = true }
        )
        assertThat(requestedPermissions).isTrue()
    }

    @Test
    fun permUi_c03_composingWithPermissionsGranted_doesNotRequestPermissions() {
        var requestedPermissions = false
        launchPermissionScreen(
            initialState = MainState(allGranted = true),
            onAction = { if (it is MainAction.RequestPermissions) requestedPermissions = true }
        )
        assertThat(requestedPermissions).isFalse()
    }

    // =========================
    // D. Tooltip visibility
    // =========================

    @Test
    fun permUi_d01_tooltipVisible_rendersContent() {
        launchPermissionScreen(
            initialState = MainState(
                allGranted = false,
                isGpsEnabled = false,
                isAutoTimeEnabled = false,
                isPermissionTooltipVisible = true
            )
        )
        composeRule.onNodeWithTag(PermissionTestTags.PERMISSION_TOOLTIP_CONTENT).assertIsDisplayed()
    }

    @Test
    fun permUi_d02_tooltipHidden_hidesContent() {
        launchPermissionScreen(
            initialState = MainState(
                allGranted = false,
                isGpsEnabled = false,
                isAutoTimeEnabled = false,
                isPermissionTooltipVisible = false
            )
        )
        composeRule.onNodeWithTag(PermissionTestTags.PERMISSION_TOOLTIP_CONTENT).assertDoesNotExist()
    }
}