import SwiftUI

public struct MainTabView: View {
    @State private var selectedTab = 0
    
    public init() {}
    
    public var body: some View {
        TabView(selection: $selectedTab) {
            DashboardView()
                .tabItem {
                    Label("Dashboard", systemImage: "house.fill")
                }
                .tag(0)
            
            LibraryView()
                .tabItem {
                    Label("Library", systemImage: "books.vertical.fill")
                }
                .tag(1)
        }
        .accentColor(.purple)
    }
}
