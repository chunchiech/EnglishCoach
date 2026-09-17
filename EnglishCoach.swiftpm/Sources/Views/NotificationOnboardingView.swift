import SwiftUI
import UserNotifications

public struct NotificationOnboardingView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var notificationManager = NotificationManager.shared
    
    public struct TimeOption: Identifiable, Equatable {
        public let id: String
        public let hour: Int
        public let minute: Int
        public let displayTime: String
        public let periodTag: String
        public let icon: String
        
        public init(id: String, hour: Int, minute: Int, displayTime: String, periodTag: String, icon: String) {
            self.id = id
            self.hour = hour
            self.minute = minute
            self.displayTime = displayTime
            self.periodTag = periodTag
            self.icon = icon
        }
    }
    
    public let timeOptions: [TimeOption] = [
        TimeOption(id: "opt_08", hour: 8, minute: 0, displayTime: "08:00", periodTag: "晨間充電", icon: "sun.max.fill"),
        TimeOption(id: "opt_12", hour: 12, minute: 0, displayTime: "12:00", periodTag: "午休放鬆", icon: "sun.haze.fill"),
        TimeOption(id: "opt_19", hour: 19, minute: 0, displayTime: "19:00", periodTag: "晚間學習", icon: "sunset.fill"),
        TimeOption(id: "opt_21", hour: 21, minute: 0, displayTime: "21:00", periodTag: "睡前複習", icon: "moon.stars.fill")
    ]
    
    @State private var selectedOptionId: String = "opt_19"
    @State private var isProcessing: Bool = false
    
    public static let onboardingFlagKey = "hasCompletedNotificationOnboarding"
    
    public var onFinished: (() -> Void)? = nil
    
    public init(onFinished: (() -> Void)? = nil) {
        self.onFinished = onFinished
    }
    
    public var body: some View {
        NavigationView {
            ZStack {
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()
                
                VStack(spacing: 0) {
                    ScrollView {
                        VStack(spacing: 24) {
                            // Top Bell Badge
                            headerBadge
                                .padding(.top, 20)
                            
                            // Title & Subtitle & Description
                            VStack(spacing: 10) {
                                Text("設定每日學習提醒")
                                    .font(.system(size: 26, weight: .bold, design: .rounded))
                                    .foregroundColor(.primary)
                                
                                Text("每天提醒你回來學英文")
                                    .font(.system(size: 17, weight: .semibold, design: .rounded))
                                    .foregroundColor(.purple)
                                
                                Text("選一個適合你的時間，讓 EnglishCoach 每天提醒你背單字。")
                                    .font(.system(size: 15))
                                    .foregroundColor(.secondary)
                                    .multilineTextAlignment(.center)
                                    .padding(.horizontal, 16)
                            }
                            
                            // Time Options
                            timeOptionsSection
                                .padding(.top, 6)
                        }
                        .padding(.horizontal, 24)
                        .padding(.bottom, 24)
                    }
                    
                    // Bottom Buttons
                    actionSection
                        .padding(.horizontal, 24)
                        .padding(.vertical, 16)
                        .background(
                            Color(.secondarySystemGroupedBackground)
                                .shadow(color: Color.black.opacity(0.05), radius: 8, x: 0, y: -4)
                        )
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        handleSkip()
                    }) {
                        Text("先不用")
                            .font(.system(size: 15))
                            .foregroundColor(.secondary)
                    }
                }
            }
        }
        .interactiveDismissDisabled(isProcessing)
    }
    
    private var headerBadge: some View {
        ZStack {
            Circle()
                .fill(
                    LinearGradient(
                        colors: [Color.purple.opacity(0.18), Color.blue.opacity(0.18)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: 88, height: 88)
            
            Image(systemName: "bell.badge.fill")
                .font(.system(size: 40))
                .foregroundStyle(
                    LinearGradient(
                        colors: [.purple, .blue],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .shadow(color: Color.purple.opacity(0.3), radius: 6, x: 0, y: 3)
        }
    }
    
    private var timeOptionsSection: some View {
        VStack(spacing: 12) {
            ForEach(timeOptions) { option in
                let isSelected = selectedOptionId == option.id
                Button(action: {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        selectedOptionId = option.id
                    }
                }) {
                    HStack(spacing: 16) {
                        Image(systemName: option.icon)
                            .font(.system(size: 20))
                            .foregroundColor(isSelected ? .purple : .secondary)
                            .frame(width: 32)
                        
                        VStack(alignment: .leading, spacing: 2) {
                            Text(option.displayTime)
                                .font(.system(size: 20, weight: .bold, design: .rounded))
                                .foregroundColor(isSelected ? .primary : .secondary)
                            
                            Text(option.periodTag)
                                .font(.system(size: 12, weight: .medium))
                                .foregroundColor(.secondary)
                        }
                        
                        Spacer()
                        
                        ZStack {
                            Circle()
                                .stroke(isSelected ? Color.purple : Color.secondary.opacity(0.3), lineWidth: 2)
                                .frame(width: 22, height: 22)
                            
                            if isSelected {
                                Circle()
                                    .fill(Color.purple)
                                    .frame(width: 14, height: 14)
                            }
                        }
                    }
                    .padding(.horizontal, 18)
                    .padding(.vertical, 14)
                    .background(Color(.secondarySystemGroupedBackground))
                    .cornerRadius(16)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(isSelected ? Color.purple : Color.clear, lineWidth: 2)
                    )
                }
                .buttonStyle(PlainButtonStyle())
            }
        }
    }
    
    private var actionSection: some View {
        VStack(spacing: 12) {
            Button(action: {
                handleEnableReminder()
            }) {
                HStack {
                    if isProcessing {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            .padding(.trailing, 6)
                    }
                    Text("開啟每日提醒")
                        .font(.system(size: 17, weight: .bold, design: .rounded))
                }
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
                .shadow(color: Color.purple.opacity(0.3), radius: 6, x: 0, y: 3)
            }
            .disabled(isProcessing)
            
            Button(action: {
                handleSkip()
            }) {
                Text("先不用")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
            }
            .disabled(isProcessing)
        }
    }
    
    private func handleEnableReminder() {
        guard let selected = timeOptions.first(where: { $0.id == selectedOptionId }) else { return }
        isProcessing = true
        
        Task {
            // Request official iOS notification authorization
            let granted = await notificationManager.requestAuthorization()
            
            if granted {
                // Synchronize time and enable reminder in existing NotificationManager
                notificationManager.updateReminderTime(hour: selected.hour, minute: selected.minute)
                notificationManager.isReminderEnabled = true
            } else {
                notificationManager.isReminderEnabled = false
            }
            
            // Mark onboarding as completed
            UserDefaults.standard.set(true, forKey: Self.onboardingFlagKey)
            isProcessing = false
            
            onFinished?()
            dismiss()
        }
    }
    
    private func handleSkip() {
        // Do not request notification permission, do not schedule
        UserDefaults.standard.set(true, forKey: Self.onboardingFlagKey)
        onFinished?()
        dismiss()
    }
}
