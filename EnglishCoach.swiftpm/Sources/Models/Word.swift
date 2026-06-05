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
    
    // Difficulty Level
    public var level: String // "Beginner", "Intermediate", "Advanced"
    
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
        level: String = "Beginner"
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
    }
}
