package com.vci.vectorcamapp.program.kenya.presentation
 
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme
import org.junit.Rule
import org.junit.Test
 
class HouseholdBannerTest {
    @get:Rule
    val composeRule = createComposeRule()
 
    @Test
    fun displaysHouseholdNumberWithoutLosingLeadingZeros() {
        composeRule.setContent {
            VectorcamappTheme { HouseholdBanner("007") }
        }
        composeRule.onNodeWithText("Household 007").assertIsDisplayed()
    }
}