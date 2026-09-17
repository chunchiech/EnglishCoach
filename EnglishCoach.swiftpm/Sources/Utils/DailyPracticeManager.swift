import Foundation
import Combine

@MainActor
public class DailyPracticeManager: ObservableObject {
    public static let shared = DailyPracticeManager()
    
    public static let maxFreeDailyQuestions = 10
    public static let premiumTargetOptions: [Int] = [5, 10, 20, 30, 50, 100, -1] // -1 = 不限 (Unlimited)
    public static let defaultPremiumDailyTarget: Int = 10
    
    private let kDailyCount = "daily_practice_completed_count"
    private let kDailyDate = "daily_practice_date"
    private let kDailyQuestionIds = "daily_practice_question_ids"
    private let kPremiumDailyTarget = "premium_daily_target"
    
    @Published public private(set) var todayCompletedCount: Int = 0
    @Published public private(set) var premiumDailyTarget: Int = 10
    
    public var displayedCompletedCount: Int {
        if isPremiumUser {
            if premiumDailyTarget == -1 {
                return todayCompletedCount
            }
            return min(todayCompletedCount, premiumDailyTarget)
        }
        return min(todayCompletedCount, Self.maxFreeDailyQuestions)
    }
    
    private var completedQuestionIds: Set<String> = []
    private var cancellables = Set<AnyCancellable>()
    
    private init() {
        var resolvedTarget = Self.defaultPremiumDailyTarget
        if let savedTarget = UserDefaults.standard.object(forKey: kPremiumDailyTarget) as? Int {
            if savedTarget == 200 || savedTarget == 300 {
                // Safely migrate deprecated 200 / 300 options to 100
                resolvedTarget = 100
                UserDefaults.standard.set(100, forKey: kPremiumDailyTarget)
            } else if Self.premiumTargetOptions.contains(savedTarget) {
                resolvedTarget = savedTarget
            } else {
                resolvedTarget = Self.defaultPremiumDailyTarget
                UserDefaults.standard.set(Self.defaultPremiumDailyTarget, forKey: kPremiumDailyTarget)
            }
        }
        self.premiumDailyTarget = resolvedTarget
        checkAndResetDailyIfNeeded()
        
        PremiumManager.shared.$isPremium
            .receive(on: RunLoop.main)
            .sink { [weak self] _ in
                self?.objectWillChange.send()
            }
            .store(in: &cancellables)
    }
    
    public func setPremiumDailyTarget(_ target: Int) {
        guard Self.premiumTargetOptions.contains(target) else { return }
        if self.premiumDailyTarget != target {
            self.premiumDailyTarget = target
            UserDefaults.standard.set(target, forKey: kPremiumDailyTarget)
        }
    }
    
    public var isPremiumUser: Bool {
        return PremiumManager.shared.isPremium
    }
    
    public var effectiveDailyLimit: Int {
        if isPremiumUser {
            return premiumDailyTarget
        }
        return Self.maxFreeDailyQuestions
    }
    
    public var isDailyLimitReached: Bool {
        if isPremiumUser {
            if premiumDailyTarget == -1 {
                return false // 不限
            }
            return todayCompletedCount >= premiumDailyTarget
        }
        return todayCompletedCount >= Self.maxFreeDailyQuestions
    }
    
    public var remainingFreeQuestions: Int {
        return max(0, Self.maxFreeDailyQuestions - todayCompletedCount)
    }
    
    public var dailyWordTarget: Int {
        if isPremiumUser {
            return premiumDailyTarget == -1 ? 50 : premiumDailyTarget
        }
        return Self.maxFreeDailyQuestions
    }
    
    public var remainingQuestions: Int {
        if isPremiumUser {
            if premiumDailyTarget == -1 {
                return Int.max
            }
            return max(0, premiumDailyTarget - todayCompletedCount)
        }
        return remainingFreeQuestions
    }
    
    public func isQuestionCompletedToday(id: String) -> Bool {
        checkAndResetDailyIfNeeded()
        let trimmedId = id.trimmingCharacters(in: .whitespacesAndNewlines)
        return completedQuestionIds.contains(trimmedId)
    }
    
    public func recordCompletedQuestion(id: String) -> Bool {
        checkAndResetDailyIfNeeded()
        
        let trimmedId = id.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedId.isEmpty else { return false }
        
        // Prevent double-counting the same question on the same day
        if completedQuestionIds.contains(trimmedId) {
            return false
        }
        
        completedQuestionIds.insert(trimmedId)
        todayCompletedCount += 1
        
        saveState()
        return true
    }
    
    public func checkAndResetDailyIfNeeded() {
        let currentDateString = getCurrentDateString()
        let savedDate = UserDefaults.standard.string(forKey: kDailyDate)
        
        if savedDate != currentDateString {
            // New day: reset counter and question tracking
            if self.todayCompletedCount != 0 {
                self.todayCompletedCount = 0
            }
            if !self.completedQuestionIds.isEmpty {
                self.completedQuestionIds = []
            }
            UserDefaults.standard.set(currentDateString, forKey: kDailyDate)
            UserDefaults.standard.set(0, forKey: kDailyCount)
            UserDefaults.standard.set([String](), forKey: kDailyQuestionIds)
        } else {
            // Same day: load persisted counts only if different
            let savedCount = UserDefaults.standard.integer(forKey: kDailyCount)
            if self.todayCompletedCount != savedCount {
                self.todayCompletedCount = savedCount
            }
            let savedIds = UserDefaults.standard.stringArray(forKey: kDailyQuestionIds) ?? []
            let idSet = Set(savedIds)
            if self.completedQuestionIds != idSet {
                self.completedQuestionIds = idSet
            }
        }
    }
    
    private func saveState() {
        let currentDateString = getCurrentDateString()
        UserDefaults.standard.set(currentDateString, forKey: kDailyDate)
        UserDefaults.standard.set(todayCompletedCount, forKey: kDailyCount)
        UserDefaults.standard.set(Array(completedQuestionIds), forKey: kDailyQuestionIds)
    }
    
    private func getCurrentDateString() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.timeZone = Calendar.current.timeZone
        return formatter.string(from: Date())
    }
    
    // For unit/simulator testing
    public func resetForTesting() {
        self.todayCompletedCount = 0
        self.completedQuestionIds = []
        saveState()
    }
}
