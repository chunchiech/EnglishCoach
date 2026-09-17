import SwiftUI

public struct WordCardView: View {
    public let word: Word
    @Binding private var externalIsFlipped: Bool
    @State private var internalIsFlipped = false
    private let hasExternalBinding: Bool
    @ObservedObject private var soundPlayer = SoundPlayer.shared
    
    private var isFlipped: Bool {
        get { hasExternalBinding ? externalIsFlipped : internalIsFlipped }
        nonmutating set {
            if hasExternalBinding {
                externalIsFlipped = newValue
            } else {
                internalIsFlipped = newValue
            }
        }
    }
    
    public init(word: Word, isFlipped: Binding<Bool>? = nil) {
        self.word = word
        if let binding = isFlipped {
            self._externalIsFlipped = binding
            self.hasExternalBinding = true
        } else {
            self._externalIsFlipped = .constant(false)
            self.hasExternalBinding = false
        }
    }
    
    public var body: some View {
        ZStack {
            // Front Card
            cardFace(isBack: false)
                .opacity(isFlipped ? 0.0 : 1.0)
                .rotation3DEffect(.degrees(isFlipped ? 180 : 0), axis: (x: 0, y: 1, z: 0))
            
            // Back Card
            cardFace(isBack: true)
                .opacity(isFlipped ? 1.0 : 0.0)
                .rotation3DEffect(.degrees(isFlipped ? 0 : -180), axis: (x: 0, y: 1, z: 0))
        }
        .contentShape(Rectangle())
        .onTapGesture {
            withAnimation(.spring(response: 0.5, dampingFraction: 0.75)) {
                isFlipped.toggle()
            }
        }
        .onAppear {
            isFlipped = false
            // Auto-pronounce when card appears
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                SoundPlayer.shared.speak(word.word)
            }
        }
        .onChange(of: word) { _ in
            isFlipped = false
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                SoundPlayer.shared.speak(word.word)
            }
        }
    }
    
    @ViewBuilder
    private func cardFace(isBack: Bool) -> some View {
        VStack(spacing: 20) {
            if !isBack {
                // Front Design
                Spacer()
                
                Text(word.word)
                    .font(.system(size: 38, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
                
                if !word.phonetic.isEmpty {
                    Text(word.phonetic)
                        .font(.system(size: 18, weight: .medium, design: .serif))
                        .foregroundColor(.purple)
                        .padding(.vertical, 4)
                        .padding(.horizontal, 16)
                        .background(Color.purple.opacity(0.1))
                        .cornerRadius(20)
                }
                
                if !word.partOfSpeech.isEmpty || !word.level.isEmpty {
                    Text(!word.partOfSpeech.isEmpty ? "\(word.partOfSpeech) · \(word.level)" : word.level)
                        .font(.system(size: 13, weight: .semibold, design: .rounded))
                        .foregroundColor(.secondary)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 4)
                        .background(Color(.systemGray6))
                        .cornerRadius(12)
                }
                
                Button(action: {
                    SoundPlayer.shared.speak(word.word)
                }) {
                    Image(systemName: "speaker.wave.3.fill")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(14)
                        .background(
                            LinearGradient(
                                colors: [.purple, .blue],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .clipShape(Circle())
                        .shadow(color: Color.purple.opacity(0.3), radius: 8, x: 0, y: 4)
                }
                .buttonStyle(PlainButtonStyle())
                
                Spacer()
                
                HStack(spacing: 6) {
                    Text("👆 點一下查看中文意思")
                        .font(.system(size: 14, weight: .bold, design: .rounded))
                        .foregroundColor(.purple)
                }
                .padding(.vertical, 8)
                .padding(.horizontal, 16)
                .background(Color.purple.opacity(0.12))
                .cornerRadius(20)
                .padding(.bottom, 20)
                
            } else {
                // Back Design
                Spacer()
                
                VStack(spacing: 8) {
                    Text(word.word)
                        .font(.system(size: 20, weight: .semibold, design: .rounded))
                        .foregroundColor(.secondary)
                    
                    Text(word.translation)
                        .font(.system(size: 28, weight: .bold, design: .rounded))
                        .foregroundColor(.primary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                }
                
                Divider()
                    .padding(.horizontal, 30)
                
                if !word.example.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    VStack(alignment: .leading, spacing: 10) {
                        HStack(alignment: .center, spacing: 8) {
                            Text("例句：")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(.purple)
                                .textCase(.uppercase)
                            
                            Button(action: {
                                SoundPlayer.shared.speakSentence(word.example)
                            }) {
                                HStack(spacing: 4) {
                                    Image(systemName: soundPlayer.currentlyPlayingText == word.example ? "speaker.wave.3.fill" : "speaker.wave.2")
                                        .font(.system(size: 12, weight: .semibold))
                                    Text("朗讀例句")
                                        .font(.system(size: 11, weight: .bold))
                                }
                                .foregroundColor(.purple)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color.purple.opacity(0.12))
                                .cornerRadius(8)
                            }
                            .buttonStyle(PlainButtonStyle())
                        }
                        
                        Text(word.example)
                            .font(.system(size: 16, weight: .medium))
                            .foregroundColor(.primary)
                            .fixedSize(horizontal: false, vertical: true)
                        
                        if !word.exampleTranslation.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                            Text(word.exampleTranslation)
                                .font(.system(size: 14))
                                .foregroundColor(.secondary)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }
                    .padding(.horizontal, 24)
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                
                Spacer()
                
                HStack {
                    Image(systemName: "arrow.triangle.2.circlepath")
                        .font(.caption)
                    Text("點擊查看單字")
                        .font(.system(size: 12, weight: .medium))
                }
                .foregroundColor(.secondary)
                .padding(.bottom, 20)
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: 380)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(Color(.secondarySystemGroupedBackground))
                .shadow(color: Color.black.opacity(0.08), radius: 15, x: 0, y: 8)
                .overlay(
                    RoundedRectangle(cornerRadius: 24)
                        .stroke(
                            LinearGradient(
                                colors: [.purple.opacity(0.2), .blue.opacity(0.1)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 1.5
                        )
                )
        )
        .padding(.horizontal, 24)
    }
}
