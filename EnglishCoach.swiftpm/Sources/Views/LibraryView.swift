import SwiftUI

public struct LibraryView: View {
    @State private var allWords: [Word] = []
    @State private var searchText = ""
    @State private var selectedWord: Word? = nil
    @State private var showPaywall = false
    
    @StateObject private var premiumManager = PremiumManager.shared
    
    public init() {}
    
    private var filteredWords: [Word] {
        if searchText.isEmpty {
            return allWords
        } else {
            return allWords.filter {
                $0.word.localizedCaseInsensitiveContains(searchText) ||
                $0.translation.localizedCaseInsensitiveContains(searchText)
            }
        }
    }
    
    public var body: some View {
        NavigationView {
            ZStack {
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()
                
                VStack(spacing: 0) {
                    // Search Bar
                    searchBar
                    
                    if filteredWords.isEmpty {
                        emptyStateView
                    } else {
                        wordList
                    }
                }
            }
            .navigationTitle("單字庫")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    if !premiumManager.isPremium {
                        Button(action: { showPaywall = true }) {
                            HStack(spacing: 4) {
                                Image(systemName: "crown.fill")
                                    .font(.system(size: 12))
                                Text("升級")
                                    .font(.system(size: 13, weight: .bold))
                            }
                            .foregroundColor(.white)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(LinearGradient(colors: [.purple, .orange], startPoint: .leading, endPoint: .trailing))
                            .clipShape(Capsule())
                        }
                    }
                }
            }
            .sheet(item: $selectedWord) { word in
                cardReviewSheet(word: word)
            }
            .sheet(isPresented: $showPaywall) {
                PremiumView()
            }
            .onAppear {
                loadAllWords()
            }
        }
    }
    
    private var searchBar: some View {
        HStack {
            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(.secondary)
                
                TextField("搜尋單字或中文翻譯...", text: $searchText)
                    .font(.system(size: 15))
                    .autocorrectionDisabled(true)
                    .textInputAutocapitalization(.never)
                
                if !searchText.isEmpty {
                    Button(action: { searchText = "" }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.secondary)
                    }
                }
            }
            .padding(10)
            .background(Color(.secondarySystemGroupedBackground))
            .cornerRadius(12)
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 12)
    }
    
    private var wordList: some View {
        List {
            ForEach(filteredWords) { word in
                let isLocked = !premiumManager.isPremium && word.id > 300
                Button(action: {
                    if isLocked {
                        showPaywall = true
                    } else {
                        selectedWord = word
                    }
                }) {
                    HStack {
                        VStack(alignment: .leading, spacing: 6) {
                            HStack(spacing: 8) {
                                Text(word.word)
                                    .font(.system(size: 17, weight: .bold, design: .rounded))
                                    .foregroundColor(isLocked ? .secondary : .primary)
                                
                                if word.learned {
                                    Text("已學習")
                                        .font(.system(size: 10, weight: .bold))
                                        .foregroundColor(.purple)
                                        .padding(.horizontal, 6)
                                        .padding(.vertical, 2)
                                        .background(Color.purple.opacity(0.1))
                                        .cornerRadius(4)
                                }
                                
                                Text(translatedLevel(word.level))
                                    .font(.system(size: 10, weight: .bold))
                                    .foregroundColor(levelColor(word.level))
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(levelColor(word.level).opacity(0.1))
                                    .cornerRadius(4)
                                
                                if isLocked {
                                    HStack(spacing: 2) {
                                        Image(systemName: "lock.fill")
                                            .font(.system(size: 9))
                                        Text("Premium")
                                            .font(.system(size: 10, weight: .bold))
                                    }
                                    .foregroundColor(.orange)
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(Color.orange.opacity(0.12))
                                    .cornerRadius(4)
                                }
                            }
                            
                            HStack(spacing: 6) {
                                Text(word.phonetic)
                                    .font(.system(size: 13, weight: .medium, design: .serif))
                                    .foregroundColor(.purple)
                                
                                Text("•")
                                    .foregroundColor(.secondary)
                                
                                Text(isLocked ? "升級解鎖中文翻譯與例句" : word.translation)
                                    .font(.system(size: 13))
                                    .foregroundColor(.secondary)
                                    .lineLimit(1)
                            }
                        }
                        
                        Spacer()
                        
                        // Performance indicators or lock
                        if isLocked {
                            Image(systemName: "lock.circle.fill")
                                .font(.system(size: 18))
                                .foregroundColor(.orange)
                        } else if word.correctCount > 0 || word.wrongCount > 0 {
                            HStack(spacing: 8) {
                                if word.correctCount > 0 {
                                    Text("\(word.correctCount)✓")
                                        .font(.system(size: 11, weight: .bold))
                                        .foregroundColor(.green)
                                }
                                if word.wrongCount > 0 {
                                    Text("\(word.wrongCount)✗")
                                        .font(.system(size: 11, weight: .bold))
                                        .foregroundColor(.red)
                                }
                            }
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color(.systemGray5))
                            .cornerRadius(6)
                        }
                        
                        Image(systemName: "chevron.right")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.secondary)
                            .padding(.leading, 4)
                    }
                    .padding(.vertical, 4)
                }
            }
        }
        .listStyle(InsetGroupedListStyle())
    }
    
    private var emptyStateView: some View {
        VStack(spacing: 16) {
            Spacer()
            Image(systemName: "questionmark.folder")
                .font(.system(size: 50))
                .foregroundColor(.secondary)
            
            Text("未找到符合的單字")
                .font(.system(size: 17, weight: .bold, design: .rounded))
            Text("請嘗試搜尋其他單字或中文。")
                .font(.system(size: 14))
                .foregroundColor(.secondary)
            Spacer()
        }
    }
    
    private func loadAllWords() {
        allWords = DatabaseManager.shared.getAllWords()
    }
    
    @ViewBuilder
    private func cardReviewSheet(word: Word) -> some View {
        NavigationView {
            ZStack {
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()
                
                VStack(spacing: 24) {
                    Spacer()
                    WordCardView(word: word)
                    Spacer()
                    
                    Button(action: { selectedWord = nil }) {
                        Text("關閉")
                            .font(.system(size: 16, weight: .bold, design: .rounded))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(Color.purple)
                            .cornerRadius(16)
                    }
                    .padding(.horizontal, 24)
                    .padding(.bottom, 20)
                }
            }
            .navigationTitle("單字詳情")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("關閉") { selectedWord = nil }
                        .foregroundColor(.purple)
                }
            }
        }
    }
    
    private func translatedLevel(_ level: String) -> String {
        switch level {
        case "Beginner": return "初級"
        case "Intermediate": return "中級"
        case "Advanced": return "高級"
        default: return level
        }
    }
    
    private func levelColor(_ level: String) -> Color {
        switch level {
        case "Beginner": return .green
        case "Intermediate": return .orange
        case "Advanced": return .red
        default: return .secondary
        }
    }
}
