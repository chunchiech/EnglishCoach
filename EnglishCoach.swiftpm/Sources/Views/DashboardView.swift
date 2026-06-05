import SwiftUI

public struct DashboardView: View {
    @State private var stats = DatabaseManager.shared.getStatistics()
    @State private var todayWords: [Word] = []
    
    @State private var showLearningFlow = false
    @State private var showQuizFlow = false
    @State private var showReviewFlow = false
    
    public init() {}
    
    private var todayProgress: Double {
        if todayWords.isEmpty { return 0.0 }
        // Let's assume words are "learned" once the user goes through them
        // Let's count how many of today's 20 words have been attempted or played in history.
        // Wait, to keep it simple, we can store inUserDefaults or database state.
        // In the database, we fetch words where learned_date = today.
        // Let's see: we can track the learned words of today.
        // For a simple premium experience, let's track today's learning index using database status.
        // But wait! When today's words are fetched, they are marked as learned = 1 in the database.
        // Let's track how many have been mastered by checking their correctCount + wrongCount.
        // Or let's keep a local state of how many cards the user has completed.
        // Actually, we can count words where correct_count > 0 OR wrong_count > 0 among today's words.
        // Let's do that! That shows real progress.
        let attempted = todayWords.filter { $0.correctCount > 0 || $0.wrongCount > 0 }.count
        return Double(attempted) / Double(max(1, todayWords.count))
    }
    
    public var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Welcoming Header
                headerSection
                
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
        .sheet(isPresented: $showLearningFlow) {
            DailyLearningView(onComplete: {
                refreshData()
            })
        }
        .sheet(isPresented: $showQuizFlow) {
            QuizView(onComplete: {
                refreshData()
            })
        }
        .sheet(isPresented: $showReviewFlow) {
            ReviewView()
                .onDisappear {
                    refreshData()
                }
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
            
            // Custom Avatar or Icon
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [.purple, .blue],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 48, height: 48)
                
                Text("EC")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(.white)
            }
        }
        .padding(.horizontal, 24)
        .padding(.top, 20)
    }
    
    private var dailyGoalCard: some View {
        VStack(spacing: 20) {
            HStack(spacing: 24) {
                ProgressRing(progress: todayProgress, size: 120, strokeWidth: 12)
                
                VStack(alignment: .leading, spacing: 10) {
                    Text("Daily Vocabulary")
                        .font(.system(size: 18, weight: .bold, design: .rounded))
                        .foregroundColor(.primary)
                    
                    Text("Learn and review 20 daily words to build your core vocabulary.")
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                        .lineLimit(3)
                        .fixedSize(horizontal: false, vertical: true)
                    
                    Text("\(todayWords.filter { $0.correctCount > 0 || $0.wrongCount > 0 }.count) / 20 Words Practiced")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(.purple)
                }
            }
            
            Button(action: { showLearningFlow = true }) {
                Text(todayProgress >= 1.0 ? "Practice Again" : "Start Today's Words")
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
            Text("Statistics")
                .font(.system(size: 18, weight: .bold, design: .rounded))
                .foregroundColor(.primary)
                .padding(.horizontal, 24)
            
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
                // Stat 1: Total Vocabulary Learned
                statBox(
                    title: "Total Learned",
                    value: "\(stats.learnedWords)",
                    subTitle: "out of \(stats.totalWords) words",
                    iconName: "book.closed.fill",
                    color: .blue
                )
                
                // Stat 2: Accuracy
                statBox(
                    title: "Quiz Accuracy",
                    value: String(format: "%.1f%%", stats.accuracy),
                    subTitle: "overall success rate",
                    iconName: "percent",
                    color: .green
                )
                
                // Stat 3: Review Due
                statBox(
                    title: "Review Due",
                    value: "\(stats.reviewDueCount)",
                    subTitle: "due today (SM-2)",
                    iconName: "calendar.badge.clock",
                    color: stats.reviewDueCount > 0 ? .orange : .secondary
                )
                
                // Stat 4: All Mistakes
                statBox(
                    title: "All Mistakes",
                    value: "\(stats.errorWords)",
                    subTitle: "wrong answers list",
                    iconName: "exclamationmark.triangle.fill",
                    color: stats.errorWords > 0 ? .red : .secondary
                )
            }
            .padding(.horizontal, 24)
        }
    }
    
    @ViewBuilder
    private func statBox(title: String, value: String, subTitle: String, iconName: String, color: Color) -> some View {
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
            // Quiz Action
            Button(action: { showQuizFlow = true }) {
                HStack(spacing: 16) {
                    ZStack {
                        Circle()
                            .fill(Color.purple.opacity(0.1))
                            .frame(width: 44, height: 44)
                        Image(systemName: "pencil.and.outline")
                            .font(.system(size: 18))
                            .foregroundColor(.purple)
                    }
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Vocabulary Quiz")
                            .font(.system(size: 16, weight: .bold, design: .rounded))
                            .foregroundColor(.primary)
                        Text("Test your knowledge on today's words")
                            .font(.system(size: 12))
                            .foregroundColor(.secondary)
                    }
                    
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.secondary)
                }
                .padding(16)
                .background(Color(.secondarySystemGroupedBackground))
                .cornerRadius(18)
            }
            .buttonStyle(PlainButtonStyle())
            
            // Spaced Repetition & Error Review Action
            Button(action: { showReviewFlow = true }) {
                HStack(spacing: 16) {
                    ZStack {
                        Circle()
                            .fill(Color.orange.opacity(0.1))
                            .frame(width: 44, height: 44)
                        Image(systemName: "calendar.badge.clock")
                            .font(.system(size: 18))
                            .foregroundColor(.orange)
                    }
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Smart Review Center")
                            .font(.system(size: 16, weight: .bold, design: .rounded))
                            .foregroundColor(.primary)
                        Text("Review scheduled words and correct mistakes")
                            .font(.system(size: 12))
                            .foregroundColor(.secondary)
                    }
                    
                    Spacer()
                    
                    let totalReview = stats.reviewDueCount + stats.errorWords
                    if totalReview > 0 {
                        Text("\(totalReview)")
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
            }
            .buttonStyle(PlainButtonStyle())
        }
        .padding(.horizontal, 24)
    }
    
    private func refreshData() {
        stats = DatabaseManager.shared.getStatistics()
        todayWords = DatabaseManager.shared.getTodayWords()
    }
    
    private func getGreeting() -> String {
        let hour = Calendar.current.component(.hour, from: Date())
        if hour < 12 { return "Good Morning" }
        if hour < 18 { return "Good Afternoon" }
        return "Good Evening"
    }
}
