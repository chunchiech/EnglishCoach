import Foundation

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
    public static let kLevelBasic = "toeic_basic"
    public static let kLevelAdvanced = "toeic_advanced"
    public static let kLevelGold = "toeic_gold"
    
    // Target Level ("toeic_basic", "toeic_advanced", "toeic_gold", backwards-compatible with legacy levels)
    public var level: String
    
    public var normalizedLevel: String {
        switch level {
        case "Beginner": return Self.kLevelBasic
        case "Intermediate": return Self.kLevelAdvanced
        case "Advanced": return Self.kLevelGold
        default: return level
        }
    }
    
    public var displayLevelName: String {
        switch normalizedLevel {
        case Self.kLevelBasic: return "550+ 基礎"
        case Self.kLevelAdvanced: return "750+ 進階"
        case Self.kLevelGold: return "860+ 金證"
        default: return level
        }
    }
    
    // Metadata fields
    public var difficulty: Int // 1 to 5
    public var topic: String
    public var subtopic: String
    public var examTags: [String]
    public var partOfSpeech: String
    
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
        self.phonetic = phonetic
        self.translation = translation
        self.example = example
        self.exampleTranslation = exampleTranslation
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
