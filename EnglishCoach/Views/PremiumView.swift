import SwiftUI
import StoreKit

public struct PremiumView: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var premiumManager = PremiumManager.shared
    @StateObject private var practiceManager = DailyPracticeManager.shared
    
    enum PlanSelection {
        case annual
        case monthly
        case lifetime
    }
    
    @State private var selectedPlan: PlanSelection = .annual
    @State private var showingAlert = false
    @State private var alertMessage = ""
    
    public init() {}
    
    private var currentPlan: PlanSelection? {
        guard premiumManager.isPremium, let id = premiumManager.activeProductID else {
            return nil
        }
        switch id {
        case PremiumManager.monthlySubscriptionID:
            return .monthly
        case PremiumManager.annualSubscriptionID:
            return .annual
        case PremiumManager.legacyLifetimeProductID:
            return .lifetime
        default:
            return nil
        }
    }
    
    public var body: some View {
        NavigationView {
            ZStack {
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 24) {
                        // Header Badge
                        headerBadge
                        
                        // Plan Comparison Card (Free vs Premium)
                        comparisonCard
                        
                        // Plan Options (Annual, Monthly, Lifetime)
                        planSelector
                        
                        // Action Buttons & Legal Info
                        actionSection
                    }
                    .padding(.vertical, 20)
                }
            }
            .navigationTitle(premiumManager.isPremium ? "管理會員方案" : "EnglishCoach Premium")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 22))
                            .foregroundColor(.secondary)
                    }
                }
            }
            .alert("提示", isPresented: $showingAlert) {
                Button("確定", role: .cancel) {
                    premiumManager.clearMessages()
                }
            } message: {
                Text(alertMessage)
            }
            .onChange(of: premiumManager.errorMessage) { msg in
                if let msg = msg, !msg.isEmpty {
                    alertMessage = msg
                    showingAlert = true
                }
            }
            .onChange(of: premiumManager.purchaseSuccessMessage) { msg in
                if let msg = msg, !msg.isEmpty {
                    alertMessage = msg
                    showingAlert = true
                }
            }
            .onChange(of: premiumManager.activeProductID) { _ in
                syncDefaultPlan()
            }
            .task {
                await premiumManager.loadProducts()
                await premiumManager.refreshEntitlements()
                syncDefaultPlan()
            }
        }
    }
    
    private func syncDefaultPlan() {
        if let current = currentPlan {
            selectedPlan = current
        } else {
            selectedPlan = .annual
        }
    }
    
    private var headerBadge: some View {
        VStack(spacing: 14) {
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [.purple.opacity(0.18), .orange.opacity(0.18)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 96, height: 96)
                
                Image(systemName: "crown.fill")
                    .font(.system(size: 48))
                    .foregroundStyle(
                        LinearGradient(
                            colors: [.purple, .orange],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .shadow(color: Color.purple.opacity(0.35), radius: 8, x: 0, y: 4)
            }
            
            VStack(spacing: 6) {
                if premiumManager.isPremium {
                    Text("管理與變更會員方案")
                        .font(.system(size: 24, weight: .bold, design: .rounded))
                        .foregroundColor(.primary)
                    
                    Text(headerSubtitle)
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                } else if practiceManager.isDailyLimitReached {
                    Text("今日 10 題免費練習已完成")
                        .font(.system(size: 24, weight: .bold, design: .rounded))
                        .foregroundColor(.primary)
                    
                    Text("升級 Premium，依你的需求自訂每日學習量")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                } else {
                    Text("升級 EnglishCoach Premium")
                        .font(.system(size: 24, weight: .bold, design: .rounded))
                        .foregroundColor(.primary)
                    
                    Text("升級 Premium，依你的需求自訂每日學習量")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                }
            }
        }
        .padding(.top, 4)
    }
    
    private var headerSubtitle: String {
        switch currentPlan {
        case .monthly:
            return "您目前使用月訂閱方案，可自由切換年約優惠或升級終身買斷"
        case .annual:
            return "您目前使用年訂閱方案，可隨時變更方案或升級終身買斷"
        case .lifetime:
            return "您已擁有終身尊榮會員，享有全部 3,070 單字與無限功能"
        default:
            return "檢視或管理您的會員方案權限"
        }
    }
    
    private var comparisonCard: some View {
        VStack(spacing: 12) {
            featureRow(
                icon: "slider.horizontal.3",
                color: .purple,
                title: "自訂每日學習題數",
                freeText: "固定 10 題",
                premiumText: "5 / 10 / 20 / 30 / 50 / 100 / 無限制"
            )
            
            featureRow(
                icon: "books.vertical.fill",
                color: .blue,
                title: "完整核心英文單字庫",
                freeText: "基礎體驗",
                premiumText: "完整 3,070 個核心單字"
            )
            
            featureRow(
                icon: "speaker.wave.3.fill",
                color: .green,
                title: "英文發音即時朗讀",
                freeText: "部分單字",
                premiumText: "無限制英文發音朗讀"
            )
            
            featureRow(
                icon: "brain.head.profile",
                color: .orange,
                title: "進階學習與間隔複習",
                freeText: "基礎模式",
                premiumText: "包含所有進階學習功能"
            )
        }
        .padding(.horizontal, 24)
    }
    
    @ViewBuilder
    private func featureRow(icon: String, color: Color, title: String, freeText: String, premiumText: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 22))
                .foregroundColor(color)
                .frame(width: 28)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 15, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)
                Text("免費：\(freeText)")
                    .font(.system(size: 12))
                    .foregroundColor(.secondary)
            }
            .layoutPriority(1)
            
            Spacer(minLength: 8)
            
            Text(premiumText)
                .font(.system(size: 11, weight: .bold, design: .rounded))
                .foregroundColor(.purple)
                .multilineTextAlignment(.trailing)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(Color.purple.opacity(0.1))
                .cornerRadius(8)
        }
        .padding(14)
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(16)
        .shadow(color: Color.black.opacity(0.02), radius: 4, x: 0, y: 2)
    }
    
    private var planSelector: some View {
        VStack(spacing: 12) {
            // Annual Plan (Recommended)
            let annualPriceText: String = {
                if let p = premiumManager.annualProduct {
                    return "\(p.displayPrice) / 年"
                } else {
                    return "取得價格中..."
                }
            }()
            let annualBadge: String? = {
                if currentPlan == .annual {
                    return "目前方案"
                } else if currentPlan == .lifetime {
                    return nil
                } else {
                    return "超值推薦"
                }
            }()
            let isAnnualCurrent = currentPlan == .annual
            planCard(
                plan: .annual,
                title: "年訂閱方案",
                badge: annualBadge,
                isCurrentPlan: isAnnualCurrent,
                price: annualPriceText,
                priceSubtitle: (currentPlan == .annual || currentPlan == .lifetime) ? nil : "7 天免費試用",
                subPrice: currentPlan == .lifetime ? "已享有終身權益，無需訂閱" : "7 天試用結束後自動續訂 NT$690/年，可隨時取消。",
                isSelected: selectedPlan == .annual
            )
            
            // Monthly Plan
            let monthlyPriceText: String = {
                if let p = premiumManager.monthlyProduct {
                    return "\(p.displayPrice) / 月"
                } else {
                    return "取得價格中..."
                }
            }()
            let monthlyBadge: String? = {
                if currentPlan == .monthly {
                    return "目前方案"
                } else {
                    return nil
                }
            }()
            let isMonthlyCurrent = currentPlan == .monthly
            planCard(
                plan: .monthly,
                title: "月訂閱方案",
                badge: monthlyBadge,
                isCurrentPlan: isMonthlyCurrent,
                price: monthlyPriceText,
                subPrice: currentPlan == .lifetime ? "已享有終身權益，無需訂閱" : "按月彈性續訂・可隨時取消",
                isSelected: selectedPlan == .monthly
            )
            
            // Lifetime Buyout Plan
            let lifetimePriceText: String = {
                if let p = premiumManager.legacyProduct {
                    return "\(p.displayPrice)（終身買斷）"
                } else {
                    return "取得價格中..."
                }
            }()
            let lifetimeBadge: String? = {
                if currentPlan == .lifetime {
                    return "目前方案"
                } else {
                    return "終身有效"
                }
            }()
            let isLifetimeCurrent = currentPlan == .lifetime
            planCard(
                plan: .lifetime,
                title: "終身買斷方案",
                badge: lifetimeBadge,
                isCurrentPlan: isLifetimeCurrent,
                price: lifetimePriceText,
                subPrice: "一次付費買斷・終身無限存取與更新",
                isSelected: selectedPlan == .lifetime
            )
        }
        .padding(.horizontal, 24)
    }
    
    @ViewBuilder
    private func planCard(
        plan: PlanSelection,
        title: String,
        badge: String?,
        isCurrentPlan: Bool,
        price: String,
        priceSubtitle: String? = nil,
        subPrice: String,
        isSelected: Bool
    ) -> some View {
        Button(action: {
            withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                selectedPlan = plan
            }
        }) {
            HStack(spacing: 14) {
                ZStack {
                    Circle()
                        .stroke(isSelected ? Color.purple : Color.secondary.opacity(0.4), lineWidth: 2)
                        .frame(width: 22, height: 22)
                    
                    if isSelected {
                        Circle()
                            .fill(Color.purple)
                            .frame(width: 14, height: 14)
                    }
                }
                
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 8) {
                        Text(title)
                            .font(.system(size: 16, weight: .bold, design: .rounded))
                            .foregroundColor(.primary)
                        
                        if let badge = badge {
                            Text(badge)
                                .font(.system(size: 10, weight: .bold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 2)
                                .background(
                                    Group {
                                        if isCurrentPlan {
                                            Color.green
                                        } else {
                                            LinearGradient(colors: [.orange, .red], startPoint: .leading, endPoint: .trailing)
                                        }
                                    }
                                )
                                .cornerRadius(6)
                        }
                    }
                    
                    Text(subPrice)
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                VStack(alignment: .trailing, spacing: 2) {
                    Text(price)
                        .font(.system(size: 15, weight: .bold, design: .rounded))
                        .foregroundColor(isSelected ? .purple : .primary)
                    
                    if let priceSubtitle = priceSubtitle {
                        Text(priceSubtitle)
                            .font(.system(size: 11, weight: .bold, design: .rounded))
                            .foregroundColor(.orange)
                    }
                }
                .fixedSize(horizontal: true, vertical: false)
            }
            .padding(16)
            .background(Color(.secondarySystemGroupedBackground))
            .cornerRadius(18)
            .overlay(
                RoundedRectangle(cornerRadius: 18)
                    .stroke(isSelected ? Color.purple : Color.clear, lineWidth: 2)
            )
            .shadow(color: isSelected ? Color.purple.opacity(0.15) : Color.black.opacity(0.02), radius: 8, x: 0, y: 3)
        }
        .buttonStyle(PlainButtonStyle())
    }
    
    private var selectedProduct: Product? {
        switch selectedPlan {
        case .annual:
            return premiumManager.annualProduct
        case .monthly:
            return premiumManager.monthlyProduct
        case .lifetime:
            return premiumManager.legacyProduct
        }
    }
    
    private var isActionButtonEnabled: Bool {
        guard selectedProduct != nil && !premiumManager.isLoading else {
            return false
        }
        
        guard premiumManager.isPremium else {
            return true
        }
        
        guard let current = currentPlan else {
            return true
        }
        
        // Cannot re-purchase current plan
        if selectedPlan == current {
            return false
        }
        
        // Lifetime users cannot purchase subscriptions
        if current == .lifetime {
            return false
        }
        
        return true
    }
    
    private var actionButtonTitle: String {
        if premiumManager.isLoading {
            return "處理中..."
        }
        guard selectedProduct != nil else {
            return "取得商品資訊中..."
        }
        
        guard premiumManager.isPremium else {
            if selectedPlan == .annual {
                return "開始 7 天免費試用 ➜"
            }
            return "立即解鎖 Premium 尊榮會員"
        }
        
        guard let current = currentPlan else {
            if selectedPlan == .annual {
                return "開始 7 天免費試用 ➜"
            }
            return "立即解鎖 Premium 尊榮會員"
        }
        
        if selectedPlan == current {
            return "目前生效方案"
        }
        
        switch (current, selectedPlan) {
        case (.monthly, .annual):
            return "變更為年訂閱"
        case (.monthly, .lifetime), (.annual, .lifetime):
            return "升級為終身買斷"
        case (.annual, .monthly):
            return "變更為月訂閱"
        case (.lifetime, _):
            return "您已擁有終身尊榮會員"
        default:
            return "變更方案"
        }
    }
    
    private var actionSection: some View {
        VStack(spacing: 16) {
            Button(action: {
                executeSelectedPurchase()
            }) {
                HStack(spacing: 8) {
                    if premiumManager.isLoading {
                        ProgressView()
                            .tint(.white)
                    } else {
                        Text(actionButtonTitle)
                            .font(.system(size: 17, weight: .bold, design: .rounded))
                    }
                }
                .foregroundColor(isActionButtonEnabled ? .white : .secondary)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .background(
                    Group {
                        if isActionButtonEnabled {
                            LinearGradient(
                                colors: [.purple, .blue],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        } else {
                            LinearGradient(
                                colors: [Color(.systemGray5), Color(.systemGray5)],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        }
                    }
                )
                .cornerRadius(18)
                .shadow(color: isActionButtonEnabled ? Color.purple.opacity(0.35) : Color.clear, radius: 10, x: 0, y: 5)
            }
            .disabled(!isActionButtonEnabled)
            
            Button(action: {
                Task {
                    await premiumManager.restore()
                }
            }) {
                HStack(spacing: 6) {
                    if premiumManager.isLoading {
                        ProgressView()
                            .scaleEffect(0.8)
                    }
                    Text("恢復購買 (Restore Purchases)")
                        .font(.system(size: 14, weight: .medium, design: .rounded))
                        .foregroundColor(.purple)
                }
                .padding(.vertical, 4)
            }
            .disabled(premiumManager.isLoading)
            
            // Legal & Terms
            HStack(spacing: 16) {
                Link("使用條款", destination: URL(string: "https://chunchiech.github.io/EnglishCoach/terms-of-service.html") ?? URL(string: "https://apple.com")!)
                    .font(.system(size: 12))
                    .foregroundColor(.secondary)
                
                Text("•")
                    .font(.system(size: 12))
                    .foregroundColor(.secondary)
                
                Link("隱私權政策", destination: URL(string: "https://chunchiech.github.io/EnglishCoach/privacy-policy.html") ?? URL(string: "https://apple.com")!)
                    .font(.system(size: 12))
                    .foregroundColor(.secondary)
            }
            .padding(.top, 2)
        }
        .padding(.horizontal, 24)
        .padding(.top, 4)
    }
    
    private func executeSelectedPurchase() {
        guard let product = selectedProduct else {
            Task {
                await premiumManager.loadProducts()
            }
            return
        }
        
        Task {
            let success = await premiumManager.purchase(product: product)
            if success {
                dismiss()
            }
        }
    }
}
