package com.epikaigle.lemondashboard

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun loginIsAccessibleAndRequiresApiKey() {
        rule.onNodeWithText("Vos ventes.", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Connecter ma boutique").assertIsNotEnabled()
        rule.onNodeWithText("Votre clé est chiffrée", substring = true).assertExists()
    }

    @Test fun fakeTokenEnablesActionAndVisibilityCanToggle() {
        rule.onNodeWithText("Collez votre clé ici").performTextInput("fake-token-not-sent")
        rule.onNodeWithText("Connecter ma boutique").assertIsEnabled()
        rule.onNodeWithContentDescription("Afficher la clé").performClick()
        rule.onNodeWithContentDescription("Masquer la clé").assertIsDisplayed()
        // Never submit the fake token: tests have no real credentials.
    }
}
