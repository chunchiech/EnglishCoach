import SwiftUI

public struct LibraryView: View {
    @State private var allWords: [Word] = []
    @State private var searchText = ""
    @State private var selectedWord: Word? = nil
    
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
            .navigationTitle("Word Library")
            .sheet(item: $selectedWord) { word in
                cardReviewSheet(word: word)
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
                
                TextField("Search words or translations...", text: $searchText)
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
                Button(action: { selectedWord = word }) {
                    HStack {
                        VStack(alignment: .leading, spacing: 6) {
                            HStack(spacing: 8) {
                                Text(word.word)
                                    .font(.system(size: 17, weight: .bold, design: .rounded))
                                    .foregroundColor(.primary)
                                
                                if word.learned {
                                    Text("Learned")
                                        .font(.system(size: 10, weight: .bold))
                                        .foregroundColor(.purple)
                                        .padding(.horizontal, 6)
                                        .padding(.vertical, 2)
                                        .background(Color.purple.opacity(0.1))
                                        .cornerRadius(4)
                                }
                            }
                            
                            HStack(spacing: 6) {
                                Text(word.phonetic)
                                    .font(.system(size: 13, weight: .medium, design: .serif))
                                    .foregroundColor(.purple)
                                
                                Text("•")
                                    .foregroundColor(.secondary)
                                
                                Text(word.translation)
                                    .font(.system(size: 13))
                                    .foregroundColor(.secondary)
                                    .lineLimit(1)
                            }
                        }
                        
                        Spacer()
                        
                        // Performance indicators
                        if word.correctCount > 0 || word.wrongCount > 0 {
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
            
            Text("No Matches Found")
                .font(.system(size: 17, weight: .bold, design: .rounded))
            Text("Try searching for another word or character.")
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
                        Text("Close")
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
            .navigationTitle("Word Details")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Close") { selectedWord = nil }
                        .foregroundColor(.purple)
                }
            }
        }
    }
}
