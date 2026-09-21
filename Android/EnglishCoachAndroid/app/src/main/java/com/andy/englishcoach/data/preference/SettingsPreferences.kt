package com.andy.englishcoach.data.preference

import android.content.Context
import android.content.SharedPreferences

/**
 * Storage abstraction for user profile, audio/TTS settings, and product personalization.
 * Uses exact iOS EnglishCoach AppStorage keys and defaults for parity.
 */
interface SettingsPreferences {
    fun getDisplayName(): String
    fun setDisplayName(name: String)

    fun getAvatarEmoji(): String
    fun setAvatarEmoji(emoji: String)

    fun isAutoReadEnabled(): Boolean
    fun setAutoReadEnabled(enabled: Boolean)

    fun isHapticFeedbackEnabled(): Boolean
    fun setHapticFeedbackEnabled(enabled: Boolean)
}

/**
 * SharedPreferences implementation of SettingsPreferences.
 */
class SharedPreferencesSettingsPreferences(
    private val prefs: SharedPreferences
) : SettingsPreferences {

    companion object {
        const val KEY_USER_DISPLAY_NAME = "user_display_name"
        const val KEY_USER_AVATAR_EMOJI = "user_avatar_emoji"
        const val KEY_AUTO_READ_TTS = "auto_read_tts_enabled"
        const val KEY_HAPTIC_FEEDBACK = "haptic_feedback_enabled"

        const val DEFAULT_DISPLAY_NAME = "英語學習者"
        const val DEFAULT_AVATAR_EMOJI = "🐶"
        const val DEFAULT_AUTO_READ_TTS = true
        const val DEFAULT_HAPTIC_FEEDBACK = true

        val AVAILABLE_AVATAR_EMOJIS = listOf(
            "🐶", "🍞", "🎓", "🌟", "🦁", "🐱",
            "🐻", "🦊", "🐨", "🚀", "💡", "🎯"
        )

        fun create(context: Context): SharedPreferencesSettingsPreferences {
            val sharedPrefs = context.getSharedPreferences(
                SharedPreferencesDailyLearningPreferences.PREFS_NAME,
                Context.MODE_PRIVATE
            )
            return SharedPreferencesSettingsPreferences(sharedPrefs)
        }
    }

    override fun getDisplayName(): String {
        return prefs.getString(KEY_USER_DISPLAY_NAME, DEFAULT_DISPLAY_NAME) ?: DEFAULT_DISPLAY_NAME
    }

    override fun setDisplayName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            prefs.edit().putString(KEY_USER_DISPLAY_NAME, trimmed).apply()
        }
    }

    override fun getAvatarEmoji(): String {
        return prefs.getString(KEY_USER_AVATAR_EMOJI, DEFAULT_AVATAR_EMOJI) ?: DEFAULT_AVATAR_EMOJI
    }

    override fun setAvatarEmoji(emoji: String) {
        prefs.edit().putString(KEY_USER_AVATAR_EMOJI, emoji).apply()
    }

    override fun isAutoReadEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_READ_TTS, DEFAULT_AUTO_READ_TTS)
    }

    override fun setAutoReadEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_READ_TTS, enabled).apply()
    }

    override fun isHapticFeedbackEnabled(): Boolean {
        return prefs.getBoolean(KEY_HAPTIC_FEEDBACK, DEFAULT_HAPTIC_FEEDBACK)
    }

    override fun setHapticFeedbackEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, enabled).apply()
    }
}

/**
 * In-memory implementation of SettingsPreferences for isolated unit testing.
 */
class InMemorySettingsPreferences(
    private var displayName: String = SharedPreferencesSettingsPreferences.DEFAULT_DISPLAY_NAME,
    private var avatarEmoji: String = SharedPreferencesSettingsPreferences.DEFAULT_AVATAR_EMOJI,
    private var autoReadEnabled: Boolean = SharedPreferencesSettingsPreferences.DEFAULT_AUTO_READ_TTS,
    private var hapticFeedbackEnabled: Boolean = SharedPreferencesSettingsPreferences.DEFAULT_HAPTIC_FEEDBACK
) : SettingsPreferences {

    override fun getDisplayName(): String = displayName

    override fun setDisplayName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            displayName = trimmed
        }
    }

    override fun getAvatarEmoji(): String = avatarEmoji

    override fun setAvatarEmoji(emoji: String) {
        avatarEmoji = emoji
    }

    override fun isAutoReadEnabled(): Boolean = autoReadEnabled

    override fun setAutoReadEnabled(enabled: Boolean) {
        autoReadEnabled = enabled
    }

    override fun isHapticFeedbackEnabled(): Boolean = hapticFeedbackEnabled

    override fun setHapticFeedbackEnabled(enabled: Boolean) {
        hapticFeedbackEnabled = enabled
    }
}
