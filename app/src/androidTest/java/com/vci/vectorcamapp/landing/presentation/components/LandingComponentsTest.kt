package com.vci.vectorcamapp.landing.presentation.components

import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vci.vectorcamapp.R
import junit.framework.TestCase
import org.junit.Rule
import org.junit.Test

class LandingComponentsTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun actionTile_click_invokesCallback() {
        var clicked = false
        rule.setContent {
            LandingActionTile(
                title = "Tile Title",
                description = "Tile Description",
                icon = painterResource(R.drawable.ic_help),
                onClick = { clicked = true },
                badgeCount = 0
            )
        }
        rule.onNodeWithText("Tile Title").performClick()
        TestCase.assertTrue(clicked)
    }

    @Test
    fun actionTile_badgeVisible_whenCountPositive() {
        rule.setContent {
            LandingActionTile(
                title = "With Badge",
                description = "desc",
                icon = painterResource(R.drawable.ic_help),
                onClick = {},
                badgeCount = 4
            )
        }
        rule.onNodeWithText("4", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun actionTile_noBadge_whenCountZero() {
        rule.setContent {
            LandingActionTile(
                title = "No Badge",
                description = "desc",
                icon = painterResource(R.drawable.ic_help),
                onClick = {},
                badgeCount = 0
            )
        }
        rule.onAllNodesWithText("0", useUnmergedTree = true)
            .assertCountEquals(0)
    }

    @Test
    fun landingSection_displaysTitle_andContent() {
        rule.setContent {
            LandingSection(title = "My Section") {
                Text("Inner Content")
            }
        }
        rule.onNodeWithText("My Section").assertIsDisplayed()
        rule.onNodeWithText("Inner Content").assertIsDisplayed()
    }
}