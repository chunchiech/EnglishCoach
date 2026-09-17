import SwiftUI

public struct MainTabView: View {
    @State private var selectedTab = 0
    @State private var showNotificationOnboarding = false
    @State private var showPersonalizedOnboarding = false
    
    public init() {}
    
    public var body: some View {
        TabView(selection: $selectedTab) {
            DashboardView()
                .tabItem {
                    Label("首頁", systemImage: "house.fill")
                }
                .tag(0)
            
            SettingsView()
                .tabItem {
                    Label("設定", systemImage: "gearshape.fill")
                }
                .tag(1)
        }
        .accentColor(.purple)
        .fullScreenCover(isPresented: $showPersonalizedOnboarding) {
            PersonalizedOnboardingView {
                checkAndShowNotificationOnboarding()
            }
        }
        .sheet(isPresented: $showNotificationOnboarding) {
            NotificationOnboardingView()
        }
        .onAppear {
            checkAndShowOnboardingFlow()
        }
    }
    
    private func checkAndShowOnboardingFlow() {
        let hasCompletedPersonalized = UserDefaults.standard.bool(forKey: PersonalizedOnboardingPreferences.onboardingCompletedKey)
        if !hasCompletedPersonalized {
            showPersonalizedOnboarding = true
        } else {
            checkAndShowNotificationOnboarding()
        }
    }
    
    private func checkAndShowNotificationOnboarding() {
        let hasCompleted = UserDefaults.standard.bool(forKey: NotificationOnboardingView.onboardingFlagKey)
        if !hasCompleted {
            showNotificationOnboarding = true
        }
    }
}
