import SwiftUI

public struct DashboardView: View {
    @State private var stats = DatabaseManager.shared.getStatistics()
    @State private var todayWords: [Word] = []
    @State private var userLevel = UserDefaults.standard.string(forKey: "user_level") ?? Word.kLevelBasic
    
    public enum DashboardSheet: Identifiable {
        case learning
        case quiz
        case review
        case paywall
        
        public var id: String {
            switch self {
            case .learning: return "learning"
            case .quiz: return "quiz"
            case .review: return "review"
            case .paywall: return "paywall"
            }
        }
    }
    
    @State private var activeSheet: DashboardSheet? = nil
    @State private var reviewCount: Int = DatabaseManager.shared.getReviewList().count
    @State private var showTargetReachedAlert = false
    @State private var showTargetPickerSheet = false
    @State private var hasAcknowledgedTargetToday = false
    
    @StateObject private var premiumManager = PremiumManager.shared
    @StateObject private var practiceManager = DailyPracticeManager.shared
    
    public init() {}
    
    private var todayProgress: Double {
        if premiumManager.isPremium {
            if practiceManager.premiumDailyTarget == -1 {
                return practiceManager.todayCompletedCount > 0 ? 1.0 : 0.0
            } else {
                return min(1.0, Double(practiceManager.todayCompletedCount) / Double(max(1, practiceManager.premiumDailyTarget)))
            }
        } else {
            return min(1.0, Double(practiceManager.todayCompletedCount) / Double(DailyPracticeManager.maxFreeDailyQuestions))
        }
    }
    
    public var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Welcoming Header
                headerSection
                
                // Target Level Selector
                Picker("目標路徑", selection: levelBinding) {
                    Text("550+ 基礎").tag(Word.kLevelBasic)
                    Text("750+ 進階").tag(Word.kLevelAdvanced)
                    Text("860+ 金證").tag(Word.kLevelGold)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 24)
                
                // Today's Goal Card
                dailyGoalCard
                
                // Stat Blocks Grid
                statsGrid
                
                // Action buttons
                actionSection
            }
            .padding(.bottom, 30)
        }
        .background(Color(.systemGroupedBackground))
        .onAppear {
            refreshData()
        }
        .task {
            await premiumManager.refreshEntitlements()
            refreshData()
        }
        .onChange(of: premiumManager.isPremium) { _ in
            refreshData()
        }
        .sheet(item: $activeSheet) { sheet in
            switch sheet {
            case .learning:
                DailyLearningView(
                    onComplete: {
                        refreshData()
                    },
                    onStartQuiz: {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                            activeSheet = .quiz
                        }
                    }
                )
                .onDisappear {
                    refreshData()
                }
            case .quiz:
                QuizView(onComplete: {
                    refreshData()
                }, onNavigateToReview: {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                        activeSheet = .review
                    }
                })
                .onDisappear {
                    refreshData()
                }
            case .review:
                ReviewView()
                    .onDisappear {
                        refreshData()
                    }
            case .paywall:
                PremiumView()
            }
        }
        .alert("今日學習已完成", isPresented: $showTargetReachedAlert) {
            Button("確定", role: .cancel) {}
            Button("⚙️ 調整每日學習量") {
                showTargetPickerSheet = true
            }
        } message: {
            Text("您已完成今日設定的 \(practiceManager.premiumDailyTarget == -1 ? "練習" : "\(practiceManager.premiumDailyTarget) 題") 目標！如需繼續學習，可點擊「調整每日學習量」。")
        }
        .confirmationDialog("⚙️ 調整每日學習量", isPresented: $showTargetPickerSheet, titleVisibility: .visible) {
            ForEach([5, 10, 20, 30, 50, 100, -1], id: \.self) { target in
                Button(target == -1 ? "不限 (Unlimited)" : "\(target) 題\(target == 10 ? " (預設)" : "")") {
                    practiceManager.setPremiumDailyTarget(target)
                    refreshData()
                }
            }
            Button("取消", role: .cancel) {}
        } message: {
            Text("選擇適合你的每日練習節奏，隨時可進行調整。")
        }
    }
    
    private var headerSection: some View {
        HStack {
            VStack(alignment: .leading, spacing: 6) {
                Text(getGreeting())
                    .font(.system(size: 14, weight: .bold, design: .rounded))
                    .foregroundColor(.purple)
                    .textCase(.uppercase)
                
                Text("English Coach")
                    .font(.system(size: 28, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)
            }
            Spacer()
            
            if premiumManager.isPremium {
                Button(action: { activeSheet = .paywall }) {
                    HStack(spacing: 4) {
                        Image(systemName: "crown.fill")
                            .font(.system(size: 12))
                            .foregroundColor(.orange)
                        Text("Premium")
                            .font(.system(size: 13, weight: .bold, design: .rounded))
                            .foregroundColor(.purple)
                        Image(systemName: "chevron.right")
                            .font(.system(size: 10, weight: .semibold))
                            .foregroundColor(.purple.opacity(0.6))
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Color.purple.opacity(0.1))
                    .clipShape(Capsule())
                }
                .buttonStyle(PlainButtonStyle())
            } else {
                Button(action: { activeSheet = .paywall }) {
                    HStack(spacing: 4) {
                        Image(systemName: "crown.fill")
                            .font(.system(size: 12))
                        Text("升級")
                            .font(.system(size: 13, weight: .bold, design: .rounded))
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(
                        LinearGradient(colors: [.purple, .orange], startPoint: .leading, endPoint: .trailing)
                    )
                    .clipShape(Capsule())
                    .shadow(color: Color.purple.opacity(0.3), radius: 4, x: 0, y: 2)
                }
            }
        }
        .padding(.horizontal, 24)
        .padding(.top, 20)
    }
    
    private var isLearningDoneAwaitingQuiz: Bool {
        DatabaseManager.shared.isTodayLearningCompleted() && !DatabaseManager.shared.isTodayQuizCompleted()
    }
    
    private var dailyGoalButtonTitle: String {
        if isLearningDoneAwaitingQuiz {
            return "開始今日測驗 ➜"
        }
        if practiceManager.isDailyLimitReached {
            if !premiumManager.isPremium {
                return "解鎖 Premium，今天繼續練習 ➜"
            } else {
                return "🎉 今日學習已完成"
            }
        }
        return todayProgress > 0 ? "繼續今日學習 ➜" : "開始今日練習"
    }
    
    private func handleDailyGoalButtonAction() {
        if isLearningDoneAwaitingQuiz {
            activeSheet = .quiz
            return
        }
        if practiceManager.isDailyLimitReached {
            if !premiumManager.isPremium {
                activeSheet = .paywall
            } else {
                showTargetReachedAlert = true
            }
        } else {
            activeSheet = .learning
        }
    }
    
    private var dailyGoalCard: some View {
        VStack(spacing: 20) {
            HStack(spacing: 24) {
                ProgressRing(
                    progress: todayProgress,
                    size: 120,
                    strokeWidth: 12,
                    centerText: (premiumManager.isPremium && practiceManager.premiumDailyTarget == -1) ? "∞" : nil
                )
                
                VStack(alignment: .leading, spacing: 10) {
                    Text("每日練習")
                        .font(.system(size: 18, weight: .bold, design: .rounded))
                        .foregroundColor(.primary)
                    
                    if !premiumManager.isPremium {
                        Text("今日測驗：\(practiceManager.todayCompletedCount) / \(DailyPracticeManager.maxFreeDailyQuestions) 題")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(.purple)
                        
                        Text("今日免費額度：\(practiceManager.todayCompletedCount) / \(DailyPracticeManager.maxFreeDailyQuestions) 題")
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(practiceManager.isDailyLimitReached ? .orange : .secondary)
                    } else {
                        if practiceManager.premiumDailyTarget == -1 {
                            Text("今日測驗：\(practiceManager.todayCompletedCount) 題")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(.purple)
                            
                            Button(action: { showTargetPickerSheet = true }) {
                                HStack(spacing: 4) {
                                    Text("每日目標：不限")
                                        .font(.system(size: 13, weight: .medium))
                                    Image(systemName: "slider.horizontal.3")
                                        .font(.system(size: 11))
                                }
                                .foregroundColor(.secondary)
                            }
                        } else {
                            Text("今日測驗：\(practiceManager.displayedCompletedCount) / \(practiceManager.premiumDailyTarget) 題")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(.purple)
                            
                            Button(action: { showTargetPickerSheet = true }) {
                                HStack(spacing: 4) {
                                    Image(systemName: "slider.horizontal.3")
                                        .font(.system(size: 11, weight: .bold))
                                    Text("⚙️ 調整每日學習量：\(practiceManager.premiumDailyTarget) 題")
                                        .font(.system(size: 12, weight: .bold, design: .rounded))
                                    Image(systemName: "chevron.right")
                                        .font(.system(size: 9, weight: .bold))
                                }
                                .foregroundColor(.purple)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color.purple.opacity(0.1))
                                .cornerRadius(8)
                            }
                        }
                    }
                }
                
                Spacer()
            }
            
            Button(action: {
                handleDailyGoalButtonAction()
            }) {
                Text(dailyGoalButtonTitle)
                    .font(.system(size: 16, weight: .bold, design: .rounded))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(
                        LinearGradient(
                            colors: [.purple, .blue],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .cornerRadius(14)
                    .shadow(color: Color.purple.opacity(0.3), radius: 6, x: 0, y: 3)
            }
        }
        .padding(20)
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(24)
        .shadow(color: Color.black.opacity(0.04), radius: 10, x: 0, y: 4)
        .padding(.horizontal, 24)
    }
    
    private var statsGrid: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("統計")
                .font(.system(size: 18, weight: .bold, design: .rounded))
                .foregroundColor(.primary)
                .padding(.horizontal, 24)
            
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
                // Stat 1: Total Vocabulary Learned
                statBox(
                    title: "已學習",
                    value: "\(stats.learnedWords)",
                    subTitle: "共 \(stats.totalWords) 個單字",
                    iconName: "book.closed.fill",
                    color: .blue
                )
                
                // Stat 2: Accuracy
                statBox(
                    title: "測驗正確率",
                    value: String(format: "%.1f%%", stats.accuracy),
                    subTitle: "總體正確率",
                    iconName: "percent",
                    color: .green
                )
            }
            .padding(.horizontal, 24)
        }
    }
    
    @ViewBuilder
    private func statBox(title: String, value: String, subTitle: String, iconName: String, color: Color, actionHint: String? = nil) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                ZStack {
                    Circle()
                        .fill(color.opacity(0.1))
                        .frame(width: 32, height: 32)
                    Image(systemName: iconName)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(color)
                }
                Spacer()
                if let actionHint = actionHint {
                    HStack(spacing: 2) {
                        Text(actionHint)
                            .font(.system(size: 11, weight: .bold, design: .rounded))
                        Image(systemName: "chevron.right")
                            .font(.system(size: 10, weight: .bold))
                    }
                    .foregroundColor(color)
                }
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text(value)
                    .font(.system(size: 24, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)
                
                Text(title)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(.primary)
                
                Text(subTitle)
                    .font(.system(size: 11))
                    .foregroundColor(.secondary)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(18)
        .shadow(color: Color.black.opacity(0.02), radius: 6, x: 0, y: 3)
    }
    
    private var actionSection: some View {
        VStack(spacing: 16) {
            if !premiumManager.isPremium {
                Button(action: { activeSheet = .paywall }) {
                    HStack(spacing: 16) {
                        ZStack {
                            Circle()
                                .fill(Color.orange.opacity(0.15))
                                .frame(width: 44, height: 44)
                            Image(systemName: "crown.fill")
                                .font(.system(size: 18))
                                .foregroundColor(.orange)
                        }
                        
                        VStack(alignment: .leading, spacing: 4) {
                            Text("解鎖 Premium 尊榮會員")
                                .font(.system(size: 16, weight: .bold, design: .rounded))
                                .foregroundColor(.primary)
                            Text("解鎖全 3,070 單字庫、每日無限練習與智慧複習")
                                .font(.system(size: 12))
                                .foregroundColor(.secondary)
                        }
                        
                        Spacer()
                        
                        Image(systemName: "chevron.right")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(.secondary)
                    }
                    .padding(16)
                    .background(
                        LinearGradient(
                            colors: [Color.purple.opacity(0.08), Color.blue.opacity(0.05)],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .cornerRadius(18)
                    .overlay(
                        RoundedRectangle(cornerRadius: 18)
                            .stroke(Color.purple.opacity(0.2), lineWidth: 1)
                    )
                }
                .buttonStyle(PlainButtonStyle())
            }
            
            // Smart Review Center Action
            Button(action: {
                activeSheet = .review
            }) {
                HStack(spacing: 16) {
                    ZStack {
                        Circle()
                            .fill(reviewCount > 0 ? Color.orange.opacity(0.1) : Color.green.opacity(0.1))
                            .frame(width: 44, height: 44)
                        Image(systemName: "calendar.badge.clock")
                            .font(.system(size: 18))
                            .foregroundColor(reviewCount > 0 ? .orange : .green)
                    }
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text("智慧複習中心")
                            .font(.system(size: 16, weight: .bold, design: .rounded))
                            .foregroundColor(.primary)
                        Text(reviewCount > 0 ? "\(reviewCount) 個單字待加強與複習" : "目前沒有待複習單字")
                            .font(.system(size: 12))
                            .foregroundColor(.secondary)
                    }
                    
                    Spacer()
                    
                    if reviewCount > 0 {
                        Text("\(reviewCount)")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.orange)
                            .clipShape(Capsule())
                    }
                    
                    Image(systemName: "chevron.right")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.secondary)
                }
                .padding(16)
                .background(Color(.secondarySystemGroupedBackground))
                .cornerRadius(18)
                .contentShape(Rectangle())
            }
            .buttonStyle(PlainButtonStyle())
        }
        .padding(.horizontal, 24)
    }
    
    private var levelBinding: Binding<String> {
        Binding<String>(
            get: {
                switch userLevel {
                case "Beginner": return Word.kLevelBasic
                case "Intermediate": return Word.kLevelAdvanced
                case "Advanced": return Word.kLevelGold
                default: return userLevel
                }
            },
            set: { newLevel in
                let target = (newLevel == "Intermediate" ? Word.kLevelAdvanced : (newLevel == "Advanced" ? Word.kLevelGold : newLevel))
                if !premiumManager.isPremium && (target == Word.kLevelAdvanced || target == Word.kLevelGold) {
                    activeSheet = .paywall
                } else {
                    userLevel = target
                    UserDefaults.standard.set(target, forKey: "user_level")
                    refreshData()
                }
            }
        )
    }
    
    private func refreshData() {
        DailyPracticeManager.shared.checkAndResetDailyIfNeeded()
        todayWords = DatabaseManager.shared.getTodayWords(isPremium: premiumManager.isPremium)
        stats = DatabaseManager.shared.getStatistics()
        reviewCount = DatabaseManager.shared.getReviewList().count
        if practiceManager.todayCompletedCount < practiceManager.premiumDailyTarget {
            hasAcknowledgedTargetToday = false
        }
    }
    
    private func getGreeting() -> String {
        let hour = Calendar.current.component(.hour, from: Date())
        if hour < 12 { return "早安" }
        if hour < 18 { return "午安" }
        return "晚安"
    }
}
