import Foundation

public struct LearningScenario: Identifiable, Hashable {
    public let id: String
    public let title: String
    public let subtitle: String
    public let icon: String
    
    public init(id: String, title: String, subtitle: String, icon: String) {
        self.id = id
        self.title = title
        self.subtitle = subtitle
        self.icon = icon
    }
}

public class PersonalizedOnboardingPreferences: ObservableObject {
    public static let shared = PersonalizedOnboardingPreferences()
    
    public static let onboardingCompletedKey = "hasCompletedPersonalizedOnboarding"
    public static let selectedScenariosKey = "userSelectedLearningScenarios"
    public static let targetScoreKey = "userToeicTargetScore"
    public static let placementScoreKey = "userPlacementTestScore"
    public static let recommendedLevelKey = "userRecommendedStartingLevel"
    
    public static let availableScenarios: [LearningScenario] = [
        LearningScenario(id: "business", title: "💼 職場商務", subtitle: "商業溝通、業務拓展、跨國商務合作", icon: "briefcase.fill"),
        LearningScenario(id: "meetings", title: "🤝 會議談判", subtitle: "跨國會議、商業談判、條款協商", icon: "person.2.fill"),
        LearningScenario(id: "travel", title: "✈️ 差旅觀光", subtitle: "機票交通、飯店住宿、餐廳點餐", icon: "airplane"),
        LearningScenario(id: "email", title: "📧 職場書信", subtitle: "商務書信、正式備忘錄、通知公告", icon: "envelope.fill"),
        LearningScenario(id: "career", title: "📈 升遷求職", subtitle: "英文履歷、外商面試、職涯發展", icon: "chart.line.uptrend.xyaxis"),
        LearningScenario(id: "office", title: "🏢 辦公行政", subtitle: "日常營運、文件管理、跨部門協調", icon: "building.2.fill")
    ]
    
    public static let availableTargetScores = [
        "400+": "基礎起步 · 掌握日常商務基礎單字",
        "600+": "職場門檻 · 多數企業基本英文門檻",
        "730+": "商務實用 · 能以英語進行日常業務溝通",
        "785+": "外商常態 · 跨國會議與流暢工作能力",
        "860+": "商務流利 · 精準掌握各領域商務英語",
        "900+": "專家精通 · 母語等級高階專業商務詞彙"
    ]
    
    public static let targetScoreOrder = ["400+", "600+", "730+", "785+", "860+", "900+"]
    
    @Published public var selectedScenarioIds: [String] {
        didSet {
            UserDefaults.standard.set(selectedScenarioIds, forKey: Self.selectedScenariosKey)
        }
    }
    
    @Published public var targetScore: String {
        didSet {
            UserDefaults.standard.set(targetScore, forKey: Self.targetScoreKey)
        }
    }
    
    @Published public var recommendedLevel: String {
        didSet {
            UserDefaults.standard.set(recommendedLevel, forKey: Self.recommendedLevelKey)
        }
    }
    
    @Published public var placementScore: Int {
        didSet {
            UserDefaults.standard.set(placementScore, forKey: Self.placementScoreKey)
        }
    }
    
    public var hasCompletedOnboarding: Bool {
        return UserDefaults.standard.bool(forKey: Self.onboardingCompletedKey)
    }
    
    public init() {
        self.selectedScenarioIds = UserDefaults.standard.stringArray(forKey: Self.selectedScenariosKey) ?? ["business", "meetings"]
        self.targetScore = UserDefaults.standard.string(forKey: Self.targetScoreKey) ?? "730+"
        self.recommendedLevel = UserDefaults.standard.string(forKey: Self.recommendedLevelKey) ?? "Intermediate"
        self.placementScore = UserDefaults.standard.integer(forKey: Self.placementScoreKey)
    }
    
    public func completeOnboarding(score: Int? = nil, recommendedLevel: String? = nil) {
        if let s = score {
            self.placementScore = s
        }
        if let lvl = recommendedLevel {
            self.recommendedLevel = lvl
            UserDefaults.standard.set(lvl, forKey: "user_level")
        }
        UserDefaults.standard.set(true, forKey: Self.onboardingCompletedKey)
    }
    
    public func toggleScenario(_ id: String) {
        if let index = selectedScenarioIds.firstIndex(of: id) {
            // Keep at least one scenario selected
            if selectedScenarioIds.count > 1 {
                selectedScenarioIds.remove(at: index)
            }
        } else {
            selectedScenarioIds.append(id)
        }
    }
    
    public func calculateRecommendedLevel(correctCount: Int) -> String {
        if correctCount >= 16 {
            return "Advanced"
        } else if correctCount >= 10 {
            return "Intermediate"
        } else {
            return "Beginner"
        }
    }
    
    public func localizedLevelName(_ level: String) -> String {
        switch level {
        case "Beginner": return "初級 (Beginner)"
        case "Intermediate": return "中級 (Intermediate)"
        case "Advanced": return "進階 (Advanced)"
        default: return level
        }
    }
    
    public static func matchesScenario(scenarioId: String, topic: String, subtopic: String) -> Bool {
        let t = topic.lowercased()
        let st = subtopic.lowercased()
        
        switch scenarioId {
        case "business":
            let isOfficeOrMeeting = st.contains("meetings") || st.contains("administration") || st.contains("facilities")
            return (!isOfficeOrMeeting && (t == "corporate" || t == "marketing" || t == "finance")) ||
                   st.contains("business") || st.contains("trade") || st.contains("sales")
        case "meetings":
            return st.contains("meetings") || st.contains("strategy") || st.contains("contracts") ||
                   st.contains("negotiation") || st.contains("alliances")
        case "travel":
            return t == "travel" || st.contains("dining") || st.contains("transit") ||
                   st.contains("hospitality") || st.contains("ticketing") || st.contains("tours") || st.contains("flight")
        case "email":
            return st.contains("documentation") || st.contains("dailywork") || st.contains("publicrelations") ||
                   st.contains("customerrelations") || st.contains("customerservice")
        case "career":
            return t == "humanresources" || st.contains("recruitment") || st.contains("qualifications") ||
                   st.contains("development") || st.contains("payroll") || st.contains("performance") || st.contains("staff")
        case "office":
            return st.contains("administration") || st.contains("facilities") || st.contains("dailywork") ||
                   st.contains("workplace") || st.contains("operations")
        default:
            return false
        }
    }
    
    public static func getScenarioBreadcrumb(topic: String, subtopic: String) -> String {
        for scenario in availableScenarios {
            if matchesScenario(scenarioId: scenario.id, topic: topic, subtopic: subtopic) {
                if !subtopic.isEmpty && subtopic != "GeneralBusiness" && subtopic != "General" {
                    return "\(scenario.title) › \(subtopic)"
                }
                return scenario.title
            }
        }
        if !topic.isEmpty {
            return "💼 \(topic)"
        }
        return "💼 職場商務"
    }
    
    public func getScenarioWeightMultiplier(topic: String, subtopic: String) -> Double {
        // Priority order: 1st priority: 3.0x, 2nd priority: 2.0x, 3rd+ priority: 1.5x, non-selected: 1.0x
        for (index, scenarioId) in selectedScenarioIds.enumerated() {
            if Self.matchesScenario(scenarioId: scenarioId, topic: topic, subtopic: subtopic) {
                if index == 0 {
                    return 3.0
                } else if index == 1 {
                    return 2.0
                } else {
                    return 1.5
                }
            }
        }
        return 1.0
    }
    
    public func getLevelWeightMultiplier(difficulty: Int) -> Double {
        let currentLevel = recommendedLevel.isEmpty ? (UserDefaults.standard.string(forKey: "user_level") ?? "Beginner") : recommendedLevel
        switch currentLevel {
        case "Beginner":
            if difficulty <= 2 {
                return 2.0 // 完全符合程度
            } else if difficulty == 3 {
                return 1.0 // 相鄰程度
            } else if difficulty == 4 {
                return 0.6 // 較難但可接受
            } else {
                return 0.35 // 明顯過難
            }
        case "Intermediate":
            if difficulty == 3 {
                return 2.0 // 完全符合程度
            } else if difficulty == 2 || difficulty == 4 {
                return 1.0 // 相鄰程度
            } else if difficulty == 5 {
                return 0.6 // 較難但可接受
            } else {
                return 0.35 // 明顯過難/差距過大
            }
        case "Advanced":
            if difficulty >= 4 {
                return 2.0 // 完全符合程度
            } else if difficulty == 3 {
                return 1.0 // 相鄰程度
            } else if difficulty == 2 {
                return 0.6 // 較難但可接受
            } else {
                return 0.35 // 明顯過難/差距過大
            }
        default:
            return 1.0
        }
    }
    
    public func getTargetScoreWeightMultiplier(difficulty: Int) -> Double {
        let score = targetScore
        if score.contains("400") || score.contains("600") {
            return (difficulty <= 2) ? 1.4 : 1.0
        } else if score.contains("730") || score.contains("785") {
            return (difficulty == 3 || difficulty == 4) ? 1.4 : 1.0
        } else if score.contains("860") || score.contains("900") {
            return (difficulty >= 4) ? 1.4 : 1.0
        }
        return 1.0
    }
    
    public func calculateTotalWeight(difficulty: Int, topic: String, subtopic: String) -> Double {
        let wScenario = getScenarioWeightMultiplier(topic: topic, subtopic: subtopic)
        let wLevel = getLevelWeightMultiplier(difficulty: difficulty)
        let wTarget = getTargetScoreWeightMultiplier(difficulty: difficulty)
        return max(0.01, 1.0 * wScenario * wLevel * wTarget)
    }
}
