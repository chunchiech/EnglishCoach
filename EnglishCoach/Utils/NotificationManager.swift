import Foundation
import UserNotifications
import UIKit

@MainActor
public class NotificationManager: ObservableObject {
    public static let shared = NotificationManager()
    
    public static let reminderIdentifier = "EnglishCoach.dailyLearningReminder"
    
    private let kReminderEnabled = "reminder_enabled"
    private let kReminderHour = "reminder_hour"
    private let kReminderMinute = "reminder_minute"
    
    @Published public var isReminderEnabled: Bool {
        didSet {
            UserDefaults.standard.set(isReminderEnabled, forKey: kReminderEnabled)
            if isReminderEnabled {
                scheduleReminder()
            } else {
                cancelReminder()
            }
        }
    }
    
    @Published public var reminderHour: Int {
        didSet {
            UserDefaults.standard.set(reminderHour, forKey: kReminderHour)
            if isReminderEnabled {
                scheduleReminder()
            }
        }
    }
    
    @Published public var reminderMinute: Int {
        didSet {
            UserDefaults.standard.set(reminderMinute, forKey: kReminderMinute)
            if isReminderEnabled {
                scheduleReminder()
            }
        }
    }
    
    @Published public var authorizationStatus: UNAuthorizationStatus = .notDetermined
    @Published public var showPermissionDeniedAlert: Bool = false
    
    public var reminderDate: Date {
        get {
            let calendar = Calendar.current
            var comps = calendar.dateComponents([.year, .month, .day], from: Date())
            comps.hour = reminderHour
            comps.minute = reminderMinute
            return calendar.date(from: comps) ?? Date()
        }
        set {
            let comps = Calendar.current.dateComponents([.hour, .minute], from: newValue)
            if let h = comps.hour, let m = comps.minute {
                updateReminderTime(hour: h, minute: m)
            }
        }
    }
    
    private init() {
        self.isReminderEnabled = UserDefaults.standard.bool(forKey: kReminderEnabled)
        // Default to 20:00 (8:00 PM) if not set yet
        let savedHour = UserDefaults.standard.object(forKey: kReminderHour) as? Int
        self.reminderHour = savedHour ?? 20
        
        let savedMinute = UserDefaults.standard.object(forKey: kReminderMinute) as? Int
        self.reminderMinute = savedMinute ?? 0
        
        Task {
            await checkAuthorizationStatus()
        }
    }
    
    public func checkAuthorizationStatus() async {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        if self.authorizationStatus != settings.authorizationStatus {
            self.authorizationStatus = settings.authorizationStatus
        }
    }
    
    public func toggleReminder(enabled: Bool) async {
        if enabled {
            await checkAuthorizationStatus()
            if authorizationStatus == .denied {
                if self.isReminderEnabled {
                    self.isReminderEnabled = false
                }
                self.showPermissionDeniedAlert = true
                return
            } else if authorizationStatus == .notDetermined {
                let granted = await requestAuthorization()
                if !granted {
                    if self.isReminderEnabled {
                        self.isReminderEnabled = false
                    }
                    self.showPermissionDeniedAlert = true
                    return
                }
            }
            if !self.isReminderEnabled {
                self.isReminderEnabled = true
            }
        } else {
            if self.isReminderEnabled {
                self.isReminderEnabled = false
            }
        }
    }
    
    public func requestAuthorization() async -> Bool {
        do {
            let granted = try await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge])
            await checkAuthorizationStatus()
            return granted
        } catch {
            print("Error requesting notification permission: \(error.localizedDescription)")
            await checkAuthorizationStatus()
            return false
        }
    }
    
    public func updateReminderTime(hour: Int, minute: Int) {
        if self.reminderHour != hour {
            self.reminderHour = hour
        }
        if self.reminderMinute != minute {
            self.reminderMinute = minute
        }
    }
    
    public func createReminderRequest(hour: Int, minute: Int) -> UNNotificationRequest {
        let content = UNMutableNotificationContent()
        content.title = "EnglishCoach 每日學習提醒"
        content.body = "該來練習多益單字囉！每天 10 題核心詞彙，輕鬆掌握多益發音與聽力！"
        content.sound = .default
        
        var dateComponents = DateComponents()
        dateComponents.hour = hour
        dateComponents.minute = minute
        
        let trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: true)
        return UNNotificationRequest(identifier: Self.reminderIdentifier, content: content, trigger: trigger)
    }
    
    public func scheduleReminder() {
        // Cancel existing EnglishCoach reminder to prevent duplicates
        cancelReminder()
        
        guard isReminderEnabled else { return }
        
        let targetHour = reminderHour
        let targetMinute = reminderMinute
        let request = createReminderRequest(hour: targetHour, minute: targetMinute)
        
        Task { @MainActor in
            do {
                try await UNUserNotificationCenter.current().add(request)
                print("Daily learning reminder scheduled for \(String(format: "%02d:%02d", targetHour, targetMinute))")
            } catch {
                print("Failed to schedule notification: \(error.localizedDescription)")
            }
        }
    }
    
    public func cancelReminder() {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [Self.reminderIdentifier])
        UNUserNotificationCenter.current().removeDeliveredNotifications(withIdentifiers: [Self.reminderIdentifier])
    }
    
    public func openSystemSettings() {
        if let url = URL(string: UIApplication.openSettingsURLString), UIApplication.shared.canOpenURL(url) {
            UIApplication.shared.open(url)
        }
    }
}
