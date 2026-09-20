import SwiftUI

public struct PlacementTestView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var preferences = PersonalizedOnboardingPreferences.shared
    
    @State private var currentIndex = 0
    @State private var selectedOption: String? = nil
    @State private var correctCount = 0
    @State private var isTestFinished = false
    
    public var onFinish: () -> Void
    
    public static var testQuestions: [PlacementQuestion] {
        return PlacementQuestion.testQuestions
    }
    
    public init(onFinish: @escaping () -> Void) {
        self.onFinish = onFinish
    }
    
    public var body: some View {
        NavigationView {
            ZStack {
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()
                
                if !isTestFinished {
                    quizScreen
                } else {
                    resultScreen
                }
            }
            .navigationTitle(isTestFinished ? "評估完成" : "程度評估測驗")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
    
    private var quizScreen: some View {
        VStack(spacing: 24) {
            // Progress
            VStack(spacing: 8) {
                HStack {
                    Text("第 \(currentIndex + 1) / \(Self.testQuestions.count) 題")
                        .font(.system(size: 14, weight: .bold, design: .rounded))
                        .foregroundColor(.purple)
                    Spacer()
                    Text("完成度 \(Int((Double(currentIndex) / Double(Self.testQuestions.count)) * 100))%")
                        .font(.system(size: 14, weight: .semibold, design: .rounded))
                        .foregroundColor(.secondary)
                }
                
                ProgressView(value: Double(currentIndex + 1), total: Double(Self.testQuestions.count))
                    .accentColor(.purple)
            }
            .padding(.horizontal, 24)
            .padding(.top, 12)
            
            // Question Card
            let currentQ = Self.testQuestions[currentIndex]
            VStack(spacing: 12) {
                Text(currentQ.word)
                    .font(.system(size: 34, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)
                
                Text(currentQ.phonetic)
                    .font(.system(size: 16, weight: .medium, design: .serif))
                    .foregroundColor(.purple)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 4)
                    .background(Color.purple.opacity(0.1))
                    .cornerRadius(12)
                
                Button(action: {
                    SoundPlayer.shared.speak(currentQ.word)
                }) {
                    Image(systemName: "speaker.wave.2.fill")
                        .foregroundColor(.purple)
                        .font(.system(size: 18))
                        .padding(8)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 28)
            .background(Color(.secondarySystemGroupedBackground))
            .cornerRadius(20)
            .shadow(color: Color.black.opacity(0.04), radius: 8, x: 0, y: 2)
            .padding(.horizontal, 24)
            
            // Options
            VStack(spacing: 12) {
                ForEach(currentQ.options, id: \.self) { option in
                    Button(action: {
                        handleOptionTap(option, correct: currentQ.correctAnswer)
                    }) {
                        HStack {
                            Text(option)
                                .font(.system(size: 17, weight: .medium, design: .rounded))
                                .foregroundColor(optionTextColor(option, correct: currentQ.correctAnswer))
                            Spacer()
                            if selectedOption == option {
                                Image(systemName: option == currentQ.correctAnswer ? "checkmark.circle.fill" : "xmark.circle.fill")
                                    .foregroundColor(option == currentQ.correctAnswer ? .green : .red)
                            }
                        }
                        .padding(.horizontal, 20)
                        .padding(.vertical, 16)
                        .background(optionBackground(option, correct: currentQ.correctAnswer))
                        .cornerRadius(16)
                    }
                    .disabled(selectedOption != nil)
                }
            }
            .padding(.horizontal, 24)
            
            Spacer()
        }
    }
    
    private var resultScreen: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Header
                VStack(spacing: 8) {
                    Image(systemName: "checkmark.seal.fill")
                        .font(.system(size: 60))
                        .foregroundColor(.purple)
                        .padding(.top, 20)
                    
                    Text("程度評估已完成！")
                        .font(.system(size: 24, weight: .bold, design: .rounded))
                    
                    Text("已為您量身分析詞彙程度與目標進度")
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)
                }
                
                // Score & Level Card
                VStack(spacing: 16) {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("評估測驗成績")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(.secondary)
                            Text("\(correctCount) / \(Self.testQuestions.count) 題")
                                .font(.system(size: 26, weight: .bold, design: .rounded))
                                .foregroundColor(.purple)
                        }
                        Spacer()
                        VStack(alignment: .trailing, spacing: 4) {
                            Text("建議目標路徑")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(.secondary)
                            Text(preferences.localizedLevelName(preferences.recommendedLevel))
                                .font(.system(size: 18, weight: .bold, design: .rounded))
                                .foregroundColor(.blue)
                        }
                    }
                    
                    Divider()
                    
                    // User's Target and Scenarios
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            Text("🎯 您的學習目標：")
                                .font(.system(size: 14, weight: .semibold))
                            Text(preferences.targetGoalTitle(for: preferences.targetScore))
                                .font(.system(size: 15, weight: .bold, design: .rounded))
                                .foregroundColor(.purple)
                        }
                        
                        Text("💼 優先學習情境：")
                            .font(.system(size: 14, weight: .semibold))
                        
                        // Scenarios chips
                        FlowLayout(spacing: 8) {
                            ForEach(preferences.selectedScenarioIds, id: \.self) { sId in
                                if let sc = PersonalizedOnboardingPreferences.availableScenarios.first(where: { $0.id == sId }) {
                                    Text(sc.title)
                                        .font(.system(size: 13, weight: .medium))
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 6)
                                        .background(Color.purple.opacity(0.1))
                                        .foregroundColor(.purple)
                                        .cornerRadius(12)
                                }
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(20)
                .background(Color(.secondarySystemGroupedBackground))
                .cornerRadius(20)
                .padding(.horizontal, 24)
                
                // CTA: Start Learning Today
                Button(action: {
                    preferences.completeOnboarding(score: correctCount, recommendedLevel: preferences.recommendedLevel)
                    onFinish()
                    dismiss()
                }) {
                    HStack(spacing: 8) {
                        Text("開始今天的學習")
                        Image(systemName: "arrow.right")
                    }
                    .font(.system(size: 17, weight: .bold, design: .rounded))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 18)
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
                .padding(.top, 10)
                
                Spacer(minLength: 30)
            }
        }
    }
    
    private func handleOptionTap(_ option: String, correct: String) {
        selectedOption = option
        if option == correct {
            correctCount += 1
        }
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
            if currentIndex < Self.testQuestions.count - 1 {
                currentIndex += 1
                selectedOption = nil
            } else {
                let recLevel = preferences.calculateRecommendedLevel(correctCount: correctCount)
                preferences.recommendedLevel = recLevel
                preferences.placementScore = correctCount
                isTestFinished = true
            }
        }
    }
    
    private func optionTextColor(_ option: String, correct: String) -> Color {
        guard let selected = selectedOption else { return .primary }
        if option == correct { return .green }
        if option == selected { return .red }
        return .secondary
    }
    
    private func optionBackground(_ option: String, correct: String) -> Color {
        guard let selected = selectedOption else { return Color(.secondarySystemGroupedBackground) }
        if option == correct { return Color.green.opacity(0.12) }
        if option == selected { return Color.red.opacity(0.12) }
        return Color(.secondarySystemGroupedBackground)
    }
}

// Simple flowing chip layout
struct FlowLayout: Layout {
    var spacing: CGFloat = 8
    
    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? 300
        var height: CGFloat = 0
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        
        for view in subviews {
            let size = view.sizeThatFits(.unspecified)
            if x + size.width > width {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
        height = y + rowHeight
        return CGSize(width: width, height: height)
    }
    
    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x: CGFloat = bounds.minX
        var y: CGFloat = bounds.minY
        var rowHeight: CGFloat = 0
        
        for view in subviews {
            let size = view.sizeThatFits(.unspecified)
            if x + size.width > bounds.maxX {
                x = bounds.minX
                y += rowHeight + spacing
                rowHeight = 0
            }
            view.place(at: CGPoint(x: x, y: y), proposal: .unspecified)
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}
