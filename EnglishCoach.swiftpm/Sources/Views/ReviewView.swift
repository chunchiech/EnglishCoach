import SwiftUI

public struct ReviewView: View {
    @Environment(\.dismiss) private var dismiss
    
    @State private var errorWords: [Word] = []
    @State private var selectedWord: Word? = nil
    @State private var showQuizMode = false
    
    // Quiz state for error review
    @State private var quizQuestions: [QuizQuestion] = []
    @State private var quizIndex = 0
    @State private var selectedOption: String? = nil
    @State private var isAnswered = false
    @State private var correctCount = 0
    @State private var quizResults: [(word: Word, isCorrect: Bool)] = []
    @State private var showQuizResults = false
    @State private var showPaywall = false
    @State private var showTargetReachedAlert = false
    
    @StateObject private var practiceManager = DailyPracticeManager.shared
    @StateObject private var premiumManager = PremiumManager.shared
    
    public init() {}
    
    public var body: some View {
        NavigationView {
            ZStack {
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()
                
                if showQuizMode {
                    if showQuizResults {
                        reviewQuizResultsView
                    } else if !quizQuestions.isEmpty {
                        reviewQuizContent
                    } else {
                        emptyStateView
                    }
                } else {
                    if errorWords.isEmpty {
                        emptyStateView
                    } else {
                        listView
                    }
                }
            }
            .navigationTitle(showQuizMode ? "複習測驗" : "智慧複習中心")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    if showQuizMode {
                        Button("離開測驗") {
                            withAnimation {
                                showQuizMode = false
                                loadErrorWords()
                            }
                        }
                        .foregroundColor(.purple)
                    } else {
                        Button("關閉") {
                            dismiss()
                        }
                        .foregroundColor(.purple)
                    }
                }
            }
            .sheet(item: $selectedWord) { word in
                cardReviewSheet(word: word)
            }
            .sheet(isPresented: $showPaywall) {
                PremiumView()
            }
            .alert("今日目標已達成", isPresented: $showTargetReachedAlert) {
                Button("確定", role: .cancel) { }
            } message: {
                Text("您已完成今日設定的 \(practiceManager.premiumDailyTarget) 題目標！")
            }
            .onAppear {
                loadErrorWords()
            }
        }
    }
    
    private var listView: some View {
        VStack(spacing: 0) {
            // Stats Header
            HStack(spacing: 16) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("\(errorWords.count) 個單字")
                        .font(.system(size: 20, weight: .bold, design: .rounded))
                    Text("需要加強練習的單字")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                }
                Spacer()
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 16)
            
            // List of words
            List {
                ForEach(errorWords) { word in
                    Button(action: { selectedWord = word }) {
                        HStack(alignment: .top) {
                            VStack(alignment: .leading, spacing: 5) {
                                // Scenario Breadcrumb & Topic / Subtopic
                                Text(PersonalizedOnboardingPreferences.getScenarioBreadcrumb(topic: word.topic, subtopic: word.subtopic))
                                    .font(.system(size: 11, weight: .semibold, design: .rounded))
                                    .foregroundColor(.purple)
                                
                                HStack(alignment: .firstTextBaseline, spacing: 8) {
                                    Text(word.word)
                                        .font(.system(size: 18, weight: .bold, design: .rounded))
                                        .foregroundColor(.primary)
                                    
                                    // Target Level & Part of Speech chips
                                    HStack(spacing: 4) {
                                        Text(word.displayLevelName)
                                            .font(.system(size: 11, weight: .bold, design: .rounded))
                                            .foregroundColor(.blue)
                                            .padding(.horizontal, 6)
                                            .padding(.vertical, 2)
                                            .background(Color.blue.opacity(0.1))
                                            .cornerRadius(6)
                                        
                                        if !word.partOfSpeech.isEmpty {
                                            Text(word.partOfSpeech)
                                                .font(.system(size: 11, weight: .semibold, design: .rounded))
                                                .foregroundColor(.secondary)
                                                .padding(.horizontal, 6)
                                                .padding(.vertical, 2)
                                                .background(Color(.systemGray6))
                                                .cornerRadius(6)
                                        }
                                    }
                                }
                                
                                HStack(spacing: 8) {
                                    if !word.phonetic.isEmpty {
                                        Text(word.phonetic)
                                            .font(.system(size: 13, weight: .medium, design: .serif))
                                            .foregroundColor(.purple)
                                    }
                                    
                                    Text(word.translation)
                                        .font(.system(size: 14))
                                        .foregroundColor(.secondary)
                                        .lineLimit(1)
                                }
                            }
                            
                            Spacer()
                            
                            if word.wrongCount > 0 {
                                HStack(spacing: 6) {
                                    Image(systemName: "exclamationmark.triangle.fill")
                                        .font(.caption)
                                    Text("\(word.wrongCount) 次錯誤")
                                        .font(.system(size: 12, weight: .bold))
                                }
                                .foregroundColor(.orange)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 4)
                                .background(Color.orange.opacity(0.1))
                                .cornerRadius(8)
                            } else {
                                HStack(spacing: 6) {
                                    Image(systemName: "calendar.badge.clock")
                                        .font(.caption)
                                    Text("排程複習")
                                        .font(.system(size: 12, weight: .bold))
                                }
                                .foregroundColor(.blue)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 4)
                                .background(Color.blue.opacity(0.1))
                                .cornerRadius(8)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                }
            }
            .listStyle(InsetGroupedListStyle())
            
            // Bottom Action CTA
            VStack(spacing: 0) {
                Divider()
                
                Button(action: startReviewQuiz) {
                    Text(quizButtonTitle)
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
                .padding(.top, 12)
                .padding(.bottom, 12)
            }
            .background(Color(.systemGroupedBackground))
        }
    }
    
    private var remainingQuota: Int {
        if premiumManager.isPremium {
            if practiceManager.premiumDailyTarget == -1 {
                return 10
            }
            return max(0, practiceManager.premiumDailyTarget - practiceManager.todayCompletedCount)
        }
        return practiceManager.remainingFreeQuestions
    }
    
    private var quizQuestionCount: Int {
        let maxAllowed = remainingQuota
        return min(errorWords.count, min(10, maxAllowed))
    }
    
    private var quizButtonTitle: String {
        if !premiumManager.isPremium && practiceManager.isDailyLimitReached {
            return "解鎖 Premium 進行複習測驗 ➜"
        } else if premiumManager.isPremium && practiceManager.isDailyLimitReached {
            return "今日目標已達成（共 \(practiceManager.premiumDailyTarget) 題）"
        } else {
            return "開始複習測驗（共 \(quizQuestionCount) 題）➜"
        }
    }
    
    private var isTodayQuizCompleted: Bool {
        DatabaseManager.shared.isTodayQuizCompleted()
    }
    
    private var emptyStateView: some View {
        VStack(spacing: 24) {
            ZStack {
                Circle()
                    .fill(Color.green.opacity(0.1))
                    .frame(width: 100, height: 100)
                Image(systemName: "checkmark.seal.fill")
                    .font(.system(size: 50))
                    .foregroundColor(.green)
            }
            
            VStack(spacing: 8) {
                Text("太棒了！目前無待複習單字")
                    .font(.system(size: 22, weight: .bold, design: .rounded))
                Text("測驗中答錯或需要強化的單字，會自動收錄在智慧複習中心，依照記憶曲線排程複習。")
                    .font(.system(size: 14))
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
            }
            
            Button(action: { dismiss() }) {
                Text("返回首頁")
                    .font(.system(size: 16, weight: .semibold, design: .rounded))
                    .foregroundColor(.white)
                    .padding(.horizontal, 32)
                    .padding(.vertical, 14)
                    .background(Color.purple)
                    .cornerRadius(12)
            }
        }
    }
    
    private var reviewQuizContent: some View {
        let currentQuestion = quizQuestions[quizIndex]
        
        return VStack(spacing: 20) {
            // Progress
            HStack {
                Text("複習 \(quizIndex + 1) / \(quizQuestions.count)")
                    .font(.system(size: 14, weight: .bold, design: .rounded))
                    .foregroundColor(.purple)
                Spacer()
                Text("正確：\(correctCount)")
                    .font(.system(size: 14, weight: .semibold, design: .rounded))
                    .foregroundColor(.green)
            }
            .padding(.horizontal, 24)
            .padding(.top, 10)
            
            // Question Card
            VStack(spacing: 16) {
                Spacer()
                Text(currentQuestion.word.word)
                    .font(.system(size: 38, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)
                
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
            .frame(height: 200)
            .background(
                RoundedRectangle(cornerRadius: 20)
                    .fill(Color(.secondarySystemGroupedBackground))
                    .shadow(color: Color.black.opacity(0.05), radius: 10, x: 0, y: 4)
            )
            .padding(.horizontal, 24)
            
            // Options
            VStack(spacing: 12) {
                ForEach(currentQuestion.options, id: \.self) { option in
                    reviewOptionButton(option: option, question: currentQuestion)
                }
            }
            .padding(.horizontal, 24)
            
            Spacer()
            
            if isAnswered {
                Button(action: handleNextReviewQuizQuestion) {
                    Text(quizIndex == quizQuestions.count - 1 ? "完成複習" : "下一個單字")
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
            } else {
                Spacer()
                    .frame(height: 72)
            }
        }
    }
    
    @ViewBuilder
    private func reviewOptionButton(option: String, question: QuizQuestion) -> some View {
        let isCorrectChoice = option == question.correctOption
        let isSelectedChoice = option == selectedOption
        
        let backgroundColor: Color = {
            if isAnswered {
                if isCorrectChoice { return Color.green.opacity(0.15) }
                if isSelectedChoice { return Color.red.opacity(0.15) }
            }
            return Color(.secondarySystemGroupedBackground)
        }()
        
        let borderColor: Color = {
            if isAnswered {
                if isCorrectChoice { return .green }
                if isSelectedChoice { return .red }
            }
            return .clear
        }()
        
        let textColor: Color = {
            if isAnswered {
                if isCorrectChoice { return .green }
                if isSelectedChoice { return .red }
                return .secondary
            }
            return .primary
        }()
        
        Button(action: {
            if !isAnswered {
                selectReviewOption(option, for: question)
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
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(borderColor, lineWidth: 2))
            )
        }
        .disabled(isAnswered)
        .buttonStyle(PlainButtonStyle())
    }
    
    private func selectReviewOption(_ option: String, for question: QuizQuestion) {
        selectedOption = option
        isAnswered = true
        
        let isCorrect = option == question.correctOption
        if isCorrect {
            correctCount += 1
            DatabaseManager.shared.decrementWrongCount(wordId: question.word.id)
        } else {
            DatabaseManager.shared.incrementWrongCount(wordId: question.word.id)
        }
        quizResults.append((word: question.word, isCorrect: isCorrect))
        _ = DailyPracticeManager.shared.recordCompletedQuestion(id: "review_\(question.word.id)")
    }
    
    private func handleNextReviewQuizQuestion() {
        if quizIndex < quizQuestions.count - 1 {
            if !premiumManager.isPremium && practiceManager.isDailyLimitReached {
                showPaywall = true
                return
            }
            if premiumManager.isPremium && practiceManager.isDailyLimitReached {
                showTargetReachedAlert = true
                return
            }
            selectedOption = nil
            isAnswered = false
            withAnimation(.easeInOut) {
                quizIndex += 1
            }
        } else {
            withAnimation(.spring()) {
                showQuizResults = true
            }
        }
    }
    
    private var reviewQuizResultsView: some View {
        VStack(spacing: 20) {
            VStack(spacing: 8) {
                Text("複習完成")
                    .font(.system(size: 24, weight: .bold, design: .rounded))
                
                Text("已解決 \(correctCount) / \(quizQuestions.count)")
                    .font(.system(size: 36, weight: .bold, design: .rounded))
                    .foregroundColor(correctCount == quizQuestions.count ? .green : .purple)
                
                Text("答對的單字已更新複習排程與紀錄。")
                    .font(.system(size: 14))
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
            }
            .padding(.top, 20)
            
            ScrollView {
                VStack(spacing: 10) {
                    ForEach(0..<quizResults.count, id: \.self) { index in
                        let result = quizResults[index]
                        HStack(spacing: 12) {
                            Image(systemName: result.isCorrect ? "checkmark.circle.fill" : "arrow.clockwise")
                                .foregroundColor(result.isCorrect ? .green : .orange)
                            
                            VStack(alignment: .leading, spacing: 4) {
                                HStack(spacing: 6) {
                                    Text(result.word.word)
                                        .font(.system(size: 16, weight: .bold))
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
                            
                            Text(result.isCorrect ? "已解決" : "需練習")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(result.isCorrect ? .green : .orange)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(result.isCorrect ? Color.green.opacity(0.1) : Color.orange.opacity(0.1))
                                .cornerRadius(6)
                        }
                        .padding()
                        .background(Color(.secondarySystemGroupedBackground))
                        .cornerRadius(12)
                    }
                }
                .padding(.horizontal, 24)
            }
            
            VStack(spacing: 12) {
                Button(action: {
                    withAnimation {
                        showQuizMode = false
                        showQuizResults = false
                        loadErrorWords()
                    }
                }) {
                    Text("返回智慧複習中心")
                        .font(.system(size: 16, weight: .bold, design: .rounded))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(Color.purple)
                        .cornerRadius(16)
                }
                
                if !premiumManager.isPremium {
                    Button(action: {
                        showPaywall = true
                    }) {
                        HStack(spacing: 6) {
                            Image(systemName: "crown.fill")
                                .foregroundColor(.orange)
                            Text("升級 Premium 解鎖全單字庫與無限每日練習")
                                .font(.system(size: 14, weight: .bold, design: .rounded))
                                .foregroundColor(.purple)
                        }
                        .padding(.vertical, 6)
                    }
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 20)
        }
    }
    
    private func loadErrorWords() {
        errorWords = DatabaseManager.shared.getReviewList()
    }
    
    private func startReviewQuiz() {
        if !premiumManager.isPremium && practiceManager.isDailyLimitReached {
            showPaywall = true
            return
        }
        if premiumManager.isPremium && practiceManager.isDailyLimitReached {
            showTargetReachedAlert = true
            return
        }
        if errorWords.isEmpty { return }
        
        let count = quizQuestionCount
        guard count > 0 else {
            if premiumManager.isPremium {
                showTargetReachedAlert = true
            } else {
                showPaywall = true
            }
            return
        }
        
        let selectWords = errorWords.shuffled().prefix(count)
        var tempQuestions: [QuizQuestion] = []
        
        for word in selectWords {
            let correctOption = word.translation
            var distractors = DatabaseManager.shared.getRandomDistractors(excludeWordId: word.id, count: 3)
            
            while distractors.count < 3 {
                distractors.append("備選答案 \(distractors.count + 1)")
            }
            
            var options = distractors
            options.append(correctOption)
            options.shuffle()
            
            tempQuestions.append(QuizQuestion(word: word, options: options, correctOption: correctOption))
        }
        
        self.quizQuestions = tempQuestions
        self.quizIndex = 0
        self.selectedOption = nil
        self.isAnswered = false
        self.correctCount = 0
        self.quizResults = []
        self.showQuizResults = false
        
        withAnimation {
            showQuizMode = true
        }
    }
    
    @ViewBuilder
    private func cardReviewSheet(word: Word) -> some View {
        NavigationView {
            ZStack {
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()
                
                VStack(spacing: 24) {
                    Spacer()
                    WordCardView(word: word)
                    Spacer()
                    
                    Button(action: { selectedWord = nil }) {
                        Text("完成")
                            .font(.system(size: 16, weight: .bold, design: .rounded))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(Color.purple)
                            .cornerRadius(16)
                    }
                    .padding(.horizontal, 24)
                    .padding(.bottom, 20)
                }
            }
            .navigationTitle("單字詳情")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("關閉") { selectedWord = nil }
                        .foregroundColor(.purple)
                }
            }
        }
    }
}
