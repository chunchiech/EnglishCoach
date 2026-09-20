import Foundation

public enum ToeicTarget: String, CaseIterable, Identifiable, Codable {
    case basic = "toeic_basic"
    case advanced = "toeic_advanced"
    case gold = "toeic_gold"
    
    public var id: String { rawValue }
    public var rawLevel: String { rawValue }
    
    public var displayName: String {
        switch self {
        case .basic: return "550+ 基礎"
        case .advanced: return "750+ 進階"
        case .gold: return "860+ 金證"
        }
    }
    
    public var shortScore: String {
        switch self {
        case .basic: return "550+"
        case .advanced: return "750+"
        case .gold: return "860+"
        }
    }
    
    public var targetScoreString: String {
        shortScore
    }
    
    public static func from(rawString: String) -> ToeicTarget {
        switch rawString {
        case "toeic_basic", "Beginner", "550+":
            return .basic
        case "toeic_advanced", "Intermediate", "750+":
            return .advanced
        case "toeic_gold", "Advanced", "860+":
            return .gold
        default:
            if rawString.contains("860") || rawString.contains("金證") || rawString.lowercased().contains("gold") {
                return .gold
            } else if rawString.contains("750") || rawString.contains("進階") || rawString.lowercased().contains("advanced") {
                return .advanced
            } else {
                return .basic
            }
        }
    }
}

public struct Word: Identifiable, Codable, Hashable {
    public let id: Int
    public let word: String
    public let phonetic: String
    public let translation: String
    public let example: String
    public let exampleTranslation: String
    public var learned: Bool
    public var learnedDate: String?
    public var correctCount: Int
    public var wrongCount: Int
    
    // SM-2 Spaced Repetition Fields
    public var easinessFactor: Double
    public var intervalDays: Int
    public var repetitionCount: Int
    public var nextReviewDate: String?
    
    // TOEIC 3-Tier Target Level
    public static let kLevelBasic = ToeicTarget.basic.rawValue
    public static let kLevelAdvanced = ToeicTarget.advanced.rawValue
    public static let kLevelGold = ToeicTarget.gold.rawValue
    
    // Target Level ("toeic_basic", "toeic_advanced", "toeic_gold", backwards-compatible with legacy levels)
    public var level: String
    
    public var toeicTarget: ToeicTarget {
        ToeicTarget.from(rawString: level)
    }
    
    public var normalizedLevel: String {
        toeicTarget.rawValue
    }
    
    public var displayLevelName: String {
        toeicTarget.displayName
    }
    
    // Metadata fields
    public var difficulty: Int // 1 to 5
    public var topic: String
    public var subtopic: String
    public var examTags: [String]
    public var partOfSpeech: String
    
    public static func normalizePhonetic(_ raw: String) -> String {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.contains(", /") {
            let parts = trimmed.components(separatedBy: ", /")
            if let first = parts.first, !first.isEmpty {
                return first.hasSuffix("/") ? first : "\(first)/"
            }
        } else if trimmed.contains("; /") {
            let parts = trimmed.components(separatedBy: "; /")
            if let first = parts.first, !first.isEmpty {
                return first.hasSuffix("/") ? first : "\(first)/"
            }
        }
        return trimmed
    }
    
    public static func normalizeTraditionalChinese(_ text: String) -> String {
        var result = text
        let map = [
            ("伙计们，朋友们，同事们", "夥伴們，朋友們，同事們"),
            ("伙计们，我们来回顾一下第三季度的销售数据。", "夥伴們，我們來回顧一下第三季度的銷售數據。"),
            ("伙伴们", "夥伴們"),
            ("销售数据", "銷售數據"),
            ("数据流", "數據流"),
            ("紮实的", "紮實的"),
            ("声音", "聲音"),
            ("市场需求", "市場需求"),
            ("独占", "獨佔"),
            ("獨占", "獨佔"),
            ("正面临诉讼，下个月需要出庭", "正面臨訴訟，下個月需要出庭"),
            ("更低的价格", "更低的價格")
        ]
        for (s, t) in map {
            result = result.replacingOccurrences(of: s, with: t)
        }
        return result
    }

    public init(
        id: Int,
        word: String,
        phonetic: String,
        translation: String,
        example: String,
        exampleTranslation: String,
        learned: Bool = false,
        learnedDate: String? = nil,
        correctCount: Int = 0,
        wrongCount: Int = 0,
        easinessFactor: Double = 2.5,
        intervalDays: Int = 0,
        repetitionCount: Int = 0,
        nextReviewDate: String? = nil,
        level: String = "toeic_basic",
        difficulty: Int = 1,
        topic: String = "",
        subtopic: String = "",
        examTags: [String] = ["TOEIC"],
        partOfSpeech: String = ""
    ) {
        self.id = id
        self.word = word
        self.phonetic = Self.normalizePhonetic(phonetic)
        self.translation = Self.normalizeTraditionalChinese(translation)
        self.example = example
        self.exampleTranslation = Self.normalizeTraditionalChinese(exampleTranslation)
        self.learned = learned
        self.learnedDate = learnedDate
        self.correctCount = correctCount
        self.wrongCount = wrongCount
        self.easinessFactor = easinessFactor
        self.intervalDays = intervalDays
        self.repetitionCount = repetitionCount
        self.nextReviewDate = nextReviewDate
        self.level = level
        self.difficulty = difficulty
        self.topic = topic
        self.subtopic = subtopic
        self.examTags = examTags
        self.partOfSpeech = partOfSpeech
    }
}
