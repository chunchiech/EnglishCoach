import SwiftUI

public struct MainTabView: View {
    @State private var selectedTab = 0
    
    public init() {}
    
    public var body: some View {
        TabView(selection: $selectedTab) {
            DashboardView()
                .tabItem {
                    Label("首頁", systemImage: "house.fill")
                }
                .tag(0)
            
            LibraryView()
                .tabItem {
                    Label("單字庫", systemImage: "books.vertical.fill")
                }
                .tag(1)
        }
        .accentColor(.purple)
    }
}
