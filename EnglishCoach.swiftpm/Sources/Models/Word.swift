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
        wrongCount: Int = 0
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
    }
}
