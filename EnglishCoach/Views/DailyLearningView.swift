import SwiftUI

public struct DailyLearningView: View {
    @Environment(\.dismiss) private var dismiss
    
    @State private var words: [Word] = []
    @State private var currentIndex = 0
    @State private var isCardFlipped = false
    @State private var showCompletionView = false
    @State private var showPaywall = false
    @State private var isLoading = true
    @AppStorage("hasSeenLearningGuide") private var hasSeenLearningGuide = false
    @State private var showLearningGuide = false
    
    @StateObject private var premiumManager = PremiumManager.shared
    @StateObject private var practiceManager = DailyPracticeManager.shared
    @ObservedObject private var onboardingPrefs = PersonalizedOnboardingPreferences.shared
    
    private var scenarioDisplayText: String? {
        onboardingPrefs.getScenarioDisplayText()
    }
    
    private var toeicTargetDisplayText: String {
        let rawLvl = UserDefaults.standard.string(forKey: "user_level") ?? onboardingPrefs.recommendedLevel
        let target = ToeicTarget.from(rawString: rawLvl)
        return "🎯 \(target.displayName)"
    }
    
    public var onComplete: () -> Void
    public var onStartQuiz: (() -> Void)?
    
    public init(onComplete: @escaping () -> Void, onStartQuiz: (() -> Void)? = nil) {
        self.onComplete = onComplete
        self.onStartQuiz = onStartQuiz
    }
    
    public var body: some View {
        NavigationView {
            ZStack {
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()
                
                if isLoading {
                    VStack {
                        ProgressView()
                            .scaleEffect(1.5)
                        Text("載入今日單字中...")
                            .font(.system(size: 16, weight: .medium, design: .rounded))
                            .foregroundColor(.secondary)
                            .padding(.top, 16)
                    }
                } else if words.isEmpty {
                    tierCompletedScreen
                } else if showCompletionView {
                    completionScreen
                } else {
                    VStack(spacing: 20) {
                        // Progress Header
                        VStack(spacing: 8) {
                            VStack(alignment: .leading, spacing: 4) {
                                if let scenarioText = scenarioDisplayText {
                                    HStack(spacing: 4) {
                                        Text(scenarioText)
                                            .font(.system(size: 12, weight: .medium, design: .rounded))
                                            .foregroundColor(.secondary)
                                            .lineLimit(1)
                                            .minimumScaleFactor(0.85)
                                        Spacer()
                                    }
                                }
                                
                                HStack(spacing: 4) {
                                    Text(toeicTargetDisplayText)
                                        .font(.system(size: 12, weight: .medium, design: .rounded))
                                        .foregroundColor(.secondary)
                                        .lineLimit(1)
                                        .minimumScaleFactor(0.85)
                                    Spacer()
                                }
                            }
                            .padding(.bottom, 2)
                            
                            HStack {
                                Text("卡片 \(currentIndex + 1) / \(words.count)")
                                    .font(.system(size: 14, weight: .bold, design: .rounded))
                                    .foregroundColor(.purple)
                                Spacer()
                                Text("已完成 \(Int((Double(currentIndex) / Double(words.count)) * 100))%")
                                    .font(.system(size: 14, weight: .semibold, design: .rounded))
                                    .foregroundColor(.secondary)
                            }
                            
                            // Custom Progress Bar
                            GeometryReader { geo in
                                ZStack(alignment: .leading) {
                                    Capsule()
                                        .fill(Color(.systemGray5))
                                        .frame(height: 6)
                                    
                                    Capsule()
                                        .fill(
                                            LinearGradient(
                                                colors: [.purple, .blue],
                                                startPoint: .leading,
                                                endPoint: .trailing
                                            )
                                        )
                                        .frame(width: geo.size.width * CGFloat(Double(currentIndex + 1) / Double(words.count)), height: 6)
                                }
                            }
                            .frame(height: 6)
                        }
                        .padding(.horizontal, 24)
                        .padding(.top, 10)
                        
                        Spacer()
                        
                        // Word Card
                        WordCardView(word: words[currentIndex], isFlipped: $isCardFlipped)
                            .id(words[currentIndex].id) // Re-instantiate card when index changes
                        
                        Spacer()
                        
                        // Control Buttons
                        HStack(spacing: 16) {
                            // Back Button
                            Button(action: {
                                if currentIndex > 0 {
                                    withAnimation(.easeInOut) {
                                        currentIndex -= 1
                                        isCardFlipped = false
                                    }
                                }
                            }) {
                                HStack {
                                    Image(systemName: "chevron.left")
                                    Text("上一個")
                                }
                                .font(.system(size: 16, weight: .semibold, design: .rounded))
                                .foregroundColor(currentIndex > 0 ? .purple : .secondary)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                                .background(
                                    RoundedRectangle(cornerRadius: 16)
                                        .fill(currentIndex > 0 ? Color.purple.opacity(0.1) : Color(.systemGray5))
                                )
                            }
                            .disabled(currentIndex == 0)
                            
                            // Primary Action: View Chinese Meaning first, then Next
                            if !isCardFlipped {
                                Button(action: {
                                    withAnimation(.spring(response: 0.5, dampingFraction: 0.75)) {
                                        isCardFlipped = true
                                    }
                                }) {
                                    HStack(spacing: 6) {
                                        Text("查看中文意思")
                                        Text("👆")
                                    }
                                    .font(.system(size: 16, weight: .bold, design: .rounded))
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 16)
                                    .background(
                                        LinearGradient(
                                            colors: [.purple, .indigo],
                                            startPoint: .leading,
                                            endPoint: .trailing
                                        )
                                    )
                                    .cornerRadius(16)
                                    .shadow(color: Color.purple.opacity(0.3), radius: 8, x: 0, y: 4)
                                }
                            } else {
                                Button(action: {
                                    handleNext()
                                    isCardFlipped = false
                                }) {
                                    HStack {
                                        Text(currentIndex == words.count - 1 ? "完成" : "下一題 →")
                                        if currentIndex == words.count - 1 {
                                            Image(systemName: "checkmark.circle.fill")
                                        }
                                    }
                                    .font(.system(size: 16, weight: .semibold, design: .rounded))
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
                            }
                        }
                        .padding(.horizontal, 24)
                        .padding(.bottom, 20)
                    }
                }
            }
            .navigationTitle("今日單字")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("離開") {
                        onComplete()
                        dismiss()
                    }
                    .foregroundColor(.purple)
                }
            }
            .onAppear {
                loadWords()
                if !hasSeenLearningGuide {
                    showLearningGuide = true
                }
            }
            .sheet(isPresented: $showLearningGuide, onDismiss: {
                hasSeenLearningGuide = true
            }) {
                learningGuideSheet
            }
            .sheet(isPresented: $showPaywall) {
                PremiumView()
            }
        }
    }
    
    private func loadWords() {
        isLoading = true
        practiceManager.checkAndResetDailyIfNeeded()
        words = DatabaseManager.shared.getTodayWords(isPremium: premiumManager.isPremium)
        currentIndex = 0
        isLoading = false
    }
    
    @ViewBuilder
    private var tierCompletedScreen: some View {
        VStack(spacing: 24) {
            Spacer()
            
            ZStack {
                Circle()
                    .fill(Color.orange.opacity(0.12))
                    .frame(width: 120, height: 120)
                
                Image(systemName: "trophy.fill")
                    .font(.system(size: 60))
                    .foregroundStyle(
                        LinearGradient(
                            colors: [.orange, .yellow],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
            }
            
            VStack(spacing: 12) {
                Text("🎉 本目標等級單字已全部學完！")
                    .font(.system(size: 24, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)
                    .multilineTextAlignment(.center)
                
                let currentTarget = ToeicTarget.from(rawString: UserDefaults.standard.string(forKey: "user_level") ?? onboardingPrefs.recommendedLevel)
                Text("恭喜您！\(currentTarget.displayName) 的單字已全部學習完畢。\n建議前往「智慧複習中心」鞏固記憶，或切換至其他目標等級繼續挑戰！")
                    .font(.system(size: 15, design: .rounded))
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .lineSpacing(6)
                    .padding(.horizontal, 32)
            }
            
            Spacer()
            
            VStack(spacing: 12) {
                if DatabaseManager.shared.getReviewList().count > 0 {
                    Button(action: {
                        dismiss()
                        onComplete()
                    }) {
                        Text("前往智慧複習中心 ➜")
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
                    }
                }
                
                Button(action: {
                    dismiss()
                }) {
                    Text("返回首頁")
                        .font(.system(size: 16, weight: .semibold, design: .rounded))
                        .foregroundColor(.purple)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(Color.purple.opacity(0.1))
                        .cornerRadius(16)
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 24)
        }
    }
    
    private func handleNext() {
        guard currentIndex < words.count else { return }
        
        if currentIndex < words.count - 1 {
            withAnimation(.easeInOut) {
                currentIndex += 1
            }
        } else {
            // Completed viewing all cards for today
            DatabaseManager.shared.markTodayLearningCompleted()
            withAnimation(.spring()) {
                showCompletionView = true
            }
        }
    }
    
    @ViewBuilder
    private var completionScreen: some View {
        VStack(spacing: 24) {
            Spacer()
            
            // Celebration Icon
            ZStack {
                Circle()
                    .fill(Color.purple.opacity(0.1))
                    .frame(width: 120, height: 120)
                
                Image(systemName: "crown.fill")
                    .font(.system(size: 60))
                    .foregroundStyle(
                        LinearGradient(
                            colors: [.purple, .orange],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
            }
            
            VStack(spacing: 12) {
                Text("太棒了！")
                    .font(.system(size: 28, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)
                
                Text("你已完成今日單字學習。立即進行單字測驗以正式驗收成果！")
                    .font(.system(size: 16))
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
            }
            
            Spacer()
            
            VStack(spacing: 12) {
                Button(action: {
                    dismiss()
                    onStartQuiz?()
                }) {
                    Text("開始今日測驗 ➜")
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
                        .font(.system(size: 14, weight: .semibold, design: .rounded))
                        .foregroundColor(.secondary)
                        .padding(.vertical, 6)
                }
                
                if !premiumManager.isPremium {
                    Button(action: {
                        showPaywall = true
                    }) {
                        HStack(spacing: 6) {
                            Image(systemName: "crown.fill")
                                .foregroundColor(.orange)
                            Text("升級 Premium 解鎖全 3,070 個單字與無限學習")
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
    
    @ViewBuilder
    private var learningGuideSheet: some View {
        VStack(spacing: 24) {
            VStack(spacing: 8) {
                Image(systemName: "lightbulb.fill")
                    .font(.system(size: 42))
                    .foregroundColor(.orange)
                    .padding(.top, 28)
                
                Text("EnglishCoach 學習指引")
                    .font(.system(size: 22, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)
                
                Text("4 步掌握高效率商務英文自律學習法")
                    .font(.system(size: 14))
                    .foregroundColor(.secondary)
            }
            
            VStack(alignment: .leading, spacing: 18) {
                guideItem(
                    icon: "calendar.badge.clock",
                    color: .blue,
                    title: "每日精選 10 個單字",
                    description: "小步高頻學習，打造無痛且可持續的英文進步節奏。"
                )
                
                guideItem(
                    icon: "hand.tap.fill",
                    color: .purple,
                    title: "點擊卡片查看中文與例句",
                    description: "輕觸單字卡正面即可翻面，查看繁體中文釋義、音標與商務例句。"
                )
                
                guideItem(
                    icon: "brain.head.profile",
                    color: .orange,
                    title: "測驗錯題進入智慧複習",
                    description: "測驗中答錯或需強化的單字，自動歸入智慧複習中心，依科學間隔複習。"
                )
                
                guideItem(
                    icon: "target",
                    color: .green,
                    title: "個人化情境優先推薦",
                    description: "系統會依據你選擇的職場情境（如職場商務、會議談判）提高抽題機率。"
                )
            }
            .padding(.horizontal, 24)
            
            Spacer()
            
            Button(action: {
                hasSeenLearningGuide = true
                showLearningGuide = false
            }) {
                Text("開始今天的學習 ➜")
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
            .padding(.bottom, 24)
        }
    }
    
    @ViewBuilder
    private func guideItem(icon: String, color: Color, title: String, description: String) -> some View {
        HStack(alignment: .top, spacing: 14) {
            ZStack {
                Circle()
                    .fill(color.opacity(0.12))
                    .frame(width: 36, height: 36)
                Image(systemName: icon)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(color)
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.system(size: 15, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)
                Text(description)
                    .font(.system(size: 13))
                    .foregroundColor(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}
