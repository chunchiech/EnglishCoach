import SwiftUI

public struct DailyLearningView: View {
    @Environment(\.dismiss) private var dismiss
    
    @State private var words: [Word] = []
    @State private var currentIndex = 0
    @State private var showCompletionView = false
    
    public var onComplete: () -> Void
    
    public init(onComplete: @escaping () -> Void) {
        self.onComplete = onComplete
    }
    
    public var body: some View {
        NavigationView {
            ZStack {
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()
                
                if words.isEmpty {
                    VStack {
                        ProgressView()
                            .scaleEffect(1.5)
                        Text("載入今日單字中...")
                            .font(.system(size: 16, weight: .medium, design: .rounded))
                            .foregroundColor(.secondary)
                            .padding(.top, 16)
                    }
                } else if showCompletionView {
                    completionScreen
                } else {
                    VStack(spacing: 20) {
                        // Progress Header
                        VStack(spacing: 8) {
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
                        WordCardView(word: words[currentIndex])
                            .id(words[currentIndex].id) // Re-instantiate card when index changes
                        
                        Spacer()
                        
                        // Control Buttons
                        HStack(spacing: 16) {
                            // Back Button
                            Button(action: {
                                if currentIndex > 0 {
                                    withAnimation(.easeInOut) {
                                        currentIndex -= 1
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
                            
                            // Next Button
                            Button(action: {
                                handleNext()
                            }) {
                                HStack {
                                    Text(currentIndex == words.count - 1 ? "完成" : "下一個")
                                    if currentIndex < words.count - 1 {
                                        Image(systemName: "chevron.right")
                                    } else {
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
                        dismiss()
                    }
                    .foregroundColor(.purple)
                }
            }
            .onAppear {
                loadWords()
            }
        }
    }
    
    private func loadWords() {
        words = DatabaseManager.shared.getTodayWords()
    }
    
    private func handleNext() {
        if currentIndex < words.count - 1 {
            withAnimation(.easeInOut) {
                currentIndex += 1
            }
        } else {
            // Completed learning session
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
                
                Text("你已完成今日 20 個每日單字學習。繼續保持！")
                    .font(.system(size: 16))
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
            }
            
            Spacer()
            
            Button(action: {
                onComplete()
                dismiss()
            }) {
                Text("返回首頁")
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
