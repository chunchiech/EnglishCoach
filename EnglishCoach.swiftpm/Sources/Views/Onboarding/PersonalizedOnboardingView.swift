import SwiftUI

public struct PersonalizedOnboardingView: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var preferences = PersonalizedOnboardingPreferences.shared
    
    @State private var currentStep = 0 // 0: Scenarios, 1: Target Score, 2: Placement Entry
    @State private var showPlacementTest = false
    
    public var onComplete: () -> Void
    
    public init(onComplete: @escaping () -> Void = {}) {
        self.onComplete = onComplete
    }
    
    public var body: some View {
        NavigationView {
            ZStack {
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()
                
                VStack(spacing: 0) {
                    // Step Indicator
                    stepIndicator
                        .padding(.top, 10)
                        .padding(.bottom, 16)
                    
                    if currentStep == 0 {
                        scenarioStep
                    } else if currentStep == 1 {
                        targetScoreStep
                    } else {
                        placementEntryStep
                    }
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .fullScreenCover(isPresented: $showPlacementTest) {
                PlacementTestView {
                    onComplete()
                    dismiss()
                }
            }
        }
    }
    
    // Step Indicator
    private var stepIndicator: some View {
        HStack(spacing: 8) {
            ForEach(0..<3) { step in
                Capsule()
                    .fill(step <= currentStep ? Color.purple : Color(.systemGray4))
                    .frame(height: 5)
            }
        }
        .padding(.horizontal, 30)
    }
    
    // Step 1: Learning Scenarios
    private var scenarioStep: some View {
        VStack(spacing: 20) {
            VStack(spacing: 6) {
                Text("選擇您的學習情境")
                    .font(.system(size: 26, weight: .bold, design: .rounded))
                Text("可複選，已按您的選擇順序排列優先級")
                    .font(.system(size: 14))
                    .foregroundColor(.secondary)
            }
            .padding(.top, 10)
            
            ScrollView {
                VStack(spacing: 12) {
                    ForEach(PersonalizedOnboardingPreferences.availableScenarios) { scenario in
                        let isSelected = preferences.selectedScenarioIds.contains(scenario.id)
                        let priorityIndex = preferences.selectedScenarioIds.firstIndex(of: scenario.id)
                        
                        Button(action: {
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                                preferences.toggleScenario(scenario.id)
                            }
                        }) {
                            HStack(spacing: 16) {
                                VStack(alignment: .leading, spacing: 4) {
                                    HStack {
                                        Text(scenario.title)
                                            .font(.system(size: 17, weight: .bold, design: .rounded))
                                            .foregroundColor(.primary)
                                        
                                        if let idx = priorityIndex {
                                            Text("優先 \(idx + 1)")
                                                .font(.system(size: 11, weight: .bold))
                                                .padding(.horizontal, 8)
                                                .padding(.vertical, 2)
                                                .background(Color.purple.opacity(0.15))
                                                .foregroundColor(.purple)
                                                .cornerRadius(8)
                                        }
                                    }
                                    
                                    Text(scenario.subtitle)
                                        .font(.system(size: 13))
                                        .foregroundColor(.secondary)
                                }
                                
                                Spacer()
                                
                                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                                    .font(.system(size: 22))
                                    .foregroundColor(isSelected ? .purple : .secondary.opacity(0.5))
                            }
                            .padding(16)
                            .background(Color(.secondarySystemGroupedBackground))
                            .cornerRadius(16)
                            .overlay(
                                RoundedRectangle(cornerRadius: 16)
                                    .stroke(isSelected ? Color.purple : Color.clear, lineWidth: 2)
                            )
                        }
                    }
                }
                .padding(.horizontal, 24)
            }
            
            // Next Button
            Button(action: {
                withAnimation {
                    currentStep = 1
                }
            }) {
                Text("下一步：設定目標分數 ➜")
                    .font(.system(size: 17, weight: .bold, design: .rounded))
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
    
    // Step 2: Target Score Selection
    private var targetScoreStep: some View {
        VStack(spacing: 20) {
            VStack(spacing: 6) {
                Text("設定您的 TOEIC 目標")
                    .font(.system(size: 26, weight: .bold, design: .rounded))
                Text("為您客製化單字難度分配與每日練習規劃")
                    .font(.system(size: 14))
                    .foregroundColor(.secondary)
            }
            .padding(.top, 10)
            
            // Disclaimer Banner
            HStack(alignment: .top, spacing: 10) {
                Image(systemName: "info.circle.fill")
                    .foregroundColor(.blue)
                    .font(.system(size: 18))
                
                Text("此目標僅作為個人化學習進度規劃與題庫配置之參考，不宣稱亦不代表預測正式 TOEIC 測驗成績。")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(12)
            .background(Color.blue.opacity(0.08))
            .cornerRadius(12)
            .padding(.horizontal, 24)
            
            ScrollView {
                VStack(spacing: 10) {
                    ForEach(PersonalizedOnboardingPreferences.targetScoreOrder, id: \.self) { score in
                        let isSelected = preferences.targetScore == score
                        let desc = PersonalizedOnboardingPreferences.availableTargetScores[score] ?? ""
                        
                        Button(action: {
                            preferences.targetScore = score
                        }) {
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(score)
                                        .font(.system(size: 19, weight: .bold, design: .rounded))
                                        .foregroundColor(isSelected ? .purple : .primary)
                                    
                                    Text(desc)
                                        .font(.system(size: 13))
                                        .foregroundColor(.secondary)
                                }
                                Spacer()
                                if isSelected {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundColor(.purple)
                                        .font(.system(size: 20))
                                }
                            }
                            .padding(16)
                            .background(Color(.secondarySystemGroupedBackground))
                            .cornerRadius(16)
                            .overlay(
                                RoundedRectangle(cornerRadius: 16)
                                    .stroke(isSelected ? Color.purple : Color.clear, lineWidth: 2)
                            )
                        }
                    }
                }
                .padding(.horizontal, 24)
            }
            
            // Bottom Buttons
            HStack(spacing: 12) {
                Button(action: {
                    withAnimation { currentStep = 0 }
                }) {
                    Text("上一步")
                        .font(.system(size: 16, weight: .semibold, design: .rounded))
                        .foregroundColor(.secondary)
                        .padding(.vertical, 16)
                        .padding(.horizontal, 20)
                        .background(Color(.systemGray5))
                        .cornerRadius(16)
                }
                
                Button(action: {
                    withAnimation { currentStep = 2 }
                }) {
                    Text("下一步：程度評估 ➜")
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
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 20)
        }
    }
    
    // Step 3: Placement Test Entry
    private var placementEntryStep: some View {
        VStack(spacing: 24) {
            Spacer()
            
            VStack(spacing: 16) {
                Image(systemName: "graduationcap.fill")
                    .font(.system(size: 64))
                    .foregroundColor(.purple)
                
                Text("完成 20 題快速程度評估")
                    .font(.system(size: 24, weight: .bold, design: .rounded))
                    .multilineTextAlignment(.center)
                
                Text("只要約 2 分鐘，透過 20 題標準商務單字測驗，系統將自動分析您目前的詞彙實力，並為您推薦最適起點。")
                    .font(.system(size: 15))
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 20)
            }
            
            VStack(spacing: 12) {
                HStack(spacing: 12) {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(.green)
                    Text("測驗包含初級、中級、進階三階題型")
                        .font(.system(size: 14))
                }
                HStack(spacing: 12) {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(.green)
                    Text("測驗結束後立即提供難度起點建議")
                        .font(.system(size: 14))
                }
            }
            .padding(16)
            .background(Color(.secondarySystemGroupedBackground))
            .cornerRadius(16)
            .padding(.horizontal, 30)
            
            Spacer()
            
            VStack(spacing: 12) {
                Button(action: {
                    showPlacementTest = true
                }) {
                    Text("開始 20 題程度評估 ➜")
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
                
                Button(action: {
                    // Skip and complete with defaults
                    preferences.completeOnboarding(recommendedLevel: "Intermediate")
                    onComplete()
                    dismiss()
                }) {
                    Text("先使用預設建議直接開始")
                        .font(.system(size: 15, weight: .medium, design: .rounded))
                        .foregroundColor(.secondary)
                        .padding(.vertical, 8)
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 20)
        }
    }
}
