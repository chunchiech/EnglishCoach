package com.andy.englishcoach.onboarding.model

import com.andy.englishcoach.data.model.ToeicTarget

/**
 * Commercial learning scenario (learning context) for personalized onboarding.
 * Source of truth: iOS PersonalizedOnboardingPreferences.availableScenarios.
 */
data class LearningScenario(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconName: String
) {
    val cleanTitle: String
        get() {
            val parts = title.split(" ")
            return if (parts.size > 1) {
                parts.drop(1).joinToString(" ").trim()
            } else {
                title.trim()
            }
        }

    companion object {
        val AVAILABLE_SCENARIOS = listOf(
            LearningScenario(
                id = "business",
                title = "💼 職場商務",
                subtitle = "商業溝通、業務拓展、跨國商務合作",
                iconName = "briefcase"
            ),
            LearningScenario(
                id = "meetings",
                title = "🤝 會議談判",
                subtitle = "跨國會議、商業談判、條款協商",
                iconName = "people"
            ),
            LearningScenario(
                id = "travel",
                title = "✈️ 差旅觀光",
                subtitle = "機票交通、飯店住宿、餐廳點餐",
                iconName = "airplane"
            ),
            LearningScenario(
                id = "email",
                title = "📧 職場書信",
                subtitle = "商務書信、正式備忘錄、通知公告",
                iconName = "mail"
            ),
            LearningScenario(
                id = "career",
                title = "📈 升遷求職",
                subtitle = "英文履歷、外商面試、職涯發展",
                iconName = "trending_up"
            ),
            LearningScenario(
                id = "office",
                title = "🏢 辦公行政",
                subtitle = "日常營運、文件管理、跨部門協調",
                iconName = "business"
            )
        )

        val DEFAULT_SELECTED_SCENARIO_IDS = listOf("business", "meetings")
    }
}

/**
 * Reminder preset time options.
 * Source of truth: iOS NotificationOnboardingView.timeOptions.
 */
data class ReminderTimeOption(
    val id: String,
    val hour: Int,
    val minute: Int,
    val displayTime: String,
    val periodTag: String,
    val iconName: String
) {
    companion object {
        val PRESET_OPTIONS = listOf(
            ReminderTimeOption(
                id = "opt_08",
                hour = 8,
                minute = 0,
                displayTime = "08:00",
                periodTag = "晨間充電",
                iconName = "sun_morning"
            ),
            ReminderTimeOption(
                id = "opt_12",
                hour = 12,
                minute = 0,
                displayTime = "12:00",
                periodTag = "午休放鬆",
                iconName = "sun_noon"
            ),
            ReminderTimeOption(
                id = "opt_19",
                hour = 19,
                minute = 0,
                displayTime = "19:00",
                periodTag = "晚間學習",
                iconName = "sunset"
            ),
            ReminderTimeOption(
                id = "opt_21",
                hour = 21,
                minute = 0,
                displayTime = "21:00",
                periodTag = "睡前複習",
                iconName = "moon"
            )
        )

        const val DEFAULT_SELECTED_OPTION_ID = "opt_19"
    }
}

/**
 * Placement test assessment question model.
 * Source of truth: iOS PlacementQuestion.swift.
 */
data class PlacementQuestion(
    val id: Int,
    val word: String,
    val phonetic: String,
    val options: List<String>,
    val correctAnswer: String,
    val level: String
) {
    companion object {
        /**
         * 20 high-quality placement test questions covering toeic_basic, toeic_advanced, and toeic_gold.
         * Verbatim 100% parity with iOS PlacementQuestion.testQuestions.
         */
        val TEST_QUESTIONS = listOf(
            // 550+ 基礎 (7 questions, difficulty 1-2)
            PlacementQuestion(
                id = 1,
                word = "accept",
                phonetic = "/əkˈsept/",
                options = listOf("接受，同意", "拒絕，放棄", "延期，推遲", "計算，估計"),
                correctAnswer = "接受，同意",
                level = "toeic_basic"
            ),
            PlacementQuestion(
                id = 2,
                word = "confirm",
                phonetic = "/kənˈfɜːrm/",
                options = listOf("確認，證實", "取消，撤回", "討論，協商", "反對，抗議"),
                correctAnswer = "確認，證實",
                level = "toeic_basic"
            ),
            PlacementQuestion(
                id = 3,
                word = "venue",
                phonetic = "/ˈvenjuː/",
                options = listOf("舉辦地點，場館", "交通工具", "演講者", "入場門票"),
                correctAnswer = "舉辦地點，場館",
                level = "toeic_basic"
            ),
            PlacementQuestion(
                id = 4,
                word = "fare",
                phonetic = "/fer/",
                options = listOf("車費，票價", "時刻表", "月台", "行李箱"),
                correctAnswer = "車費，票價",
                level = "toeic_basic"
            ),
            PlacementQuestion(
                id = 5,
                word = "delay",
                phonetic = "/dɪˈleɪ/",
                options = listOf("延遲，耽擱", "提前，提早", "取消，作廢", "出發，啟程"),
                correctAnswer = "延遲，耽擱",
                level = "toeic_basic"
            ),
            PlacementQuestion(
                id = 6,
                word = "notice",
                phonetic = "/ˈnoʊtɪs/",
                options = listOf("通知，公告", "合約，條款", "收據，發票", "投訴，抱怨"),
                correctAnswer = "通知，公告",
                level = "toeic_basic"
            ),
            PlacementQuestion(
                id = 7,
                word = "agenda",
                phonetic = "/əˈdʒendə/",
                options = listOf("議程，討論事項", "會議室", "簽到表", "會議紀錄"),
                correctAnswer = "議程，討論事項",
                level = "toeic_basic"
            ),

            // 750+ 進階 (7 questions, difficulty 2-4)
            PlacementQuestion(
                id = 8,
                word = "budget",
                phonetic = "/ˈbʌdʒɪt/",
                options = listOf("預算", "利息", "帳戶", "借款"),
                correctAnswer = "預算",
                level = "toeic_advanced"
            ),
            PlacementQuestion(
                id = 9,
                word = "invoice",
                phonetic = "/ˈɪnvɔɪs/",
                options = listOf("發票，請款單", "合約，協議", "收據，小票", "備忘錄，公文"),
                correctAnswer = "發票，請款單",
                level = "toeic_advanced"
            ),
            PlacementQuestion(
                id = 10,
                word = "deadline",
                phonetic = "/ˈdedlaɪn/",
                options = listOf("截止日期", "開會時間", "面試日期", "出發時程"),
                correctAnswer = "截止日期",
                level = "toeic_advanced"
            ),
            PlacementQuestion(
                id = 11,
                word = "applicant",
                phonetic = "/ˈæplɪkənt/",
                options = listOf("求職者，申請人", "面試官", "董事長", "客戶代表"),
                correctAnswer = "求職者，申請人",
                level = "toeic_advanced"
            ),
            PlacementQuestion(
                id = 12,
                word = "itinerary",
                phonetic = "/aɪˈtɪnəreri/",
                options = listOf("行程表，路線", "護照，簽證", "機票，登機證", "行李，包裹"),
                correctAnswer = "行程表，路線",
                level = "toeic_advanced"
            ),
            PlacementQuestion(
                id = 13,
                word = "supplier",
                phonetic = "/səˈplaɪər/",
                options = listOf("供應商，供貨商", "消費者", "求職者", "投資人"),
                correctAnswer = "供應商，供貨商",
                level = "toeic_advanced"
            ),
            PlacementQuestion(
                id = 14,
                word = "inventory",
                phonetic = "/ˈɪnvəntɔːri/",
                options = listOf("庫存，盤點", "銷售額", "損益表", "廣告宣傳"),
                correctAnswer = "庫存，盤點",
                level = "toeic_advanced"
            ),

            // 860+ 金證 (6 questions, difficulty 3-5)
            PlacementQuestion(
                id = 15,
                word = "negotiate",
                phonetic = "/nɪˈɡoʊʃieɪt/",
                options = listOf("談判，協商", "執行，實施", "終止，中斷", "指派，分工"),
                correctAnswer = "談判，協商",
                level = "toeic_gold"
            ),
            PlacementQuestion(
                id = 16,
                word = "arbitration",
                phonetic = "/ˌɑːrbɪˈtreɪʃn/",
                options = listOf("仲裁，公斷", "起訴，審判", "宣誓，證明", "上訴，抗告"),
                correctAnswer = "仲裁，公斷",
                level = "toeic_gold"
            ),
            PlacementQuestion(
                id = 17,
                word = "compliance",
                phonetic = "/kəmˈplaɪəns/",
                options = listOf("合規，遵守", "抗辯，異議", "創新，變革", "授權，批准"),
                correctAnswer = "合規，遵守",
                level = "toeic_gold"
            ),
            PlacementQuestion(
                id = 18,
                word = "indemnify",
                phonetic = "/ɪnˈdemnɪfaɪ/",
                options = listOf("賠償，使免受損失", "扣留，扣押", "取消，宣告無效", "借貸，融資"),
                correctAnswer = "賠償，使免受損失",
                level = "toeic_gold"
            ),
            PlacementQuestion(
                id = 19,
                word = "unanimous",
                phonetic = "/juˈnænɪməs/",
                options = listOf("全體一致的", "多數贊成的", "有爭議的", "未決定的"),
                correctAnswer = "全體一致的",
                level = "toeic_gold"
            ),
            PlacementQuestion(
                id = 20,
                word = "prerequisite",
                phonetic = "/ˌpriːˈrekwəzɪt/",
                options = listOf("先決條件，必備要素", "替代方案", "補充條款", "最終結果"),
                correctAnswer = "先決條件，必備要素",
                level = "toeic_gold"
            )
        )
    }
}

/**
 * Scoring policy for Assessment.
 * Source of truth: iOS PersonalizedOnboardingPreferences.calculateRecommendedLevel.
 */
object AssessmentScoringPolicy {
    fun calculateRecommendedLevel(correctCount: Int): ToeicTarget {
        return when {
            correctCount >= 16 -> ToeicTarget.GOLD
            correctCount >= 10 -> ToeicTarget.ADVANCED
            else -> ToeicTarget.BASIC
        }
    }
}
