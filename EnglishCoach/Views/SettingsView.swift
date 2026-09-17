import SwiftUI
import UserNotifications
import StoreKit
import UIKit

@MainActor
public struct SettingsView: View {
    @StateObject private var notificationManager = NotificationManager.shared
    @StateObject private var premiumManager = PremiumManager.shared
    @StateObject private var practiceManager = DailyPracticeManager.shared
    
    @State private var showPaywall = false
    @State private var showingAlert = false
    @State private var alertMessage = ""
    @State private var showingEditProfileSheet = false
    
    @AppStorage("user_display_name") private var userDisplayName: String = "英語學習者"
    @AppStorage("user_avatar_emoji") private var userAvatarEmoji: String = "🐶"
    @AppStorage("haptic_feedback_enabled") private var hapticFeedbackEnabled: Bool = true
    
    public init() {}
    
    public var body: some View {
        NavigationView {
            Form {
                // Section 1: 一、會員
                membershipSection
                
                // Section 2: 二、學習
                learningSection
                
                // Section 3: 三、個人化
                personalizationSection
                
                // Section 4: 四、互動與支持
                interactionSection
                
                // Section 5: 五、社群
                socialSection
                
                // Section 6: 六、關於
                aboutSection
            }
            .navigationTitle("設定")
            .sheet(isPresented: $showPaywall) {
                PremiumView()
            }
            .sheet(isPresented: $showingEditProfileSheet) {
                ProfileEditSheet(displayName: $userDisplayName, avatarEmoji: $userAvatarEmoji)
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
            .task {
                await notificationManager.checkAuthorizationStatus()
                await premiumManager.refreshEntitlements()
            }
            .onAppear {
                Task {
                    await premiumManager.refreshEntitlements()
                }
            }
        }
    }
    
    // MARK: - Section 1: 一、會員
    private var membershipSection: some View {
        Section(header: Text("會員")) {
            HStack(spacing: 16) {
                ZStack {
                    Circle()
                        .fill(premiumManager.isPremium ? Color.orange.opacity(0.15) : Color.purple.opacity(0.1))
                        .frame(width: 44, height: 44)
                    Image(systemName: premiumManager.isPremium ? "crown.fill" : "person.fill")
                        .font(.system(size: 20))
                        .foregroundColor(premiumManager.isPremium ? .orange : .purple)
                }
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(premiumManager.isPremium ? "Premium 尊榮會員" : "免費方案")
                        .font(.system(size: 17, weight: .bold, design: .rounded))
                        .foregroundColor(.primary)
                    Text(premiumManager.isPremium ? "自訂每日學習量・解鎖完整 3,070 單字庫" : "每日最多完成 10 題練習")
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                if !premiumManager.isPremium {
                    Button(action: { showPaywall = true }) {
                        Text("升級 Premium")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(
                                LinearGradient(colors: [.purple, .orange], startPoint: .leading, endPoint: .trailing)
                            )
                            .clipShape(Capsule())
                    }
                    .buttonStyle(BorderlessButtonStyle())
                } else {
                    Button(action: { showPaywall = true }) {
                        HStack(spacing: 4) {
                            Text("管理／變更方案")
                                .font(.system(size: 13, weight: .semibold))
                            Image(systemName: "chevron.right")
                                .font(.system(size: 11, weight: .semibold))
                        }
                        .foregroundColor(.purple)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(Color.purple.opacity(0.1))
                        .clipShape(Capsule())
                    }
                    .buttonStyle(BorderlessButtonStyle())
                }
            }
            .contentShape(Rectangle())
            .onTapGesture {
                showPaywall = true
            }
            .padding(.vertical, 4)
            
            Button(action: {
                Task {
                    await premiumManager.restore()
                }
            }) {
                HStack {
                    Image(systemName: "arrow.clockwise.circle")
                        .foregroundColor(.purple)
                    Text("恢復購買 (Restore Purchases)")
                        .foregroundColor(.primary)
                    Spacer()
                    if premiumManager.isLoading {
                        ProgressView()
                    }
                }
            }
            .disabled(premiumManager.isLoading)
        }
    }
    
    // MARK: - Section 2: 二、學習
    private var learningSection: some View {
        Section(
            header: Text("學習"),
            footer: learningFooter
        ) {
            // 每日學習提醒
            Toggle(isOn: Binding(
                get: { notificationManager.isReminderEnabled },
                set: { newValue in
                    Task {
                        await notificationManager.toggleReminder(enabled: newValue)
                    }
                }
            )) {
                HStack(spacing: 12) {
                    Image(systemName: "bell.badge.fill")
                        .foregroundColor(.purple)
                        .font(.system(size: 18))
                    Text("每日學習提醒")
                        .font(.system(size: 16, weight: .medium))
                }
            }
            
            if notificationManager.isReminderEnabled {
                DatePicker(
                    "提醒時間",
                    selection: $notificationManager.reminderDate,
                    displayedComponents: .hourAndMinute
                )
            }
            
            if notificationManager.authorizationStatus == .denied {
                VStack(alignment: .leading, spacing: 8) {
                    HStack(spacing: 6) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundColor(.orange)
                        Text("通知權限已被關閉")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(.orange)
                    }
                    Text("若要接收每日多益學習提醒，請前往 iOS「設定」允許 EnglishCoach 發送通知。")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                    
                    Button(action: {
                        notificationManager.openSystemSettings()
                    }) {
                        Text("前往 iOS 設定開啟通知")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(.purple)
                    }
                    .padding(.top, 2)
                }
                .padding(.vertical, 4)
            }
            
            // 每日學習目標
            HStack {
                HStack(spacing: 12) {
                    Image(systemName: "target")
                        .foregroundColor(.purple)
                        .font(.system(size: 18))
                    Text("每日學習目標")
                        .font(.system(size: 16, weight: .medium))
                }
                Spacer()
                if premiumManager.isPremium {
                    if practiceManager.premiumDailyTarget == -1 {
                        Text("Unlimited (不限)")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(.purple)
                    } else {
                        Text("\(practiceManager.premiumDailyTarget) 題")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(.purple)
                    }
                } else {
                    Text("固定 10 題")
                        .font(.system(size: 15))
                        .foregroundColor(.secondary)
                }
            }
            
            HStack {
                Text("今日完成進度")
                Spacer()
                if premiumManager.isPremium {
                    if practiceManager.premiumDailyTarget == -1 {
                        Text("\(practiceManager.todayCompletedCount) 題（不限）")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(.purple)
                    } else {
                        Text("\(practiceManager.displayedCompletedCount) / \(practiceManager.premiumDailyTarget) 題")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(practiceManager.isDailyLimitReached ? .orange : .purple)
                    }
                } else {
                    Text("\(practiceManager.displayedCompletedCount) / \(DailyPracticeManager.maxFreeDailyQuestions) 題")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(practiceManager.isDailyLimitReached ? .orange : .primary)
                }
            }
            
            if premiumManager.isPremium {
                Picker("調整每日目標", selection: Binding<Int>(
                    get: { practiceManager.premiumDailyTarget },
                    set: { newTarget in
                        practiceManager.setPremiumDailyTarget(newTarget)
                    }
                )) {
                    ForEach(DailyPracticeManager.premiumTargetOptions, id: \.self) { option in
                        if option == -1 {
                            Text("Unlimited (不限)").tag(option)
                        } else {
                            Text("\(option) 題").tag(option)
                        }
                    }
                }
            } else {
                HStack {
                    Text("剩餘免費額度")
                    Spacer()
                    Text("\(practiceManager.remainingFreeQuestions) 題")
                        .foregroundColor(.secondary)
                }
            }
        }
    }
    
    @ViewBuilder
    private var learningFooter: some View {
        if premiumManager.isPremium {
            Text("Premium 尊榮會員可自由調整每日目標題數（5 / 10 / 20 / 30 / 50 / 100 / Unlimited），修改後立即生效。")
                .font(.caption)
                .foregroundColor(.secondary)
        } else {
            Text("免費版每日固定享有 10 題練習。升級 Premium 可自訂每日學習量，或設定為不限題數。")
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
    
    // MARK: - Section 3: 三、個人化
    private var personalizationSection: some View {
        Section(header: Text("個人化")) {
            Button(action: { showingEditProfileSheet = true }) {
                HStack(spacing: 12) {
                    ZStack {
                        Circle()
                            .fill(Color.purple.opacity(0.12))
                            .frame(width: 30, height: 30)
                        Text(userAvatarEmoji)
                            .font(.system(size: 16))
                    }
                    Text("修改名稱＆大頭貼")
                        .foregroundColor(.primary)
                        .font(.system(size: 16))
                    Spacer()
                    Text(userDisplayName)
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                }
            }
            
            Button(action: {
                if let url = URL(string: UIApplication.openSettingsURLString), UIApplication.shared.canOpenURL(url) {
                    UIApplication.shared.open(url)
                }
            }) {
                HStack(spacing: 12) {
                    Image(systemName: "gearshape")
                        .foregroundColor(.purple)
                        .font(.system(size: 18))
                    Text("系統設定")
                        .foregroundColor(.primary)
                        .font(.system(size: 16))
                    Spacer()
                    Text("開啟 iOS 設定")
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                    Image(systemName: "arrow.up.right")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                }
            }
            
            Toggle(isOn: $hapticFeedbackEnabled) {
                HStack(spacing: 12) {
                    Image(systemName: "iphone.radiowaves.left.and.right")
                        .foregroundColor(.purple)
                        .font(.system(size: 18))
                    Text("手機震動")
                        .foregroundColor(.primary)
                        .font(.system(size: 16))
                }
            }
            .onChange(of: hapticFeedbackEnabled) { enabled in
                if enabled {
                    let impact = UIImpactFeedbackGenerator(style: .medium)
                    impact.impactOccurred()
                }
            }
        }
    }
    
    // MARK: - Section 4: 四、互動與支持
    private var interactionSection: some View {
        Section(header: Text("互動與支持")) {
            Button(action: {
                requestAppReview()
            }) {
                HStack(spacing: 12) {
                    Image(systemName: "star.fill")
                        .foregroundColor(.orange)
                        .font(.system(size: 18))
                    Text("幫我們評分")
                        .foregroundColor(.primary)
                        .font(.system(size: 16))
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                }
            }
            
            ShareLink(
                item: URL(string: "https://chunchiech.github.io/EnglishCoach/")!,
                subject: Text("推薦好用的英文學習 App：EnglishCoach"),
                message: Text("推薦你這款「EnglishCoach - 多益單字與口說教練」，每天練習高效累積多益核心單字！")
            ) {
                HStack(spacing: 12) {
                    Image(systemName: "square.and.arrow.up")
                        .foregroundColor(.purple)
                        .font(.system(size: 18))
                    Text("分享 APP 給朋友")
                        .foregroundColor(.primary)
                        .font(.system(size: 16))
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                }
            }
        }
    }
    
    // MARK: - Section 5: 五、社群
    private var socialSection: some View {
        Section(header: Text("社群")) {
            Button(action: {
                openInstagram()
            }) {
                HStack(spacing: 12) {
                    Image(systemName: "camera.circle.fill")
                        .foregroundColor(.pink)
                        .font(.system(size: 20))
                    Text("追蹤 EnglishCoach IG")
                        .foregroundColor(.primary)
                        .font(.system(size: 16))
                    Spacer()
                    Text("@englishcoach_toeic_tw")
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                    Image(systemName: "arrow.up.right")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                }
            }
            
            Button(action: {
                openThreads()
            }) {
                HStack(spacing: 12) {
                    Image(systemName: "at.circle.fill")
                        .foregroundColor(.primary)
                        .font(.system(size: 20))
                    Text("追蹤 EnglishCoach Threads")
                        .foregroundColor(.primary)
                        .font(.system(size: 16))
                    Spacer()
                    Text("@englishcoach_toeic_tw")
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                    Image(systemName: "arrow.up.right")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                }
            }
        }
    }
    
    // MARK: - Section 6: 六、關於
    private var aboutSection: some View {
        Section(header: Text("關於")) {
            HStack {
                Text("App 名稱")
                Spacer()
                Text("EnglishCoach")
                    .foregroundColor(.secondary)
            }
            
            HStack {
                Text("副標題")
                Spacer()
                Text("多益單字與口說教練")
                    .foregroundColor(.secondary)
            }
            
            HStack {
                Text("版本資訊")
                Spacer()
                Text("1.0.2 (3)")
                    .foregroundColor(.secondary)
            }
            
            Link(destination: URL(string: "https://chunchiech.github.io/EnglishCoach/privacy-policy.html") ?? URL(string: "https://apple.com")!) {
                HStack {
                    Text("隱私權政策 (Privacy Policy)")
                        .foregroundColor(.primary)
                    Spacer()
                    Image(systemName: "arrow.up.right")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                }
            }
            
            Link(destination: URL(string: "https://chunchiech.github.io/EnglishCoach/terms-of-service.html") ?? URL(string: "https://apple.com")!) {
                HStack {
                    Text("使用條款 (Terms of Service)")
                        .foregroundColor(.primary)
                    Spacer()
                    Image(systemName: "arrow.up.right")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                }
            }
        }
    }
    
    private func requestAppReview() {
        // Direct link to App Store write-review page.
        // Apple HIG recommended mechanism for user-initiated rating in Settings.
        // Avoids in-app review modal freeze/hang on submission in TestFlight and sandbox environments.
        let appStoreSchemeURL = URL(string: "itms-apps://itunes.apple.com/app/id6800965329?action=write-review")!
        let webFallbackURL = URL(string: "https://apps.apple.com/tw/app/id6800965329?action=write-review")!
        
        if UIApplication.shared.canOpenURL(appStoreSchemeURL) {
            UIApplication.shared.open(appStoreSchemeURL, options: [:]) { success in
                if !success {
                    UIApplication.shared.open(webFallbackURL, options: [:], completionHandler: nil)
                }
            }
        } else {
            UIApplication.shared.open(webFallbackURL, options: [:], completionHandler: nil)
        }
    }
    
    private func openInstagram() {
        let appURL = URL(string: "instagram://user?username=englishcoach_toeic_tw")!
        let webURL = URL(string: "https://www.instagram.com/englishcoach_toeic_tw/")!
        
        if UIApplication.shared.canOpenURL(appURL) {
            UIApplication.shared.open(appURL, options: [:]) { success in
                if !success {
                    UIApplication.shared.open(webURL, options: [:], completionHandler: nil)
                }
            }
        } else {
            UIApplication.shared.open(webURL, options: [:], completionHandler: nil)
        }
    }
    
    private func openThreads() {
        let barcelonaURL = URL(string: "barcelona://user?username=englishcoach_toeic_tw")!
        let threadsURL = URL(string: "threads://user?username=englishcoach_toeic_tw")!
        let webURL = URL(string: "https://www.threads.net/@englishcoach_toeic_tw")!
        
        if UIApplication.shared.canOpenURL(barcelonaURL) {
            UIApplication.shared.open(barcelonaURL, options: [:]) { success in
                if !success {
                    UIApplication.shared.open(webURL, options: [:], completionHandler: nil)
                }
            }
        } else if UIApplication.shared.canOpenURL(threadsURL) {
            UIApplication.shared.open(threadsURL, options: [:]) { success in
                if !success {
                    UIApplication.shared.open(webURL, options: [:], completionHandler: nil)
                }
            }
        } else {
            UIApplication.shared.open(webURL, options: [:], completionHandler: nil)
        }
    }
}

// MARK: - Profile Edit Sheet
struct ProfileEditSheet: View {
    @Binding var displayName: String
    @Binding var avatarEmoji: String
    @Environment(\.dismiss) private var dismiss
    
    @State private var tempName: String = ""
    @State private var tempEmoji: String = ""
    
    private let availableEmojis = ["🐶", "🍞", "🎓", "🌟", "🦁", "🐱", "🐻", "🦊", "🐨", "🚀", "💡", "🎯"]
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("選擇大頭貼")) {
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 6), spacing: 14) {
                        ForEach(availableEmojis, id: \.self) { emoji in
                            ZStack {
                                Circle()
                                    .fill(tempEmoji == emoji ? Color.purple.opacity(0.18) : Color.gray.opacity(0.1))
                                    .frame(width: 44, height: 44)
                                    .overlay(
                                        Circle()
                                            .stroke(tempEmoji == emoji ? Color.purple : Color.clear, lineWidth: 2)
                                    )
                                Text(emoji)
                                    .font(.system(size: 22))
                            }
                            .onTapGesture {
                                tempEmoji = emoji
                            }
                        }
                    }
                    .padding(.vertical, 8)
                }
                
                Section(header: Text("學習者暱稱")) {
                    TextField("請輸入您的名稱", text: $tempName)
                }
            }
            .navigationTitle("個人資料")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("儲存") {
                        let trimmed = tempName.trimmingCharacters(in: .whitespacesAndNewlines)
                        if !trimmed.isEmpty {
                            displayName = trimmed
                        }
                        avatarEmoji = tempEmoji
                        dismiss()
                    }
                }
            }
            .onAppear {
                tempName = displayName
                tempEmoji = avatarEmoji
            }
        }
    }
}
