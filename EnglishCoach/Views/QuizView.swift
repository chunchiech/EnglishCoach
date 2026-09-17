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
    @State private var showPaywall = false
    @State private var showTargetReachedAlert = false
    
    @StateObject private var practiceManager = DailyPracticeManager.shared
    @StateObject private var premiumManager = PremiumManager.shared
    
    public var onComplete: () -> Void
    public var onNavigateToReview: (() -> Void)?
    
    public init(onComplete: @escaping () -> Void, onNavigateToReview: (() -> Void)? = nil) {
        self.onComplete = onComplete
        self.onNavigateToReview = onNavigateToReview
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
            .navigationTitle("單字測驗")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("離開") {
                        dismiss()
                    }
                    .foregroundColor(.purple)
                }
            }
            .onAppear {
                if !premiumManager.isPremium && practiceManager.isDailyLimitReached {
                    showPaywall = true
                } else {
                    generateQuiz()
                }
            }
            .sheet(isPresented: $showPaywall) {
                PremiumView()
            }
            .alert("今日目標已達成", isPresented: $showTargetReachedAlert) {
                Button("確定", role: .cancel) {
                    dismiss()
                }
            } message: {
                Text("您已完成今日設定的 \(practiceManager.premiumDailyTarget) 題目標！")
            }
        }
    }
    
    private var emptyQuizView: some View {
        VStack(spacing: 20) {
            Image(systemName: "square.and.pencil")
                .font(.system(size: 60))
                .foregroundColor(.secondary)
            
            Text("無可用於測驗的單字。")
                .font(.system(size: 18, weight: .bold, design: .rounded))
            
            Text("請先學習今日單字以生成測驗題目。")
                .font(.system(size: 14))
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
            
            Button(action: { dismiss() }) {
                Text("返回")
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
                Text("題目 \(currentIndex + 1) / \(questions.count)")
                    .font(.system(size: 14, weight: .bold, design: .rounded))
                    .foregroundColor(.purple)
                Spacer()
                Text("正確：\(correctAnswersCount)")
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
                    Text(currentIndex == questions.count - 1 ? "完成測驗" : "下一題")
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
        }
        
        // Purely in-memory session tracking. Do NOT write to SQLite or DailyPracticeManager here.
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
            // Rule 5: Commit immediately upon completing the 10th question before showing scorecard
            commitQuizSession()
            withAnimation(.spring()) {
                showScorecard = true
            }
        }
    }
    
    private func commitQuizSession() {
        let sessionResults = quizResults.map { ($0.word, $0.isCorrect) }
        let success = DatabaseManager.shared.commitQuizSession(results: sessionResults)
        if success {
            for result in quizResults {
                _ = practiceManager.recordCompletedQuestion(id: "quiz_\(result.word.id)")
            }
            onComplete()
        }
    }
    
    private func generateQuiz() {
        // Fetch today's words to build quiz
        let todayWords = DatabaseManager.shared.getTodayWords(isPremium: premiumManager.isPremium)
        
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
        let wrongWordsCount = questions.count - correctAnswersCount
        let accuracyPercent = questions.isEmpty ? 0 : Int((Double(correctAnswersCount) / Double(questions.count)) * 100)
        
        VStack(spacing: 16) {
            // Score Header
            VStack(spacing: 6) {
                Text("測驗成果驗收")
                    .font(.system(size: 22, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)
                
                Text(wrongWordsCount == 0 ? "🎉 全部掌握，太厲害了！" : "今日完成了一次精準自我檢測")
                    .font(.system(size: 14))
                    .foregroundColor(.secondary)
            }
            .padding(.top, 16)
            
            // 4-Stat Metric Grid
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                metricBox(title: "完成題數", value: "\(questions.count) 題", icon: "checkmark.circle.fill", color: .blue)
                metricBox(title: "答對題數", value: "\(correctAnswersCount) 題", icon: "hand.thumbsup.fill", color: .green)
                metricBox(title: "測驗正確率", value: "\(accuracyPercent)%", icon: "percent", color: .purple)
                metricBox(title: "待加強單字", value: "\(wrongWordsCount) 個", icon: "exclamationmark.triangle.fill", color: wrongWordsCount > 0 ? .orange : .green)
            }
            .padding(.horizontal, 24)
            
            // Detailed breakdown
            VStack(alignment: .leading, spacing: 10) {
                Text("詳細檢討")
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
                                    HStack(spacing: 6) {
                                        Text(result.word.word)
                                            .font(.system(size: 16, weight: .semibold))
                                        
                                        if !result.word.example.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                                            Button(action: {
                                                SoundPlayer.shared.speakSentence(result.word.example)
                                            }) {
                                                Image(systemName: "speaker.wave.2.fill")
                                                    .font(.system(size: 11))
                                                    .foregroundColor(.purple)
                                                    .padding(4)
                                                    .background(Color.purple.opacity(0.1))
                                                    .clipShape(Circle())
                                            }
                                            .buttonStyle(PlainButtonStyle())
                                        }
                                    }
                                    
                                    Text(result.word.translation)
                                        .font(.system(size: 13))
                                        .foregroundColor(.secondary)
                                    
                                    if !result.word.example.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                                        Text(result.word.example)
                                            .font(.system(size: 12))
                                            .foregroundColor(.purple.opacity(0.85))
                                            .lineLimit(2)
                                    }
                                }
                                
                                Spacer()
                                
                                if !result.isCorrect {
                                    VStack(alignment: .trailing, spacing: 2) {
                                        Text("選擇答案：")
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
            
            // Action Buttons
            VStack(spacing: 12) {
                if wrongWordsCount > 0 {
                    Button(action: {
                        onComplete()
                        dismiss()
                        onNavigateToReview?()
                    }) {
                        HStack(spacing: 6) {
                            Text("前往智慧複習中心 ➜")
                            Image(systemName: "calendar.badge.clock")
                        }
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
                    
                    Button(action: {
                        onComplete()
                        dismiss()
                    }) {
                        Text("返回首頁")
                            .font(.system(size: 15, weight: .semibold, design: .rounded))
                            .foregroundColor(.secondary)
                            .padding(.vertical, 4)
                    }
                } else {
                    Button(action: {
                        onComplete()
                        dismiss()
                    }) {
                        Text("太棒了，完成今日學習 ➜")
                            .font(.system(size: 16, weight: .bold, design: .rounded))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(
                                LinearGradient(
                                    colors: [.green, .blue],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .cornerRadius(16)
                            .shadow(color: Color.green.opacity(0.3), radius: 8, x: 0, y: 4)
                    }
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 20)
        }
    }
    
    @ViewBuilder
    private func metricBox(title: String, value: String, icon: String, color: Color) -> some View {
        HStack(spacing: 10) {
            ZStack {
                Circle()
                    .fill(color.opacity(0.12))
                    .frame(width: 34, height: 34)
                Image(systemName: icon)
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(color)
            }
            
            VStack(alignment: .leading, spacing: 2) {
                Text(value)
                    .font(.system(size: 17, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)
                Text(title)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(.secondary)
            }
            Spacer()
        }
        .padding(10)
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(12)
    }
}
