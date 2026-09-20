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
    
    public struct TargetGoal: Identifiable, Hashable {
        public let id: String
        public let level: String
        public let title: String
        public let subtitle: String
        
        public init(id: String, level: String, title: String, subtitle: String) {
            self.id = id
            self.level = level
            self.title = title
            self.subtitle = subtitle
        }
    }
    
    public static let targetGoals: [TargetGoal] = [
        TargetGoal(id: "550+", level: Word.kLevelBasic, title: "🎯 550+ 基礎", subtitle: "打底必備 · 掌握日常商務基礎單字與基本溝通"),
        TargetGoal(id: "750+", level: Word.kLevelAdvanced, title: "🎯 750+ 進階", subtitle: "商務實用 · 跨國職場溝通、會議談判與商務書信"),
        TargetGoal(id: "860+", level: Word.kLevelGold, title: "🏆 860+ 金證", subtitle: "頂尖金色證書 · 外商高管必備，精通全方位商業策略與專業術語")
    ]
    
    public static let availableTargetScores = [
        "550+": "打底必備 · 掌握日常商務基礎單字與基本溝通",
        "750+": "商務實用 · 跨國職場溝通、會議談判與商務書信",
        "860+": "頂尖金色證書 · 外商高管必備，精通全方位商業策略與專業術語"
    ]
    
    public static let targetScoreOrder = ["550+", "750+", "860+"]
    
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
        let rawScore = UserDefaults.standard.string(forKey: Self.targetScoreKey) ?? "550+"
        if rawScore == "400+" || rawScore == "600+" {
            self.targetScore = "550+"
        } else if rawScore == "730+" || rawScore == "785+" {
            self.targetScore = "750+"
        } else if rawScore == "900+" {
            self.targetScore = "860+"
        } else {
            self.targetScore = rawScore
        }
        let rawLvl = UserDefaults.standard.string(forKey: Self.recommendedLevelKey) ?? Word.kLevelBasic
        switch rawLvl {
        case "Beginner": self.recommendedLevel = Word.kLevelBasic
        case "Intermediate": self.recommendedLevel = Word.kLevelAdvanced
        case "Advanced": self.recommendedLevel = Word.kLevelGold
        default: self.recommendedLevel = rawLvl
        }
        self.placementScore = UserDefaults.standard.integer(forKey: Self.placementScoreKey)
    }
    
    public func completeOnboarding(score: Int? = nil, recommendedLevel: String? = nil) {
        if let s = score {
            self.placementScore = s
        }
        if let lvl = recommendedLevel {
            let normalized = (lvl == "Beginner" ? Word.kLevelBasic : (lvl == "Intermediate" ? Word.kLevelAdvanced : (lvl == "Advanced" ? Word.kLevelGold : lvl)))
            self.recommendedLevel = normalized
            UserDefaults.standard.set(normalized, forKey: "user_level")
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
            return Word.kLevelGold
        } else if correctCount >= 10 {
            return Word.kLevelAdvanced
        } else {
            return Word.kLevelBasic
        }
    }
    
    public func localizedLevelName(_ level: String) -> String {
        switch level {
        case "Beginner", Word.kLevelBasic: return "550+ 基礎"
        case "Intermediate", Word.kLevelAdvanced: return "750+ 進階"
        case "Advanced", Word.kLevelGold: return "860+ 金證"
        default: return level
        }
    }
    
    public func targetGoalTitle(for score: String) -> String {
        if let g = Self.targetGoals.first(where: { $0.id == score }) {
            return g.title
        }
        return score
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
    
    public func getPathWeightMultiplier(level: String) -> Double {
        let currentLevel = UserDefaults.standard.string(forKey: "user_level") ?? (recommendedLevel.isEmpty ? Word.kLevelBasic : recommendedLevel)
        let userTier: String
        switch currentLevel {
        case "Beginner", Word.kLevelBasic: userTier = Word.kLevelBasic
        case "Intermediate", Word.kLevelAdvanced: userTier = Word.kLevelAdvanced
        case "Advanced", Word.kLevelGold: userTier = Word.kLevelGold
        default: userTier = Word.kLevelBasic
        }
        
        let wordTier: String
        switch level {
        case "Beginner", Word.kLevelBasic: wordTier = Word.kLevelBasic
        case "Intermediate", Word.kLevelAdvanced: wordTier = Word.kLevelAdvanced
        case "Advanced", Word.kLevelGold: wordTier = Word.kLevelGold
        default: wordTier = Word.kLevelBasic
        }
        
        // Target path gets 3.0x
        if userTier == wordTier {
            return 3.0
        }
        
        // Adjacent gets 1.0x, Non-target distant gets 0.2x
        if userTier == Word.kLevelBasic {
            return wordTier == Word.kLevelAdvanced ? 1.0 : 0.2
        } else if userTier == Word.kLevelAdvanced {
            return 1.0 // Both Basic and Gold are adjacent to Advanced
        } else { // Word.kLevelGold
            return wordTier == Word.kLevelAdvanced ? 1.0 : 0.2
        }
    }
    
    public func getDifficultyWeightMultiplier(difficulty: Int) -> Double {
        let currentLevel = UserDefaults.standard.string(forKey: "user_level") ?? (recommendedLevel.isEmpty ? Word.kLevelBasic : recommendedLevel)
        let userTier: String
        switch currentLevel {
        case "Beginner", Word.kLevelBasic: userTier = Word.kLevelBasic
        case "Intermediate", Word.kLevelAdvanced: userTier = Word.kLevelAdvanced
        case "Advanced", Word.kLevelGold: userTier = Word.kLevelGold
        default: userTier = Word.kLevelBasic
        }
        
        switch userTier {
        case Word.kLevelBasic:
            // 550+ 聚焦 Lv.1~3
            switch difficulty {
            case 1: return 1.6
            case 2: return 1.4
            case 3: return 1.1
            case 4: return 0.5
            default: return 0.2
            }
        case Word.kLevelAdvanced:
            // 750+ 均衡涵蓋 Lv.2~4
            switch difficulty {
            case 1: return 0.7
            case 2: return 1.2
            case 3: return 1.5
            case 4: return 1.3
            default: return 0.7
            }
        case Word.kLevelGold:
            // 860+ 密集涵蓋 Lv.4~5
            switch difficulty {
            case 1: return 0.2
            case 2: return 0.4
            case 3: return 0.9
            case 4: return 1.8
            default: return 2.2
            }
        default:
            return 1.0
        }
    }
    
    public func calculateTotalWeight(level: String, difficulty: Int, topic: String, subtopic: String) -> Double {
        let wScenario = getScenarioWeightMultiplier(topic: topic, subtopic: subtopic)
        let wPath = getPathWeightMultiplier(level: level)
        let wDiff = getDifficultyWeightMultiplier(difficulty: difficulty)
        return max(0.01, wScenario * wPath * wDiff)
    }
    
    public func calculateTotalWeight(difficulty: Int, topic: String, subtopic: String) -> Double {
        return calculateTotalWeight(level: Word.kLevelBasic, difficulty: difficulty, topic: topic, subtopic: subtopic)
    }
}
