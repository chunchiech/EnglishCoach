package com.andy.englishcoach.ui.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.data.preference.InMemoryDailyLearningPreferences
import com.andy.englishcoach.data.preference.InMemorySettingsPreferences
import com.andy.englishcoach.data.preference.SharedPreferencesDailyLearningPreferences
import com.andy.englishcoach.data.preference.SharedPreferencesSettingsPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    private lateinit var context: Context
    private lateinit var settingsPrefs: InMemorySettingsPreferences
    private lateinit var learningPrefs: InMemoryDailyLearningPreferences
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        settingsPrefs = InMemorySettingsPreferences()
        learningPrefs = InMemoryDailyLearningPreferences()
        viewModel = SettingsViewModel(
            settingsPreferences = settingsPrefs,
            learningPreferences = learningPrefs,
            versionName = "1.1.0 (7)"
        )
    }

    @Test
    fun initialState_hasCorrectDefaults() {
        val state = viewModel.uiState.value

        assertEquals("英語學習者", state.displayName)
        assertEquals("🐶", state.avatarEmoji)
        assertTrue(state.hapticFeedbackEnabled)
        assertEquals(ToeicTarget.BASIC, state.targetLevel)
        assertEquals(10, state.dailyTarget)
        assertEquals(10, state.remainingFreeQuestions)
        assertEquals(0, state.todayCompletedCount)
        assertEquals("1.1.0 (7)", state.versionName)
        assertEquals(12, state.availableEmojis.size)
        assertTrue(state.availableEmojis.contains("🐶"))
        assertTrue(state.availableEmojis.contains("🍞"))
        assertTrue(state.availableEmojis.contains("🎯"))
    }

    @Test
    fun updateProfile_persistsNameAndEmoji() {
        viewModel.updateProfile("英文大師", "🦊")

        val state = viewModel.uiState.value
        assertEquals("英文大師", state.displayName)
        assertEquals("🦊", state.avatarEmoji)
        assertEquals("英文大師", settingsPrefs.getDisplayName())
        assertEquals("🦊", settingsPrefs.getAvatarEmoji())
    }

    @Test
    fun updateProfile_ignoresBlankName() {
        viewModel.updateProfile("   ", "🚀")

        val state = viewModel.uiState.value
        assertEquals("英語學習者", state.displayName)
        assertEquals("🚀", state.avatarEmoji)
    }

    @Test
    fun toggleHapticFeedback_persistsState() {
        viewModel.setHapticFeedbackEnabled(false)

        assertFalse(viewModel.uiState.value.hapticFeedbackEnabled)
        assertFalse(settingsPrefs.isHapticFeedbackEnabled())

        viewModel.setHapticFeedbackEnabled(true)
        assertTrue(viewModel.uiState.value.hapticFeedbackEnabled)
        assertTrue(settingsPrefs.isHapticFeedbackEnabled())
    }

    @Test
    fun targetLevelAndQuota_reflectPreferences() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        learningPrefs.setUserLevel(ToeicTarget.GOLD.rawLevel)
        learningPrefs.recordPracticeQuestions(todayStr, 6)

        viewModel.refresh()

        val state = viewModel.uiState.value
        assertEquals(ToeicTarget.GOLD, state.targetLevel)
        assertEquals(6, state.todayCompletedCount)
        assertEquals(4, state.remainingFreeQuestions)
    }

    @Test
    fun sheetVisibility_toggledProperly() {
        assertFalse(viewModel.uiState.value.showEditProfileSheet)

        viewModel.setEditProfileSheetVisible(true)
        assertTrue(viewModel.uiState.value.showEditProfileSheet)

        viewModel.setEditProfileSheetVisible(false)
        assertFalse(viewModel.uiState.value.showEditProfileSheet)
    }

    @Test
    fun crossDay_resetsTodayCompletedCount() {
        learningPrefs.recordPracticeQuestions("2026-09-20", 10)
        // Today is not 2026-09-20
        viewModel.refresh()

        val state = viewModel.uiState.value
        assertEquals(0, state.todayCompletedCount)
        assertEquals(10, state.remainingFreeQuestions)
    }

    @Test
    fun availableEmojis_matchesAll12Tokens() {
        val expected = listOf("🐶", "🍞", "🎓", "🌟", "🦁", "🐱", "🐻", "🦊", "🐨", "🚀", "💡", "🎯")
        assertEquals(expected, SharedPreferencesSettingsPreferences.AVAILABLE_AVATAR_EMOJIS)
        assertEquals(expected, viewModel.uiState.value.availableEmojis)
    }

    @Test
    fun inMemorySettingsPreferences_mutationsPreserved() {
        val inMem = InMemorySettingsPreferences()
        inMem.setDisplayName("Alice")
        inMem.setAvatarEmoji("🎓")
        inMem.setHapticFeedbackEnabled(false)

        assertEquals("Alice", inMem.getDisplayName())
        assertEquals("🎓", inMem.getAvatarEmoji())
        assertFalse(inMem.isHapticFeedbackEnabled())
    }

    @Test
    fun sharedPreferencesSettingsPreferences_persistsAcrossInstances() {
        val realPrefs = SharedPreferencesSettingsPreferences.create(context)
        realPrefs.setDisplayName("Test User")
        realPrefs.setAvatarEmoji("🐻")
        realPrefs.setHapticFeedbackEnabled(false)

        // Create a new instance pointing to same shared preferences
        val reloadedPrefs = SharedPreferencesSettingsPreferences.create(context)
        assertEquals("Test User", reloadedPrefs.getDisplayName())
        assertEquals("🐻", reloadedPrefs.getAvatarEmoji())
        assertFalse(reloadedPrefs.isHapticFeedbackEnabled())
    }
}
