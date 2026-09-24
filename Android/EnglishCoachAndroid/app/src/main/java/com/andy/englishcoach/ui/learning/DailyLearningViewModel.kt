package com.andy.englishcoach.ui.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.andy.englishcoach.billing.DefaultBillingRepository
import com.andy.englishcoach.billing.PremiumEntitlementProvider
import com.andy.englishcoach.data.model.DailyLearningUiState
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.data.preference.SettingsPreferences
import com.andy.englishcoach.data.repository.DailyLearningRepository
import com.andy.englishcoach.util.TtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for the Daily Learning screen.
 * Coordinates word loading, card flips, navigation, learned state marking, and TTS pronunciation.
 */
class DailyLearningViewModel(
    private val repository: DailyLearningRepository,
    private val ttsManager: TtsManager? = null,
    private val settingsPreferences: SettingsPreferences? = null,
    private val entitlementProvider: PremiumEntitlementProvider = DefaultBillingRepository(),
    private val preferences: com.andy.englishcoach.data.preference.DailyLearningPreferences? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyLearningUiState())
    val uiState: StateFlow<DailyLearningUiState> = _uiState.asStateFlow()

    init {
        loadTodayWords()
    }

    fun loadTodayWords(target: ToeicTarget? = null) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val selectedTarget = target ?: repository.getUserTargetLevel()
            val isPremium = entitlementProvider.isPremium
            val policy = com.andy.englishcoach.billing.DailyTargetPolicy.forIsPremium(isPremium)
            val currentTarget = policy.coerceTarget(settingsPreferences?.getDailyTarget() ?: 10)
            val effectiveLimit = if (currentTarget == com.andy.englishcoach.billing.DailyTargetPolicy.UNLIMITED_TARGET) {
                DailyLearningRepository.UNLIMITED_BATCH_SIZE
            } else {
                currentTarget
            }

            val allTodayWords = withContext(Dispatchers.IO) {
                repository.getTodayWords(selectedTarget, limit = effectiveLimit)
            }

            val today = repository.getTodayDateString()
            val practiceDate = preferences?.getDailyPracticeDate()
            val todayCompleted = if (practiceDate == today) (preferences?.getDailyPracticeCount() ?: 0) else 0

            val sessionWords = if (todayCompleted > 0 && todayCompleted < allTodayWords.size) {
                allTodayWords.drop(todayCompleted)
            } else if (todayCompleted >= allTodayWords.size && allTodayWords.isNotEmpty()) {
                allTodayWords
            } else {
                allTodayWords
            }

            val isTierCompleted = sessionWords.isEmpty()

            _uiState.update {
                it.copy(
                    target = selectedTarget,
                    words = sessionWords,
                    currentIndex = 0,
                    isCardFlipped = false,
                    isLoading = false,
                    isCompleted = false,
                    isTierCompleted = isTierCompleted,
                    isPremium = isPremium
                )
            }

            // Auto-pronounce first word if available and auto-read is enabled
            if (sessionWords.isNotEmpty() && (settingsPreferences?.isAutoReadEnabled() != false)) {
                ttsManager?.speak(sessionWords[0].word)
            }
        }
    }

    fun onCardClicked() {
        _uiState.update { it.copy(isCardFlipped = !it.isCardFlipped) }
    }

    fun onFlipToMeaning() {
        _uiState.update { it.copy(isCardFlipped = true) }
    }

    fun onNextCard() {
        val state = _uiState.value
        val currentWord = state.currentWord ?: return

        // Mark current word as learned in background
        viewModelScope.launch(Dispatchers.IO) {
            repository.markWordAsLearned(currentWord.word)
        }

        if (state.currentIndex < state.words.size - 1) {
            val nextIndex = state.currentIndex + 1
            _uiState.update {
                it.copy(
                    currentIndex = nextIndex,
                    isCardFlipped = false
                )
            }
            // Auto-pronounce next word if auto-read is enabled
            if (settingsPreferences?.isAutoReadEnabled() != false) {
                ttsManager?.speak(state.words[nextIndex].word)
            }
        } else {
            // Completed all cards for today
            viewModelScope.launch(Dispatchers.IO) {
                repository.markTodayLearningCompleted()
            }
            _uiState.update {
                it.copy(
                    isCompleted = true,
                    isCardFlipped = false
                )
            }
        }
    }

    fun onPreviousCard() {
        val state = _uiState.value
        if (state.currentIndex > 0) {
            val prevIndex = state.currentIndex - 1
            _uiState.update {
                it.copy(
                    currentIndex = prevIndex,
                    isCardFlipped = false
                )
            }
            ttsManager?.speak(state.words[prevIndex].word)
        }
    }

    fun speakCurrentWord() {
        _uiState.value.currentWord?.let {
            ttsManager?.speak(it.word)
        }
    }

    fun speakSentence(text: String) {
        ttsManager?.speak(text)
    }

    class Factory(
        private val repository: DailyLearningRepository,
        private val ttsManager: TtsManager? = null,
        private val settingsPreferences: SettingsPreferences? = null,
        private val entitlementProvider: PremiumEntitlementProvider = DefaultBillingRepository(),
        private val preferences: com.andy.englishcoach.data.preference.DailyLearningPreferences? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DailyLearningViewModel(repository, ttsManager, settingsPreferences, entitlementProvider, preferences) as T
        }
    }
}
