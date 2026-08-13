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
            .navigationTitle(showQuizMode ? "複習測驗" : "錯題庫")
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
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    if !showQuizMode && !errorWords.isEmpty {
                        Button(action: startReviewQuiz) {
                            HStack(spacing: 4) {
                                Image(systemName: "play.fill")
                                Text("測驗")
                            }
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(
                                LinearGradient(colors: [.purple, .blue], startPoint: .leading, endPoint: .trailing)
                            )
                            .cornerRadius(12)
                        }
                    }
                }
            }
            .sheet(item: $selectedWord) { word in
                cardReviewSheet(word: word)
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
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(word.word)
                                    .font(.system(size: 18, weight: .bold, design: .rounded))
                                    .foregroundColor(.primary)
                                
                                HStack(spacing: 8) {
                                    Text(word.phonetic)
                                        .font(.system(size: 13, weight: .medium, design: .serif))
                                        .foregroundColor(.purple)
                                    
                                    Text(word.translation)
                                        .font(.system(size: 14))
                                        .foregroundColor(.secondary)
                                }
                            }
                            
                            Spacer()
                            
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
                        }
                        .padding(.vertical, 4)
                    }
                }
            }
            .listStyle(InsetGroupedListStyle())
        }
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
                Text("全部完成！")
                    .font(.system(size: 22, weight: .bold, design: .rounded))
                Text("太棒了！錯題庫中沒有需要複習的單字。請繼續保持！")
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
    }
    
    private func handleNextReviewQuizQuestion() {
        if quizIndex < quizQuestions.count - 1 {
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
                
                Text("答對的單字已從錯題庫中移除或減少錯誤次數。")
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
                            
                            VStack(alignment: .leading) {
                                Text(result.word.word)
                                    .font(.system(size: 16, weight: .bold))
                                Text(result.word.translation)
                                    .font(.system(size: 13))
                                    .foregroundColor(.secondary)
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
            
            Button(action: {
                withAnimation {
                    showQuizMode = false
                    showQuizResults = false
                    loadErrorWords()
                }
            }) {
                Text("返回錯題庫")
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
    
    private func loadErrorWords() {
        errorWords = DatabaseManager.shared.getReviewList()
    }
    
    private func startReviewQuiz() {
        if errorWords.isEmpty { return }
        
        let selectWords = errorWords.shuffled().prefix(10)
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
