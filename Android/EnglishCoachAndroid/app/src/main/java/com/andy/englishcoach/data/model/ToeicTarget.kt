package com.andy.englishcoach.data.model

enum class ToeicTarget(val rawLevel: String, val displayName: String, val targetScore: String) {
    BASIC("toeic_basic", "550+ 基礎", "550+"),
    ADVANCED("toeic_advanced", "750+ 進階", "750+"),
    GOLD("toeic_gold", "860+ 金證", "860+");

    companion object {
        fun from(rawString: String): ToeicTarget {
            return when (rawString.trim()) {
                "toeic_basic", "Beginner", "550+" -> BASIC
                "toeic_advanced", "Intermediate", "750+" -> ADVANCED
                "toeic_gold", "Advanced", "860+" -> GOLD
                else -> {
                    val lower = rawString.lowercase()
                    when {
                        lower.contains("860") || lower.contains("金證") || lower.contains("gold") -> GOLD
                        lower.contains("750") || lower.contains("進階") || lower.contains("advanced") -> ADVANCED
                        else -> BASIC
                    }
                }
            }
        }
    }
}
