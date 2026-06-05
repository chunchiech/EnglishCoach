import SwiftUI

struct QuizQuestion: Identifiable {
    let id = UUID()
    let word: Word
    let options: [String]
    let correctOption: String
}

public struct QuizView: View {
    @Environment(\.dismiss) private var dismiss
    
    @State private var questions: [QuizQuestion] = []
    @State private var currentIndex = 0
    @State private var selectedOption: String? = nil
    @State private var isAnswered = false
    @State private var correctAnswersCount = 0
    @State private var showScorecard = false
    @State private var quizResults: [(word: Word, isCorrect: Bool, chosenAnswer: String)] = []
    
    public var onComplete: () -> Void
    
    public init(onComplete: @escaping () -> Void) {
        self.onComplete = onComplete
    }
    
    public var body: some View {
        NavigationView {
            ZStack {
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()
                
                if questions.isEmpty {
                    emptyQuizView
                } else if showScorecard {
                    scorecardView
                } else {
                    quizContent
                }
            }
            .navigationTitle("Word Quiz")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Quit") {
                        dismiss()
                    }
                    .foregroundColor(.purple)
                }
            }
            .onAppear {
                generateQuiz()
            }
        }
    }
    
    private var emptyQuizView: some View {
        VStack(spacing: 20) {
            Image(systemName: "square.and.pencil")
                .font(.system(size: 60))
                .foregroundColor(.secondary)
            
            Text("No words available for a quiz.")
                .font(.system(size: 18, weight: .bold, design: .rounded))
            
            Text("Please study today's 20 words first to generate quiz questions.")
                .font(.system(size: 14))
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
            
            Button(action: { dismiss() }) {
                Text("Go Back")
                    .font(.system(size: 16, weight: .bold, design: .rounded))
                    .foregroundColor(.white)
                    .padding(.horizontal, 40)
                    .padding(.vertical, 14)
                    .background(Color.purple)
                    .cornerRadius(12)
            }
        }
    }
    
    private var quizContent: some View {
        let currentQuestion = questions[currentIndex]
        
        return VStack(spacing: 20) {
            // Progress
            HStack {
                Text("Question \(currentIndex + 1) of \(questions.count)")
                    .font(.system(size: 14, weight: .bold, design: .rounded))
                    .foregroundColor(.purple)
                Spacer()
                Text("Correct: \(correctAnswersCount)")
                    .font(.system(size: 14, weight: .semibold, design: .rounded))
                    .foregroundColor(.green)
            }
            .padding(.horizontal, 24)
            .padding(.top, 10)
            
            // Question Card
            VStack(spacing: 16) {
                Spacer()
                
                Text(currentQuestion.word.word)
                    .font(.system(size: 40, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)
                    .multilineTextAlignment(.center)
                
                if !currentQuestion.word.phonetic.isEmpty {
                    Text(currentQuestion.word.phonetic)
                        .font(.system(size: 18, weight: .medium, design: .serif))
                        .foregroundColor(.purple)
                }
                
                Button(action: {
                    SoundPlayer.shared.speak(currentQuestion.word.word)
                }) {
                    Image(systemName: "speaker.wave.3.fill")
                        .font(.system(size: 18))
                        .foregroundColor(.white)
                        .padding(12)
                        .background(Color.purple)
                        .clipShape(Circle())
                }
                .buttonStyle(PlainButtonStyle())
                
                Spacer()
            }
            .frame(maxWidth: .infinity)
            .frame(height: 220)
            .background(
                RoundedRectangle(cornerRadius: 20)
                    .fill(Color(.secondarySystemGroupedBackground))
                    .shadow(color: Color.black.opacity(0.05), radius: 10, x: 0, y: 4)
            )
            .padding(.horizontal, 24)
            
            // Options List
            VStack(spacing: 12) {
                ForEach(currentQuestion.options, id: \.self) { option in
                    optionButton(option: option, question: currentQuestion)
                }
            }
            .padding(.horizontal, 24)
            
            Spacer()
            
            // Next Button
            if isAnswered {
                Button(action: handleNextQuestion) {
                    Text(currentIndex == questions.count - 1 ? "Finish Quiz" : "Next Question")
                        .font(.system(size: 16, weight: .bold, design: .rounded))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(
                            LinearGradient(
                                colors: [.purple, .blue],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .cornerRadius(16)
                        .shadow(color: Color.purple.opacity(0.3), radius: 8, x: 0, y: 4)
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 20)
                .transition(.opacity.combined(with: .move(edge: .bottom)))
            } else {
                Spacer()
                    .frame(height: 72)
            }
        }
    }
    
    @ViewBuilder
    private func optionButton(option: String, question: QuizQuestion) -> some View {
        let isCorrectChoice = option == question.correctOption
        let isSelectedChoice = option == selectedOption
        
        let backgroundColor: Color = {
            if isAnswered {
                if isCorrectChoice {
                    return Color.green.opacity(0.15)
                } else if isSelectedChoice {
                    return Color.red.opacity(0.15)
                }
                return Color(.secondarySystemGroupedBackground)
            }
            return Color(.secondarySystemGroupedBackground)
        }()
        
        let borderColor: Color = {
            if isAnswered {
                if isCorrectChoice {
                    return .green
                } else if isSelectedChoice {
                    return .red
                }
            }
            return .clear
        }()
        
        let textColor: Color = {
            if isAnswered {
                if isCorrectChoice {
                    return .green
                } else if isSelectedChoice {
                    return .red
                }
                return .secondary
            }
            return .primary
        }()
        
        Button(action: {
            if !isAnswered {
                selectOption(option, for: question)
            }
        }) {
            HStack {
                Text(option)
                    .font(.system(size: 16, weight: .semibold, design: .rounded))
                    .foregroundColor(textColor)
                Spacer()
                if isAnswered {
                    if isCorrectChoice {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundColor(.green)
                    } else if isSelectedChoice {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.red)
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
            .background(
                RoundedRectangle(cornerRadius: 14)
                    .fill(backgroundColor)
                    .overlay(
                        RoundedRectangle(cornerRadius: 14)
                            .stroke(borderColor, lineWidth: 2)
                    )
                    .shadow(color: Color.black.opacity(0.02), radius: 4, x: 0, y: 2)
            )
        }
        .disabled(isAnswered)
        .buttonStyle(PlainButtonStyle())
    }
    
    private func selectOption(_ option: String, for question: QuizQuestion) {
        selectedOption = option
        isAnswered = true
        
        let isCorrect = option == question.correctOption
        if isCorrect {
            correctAnswersCount += 1
            DatabaseManager.shared.incrementCorrectCount(wordId: question.word.id)
        } else {
            DatabaseManager.shared.incrementWrongCount(wordId: question.word.id)
        }
        
        quizResults.append((word: question.word, isCorrect: isCorrect, chosenAnswer: option))
    }
    
    private func handleNextQuestion() {
        if currentIndex < questions.count - 1 {
            selectedOption = nil
            isAnswered = false
            withAnimation(.easeInOut) {
                currentIndex += 1
            }
        } else {
            withAnimation(.spring()) {
                showScorecard = true
            }
        }
    }
    
    private func generateQuiz() {
        // Fetch today's words to build quiz
        let todayWords = DatabaseManager.shared.getTodayWords()
        
        if todayWords.isEmpty {
            return
        }
        
        // Take up to 10 random words from today's list for the quiz
        let selectedWords = todayWords.shuffled().prefix(10)
        
        var generatedQuestions: [QuizQuestion] = []
        for word in selectedWords {
            let correctOption = word.translation
            var distractors = DatabaseManager.shared.getRandomDistractors(excludeWordId: word.id, count: 3)
            
            // In case the DB is small and we don't have enough distractors, fill it with placeholders
            while distractors.count < 3 {
                distractors.append("備選答案 \(distractors.count + 1)")
            }
            
            var options = distractors
            options.append(correctOption)
            options.shuffle()
            
            generatedQuestions.append(QuizQuestion(word: word, options: options, correctOption: correctOption))
        }
        
        self.questions = generatedQuestions
    }
    
    @ViewBuilder
    private var scorecardView: some View {
        VStack(spacing: 20) {
            // Score Header
            VStack(spacing: 8) {
                Text("Quiz Results")
                    .font(.system(size: 24, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)
                
                Text("\(correctAnswersCount) / \(questions.count) Correct")
                    .font(.system(size: 36, weight: .bold, design: .rounded))
                    .foregroundColor(correctAnswersCount >= 6 ? .green : .orange)
                
                Text("Accuracy: \(Int((Double(correctAnswersCount) / Double(questions.count)) * 100))%")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(.secondary)
            }
            .padding(.top, 20)
            
            // Detailed breakdown
            VStack(alignment: .leading, spacing: 10) {
                Text("Detailed Review")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 24)
                
                ScrollView {
                    VStack(spacing: 10) {
                        ForEach(0..<quizResults.count, id: \.self) { index in
                            let result = quizResults[index]
                            HStack(spacing: 12) {
                                Image(systemName: result.isCorrect ? "checkmark.circle.fill" : "xmark.circle.fill")
                                    .foregroundColor(result.isCorrect ? .green : .red)
                                    .font(.title3)
                                
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(result.word.word)
                                        .font(.system(size: 16, weight: .semibold))
                                    Text(result.word.translation)
                                        .font(.system(size: 13))
                                        .foregroundColor(.secondary)
                                }
                                
                                Spacer()
                                
                                if !result.isCorrect {
                                    VStack(alignment: .trailing, spacing: 2) {
                                        Text("Answered:")
                                            .font(.system(size: 10))
                                            .foregroundColor(.secondary)
                                        Text(result.chosenAnswer)
                                            .font(.system(size: 12, weight: .medium))
                                            .foregroundColor(.red)
                                    }
                                }
                            }
                            .padding()
                            .background(Color(.secondarySystemGroupedBackground))
                            .cornerRadius(12)
                        }
                    }
                    .padding(.horizontal, 24)
                }
            }
            
            Spacer()
            
            Button(action: {
                onComplete()
                dismiss()
            }) {
                Text("Finish")
                    .font(.system(size: 16, weight: .bold, design: .rounded))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(
                        LinearGradient(
                            colors: [.purple, .blue],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .cornerRadius(16)
                    .shadow(color: Color.purple.opacity(0.3), radius: 8, x: 0, y: 4)
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 20)
        }
    }
}
