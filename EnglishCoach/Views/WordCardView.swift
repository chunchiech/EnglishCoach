import SwiftUI

public struct WordCardView: View {
    public let word: Word
    @State private var isFlipped = false
    
    public init(word: Word) {
        self.word = word
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
                
                HStack {
                    Image(systemName: "arrow.triangle.2.circlepath")
                        .font(.caption)
                    Text("Tap to reveal meaning")
                        .font(.system(size: 12, weight: .medium))
                }
                .foregroundColor(.secondary)
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
                
                VStack(alignment: .leading, spacing: 10) {
                    Text("Example:")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(.purple)
                        .textCase(.uppercase)
                    
                    Text(word.example)
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(.primary)
                        .fixedSize(horizontal: false, vertical: true)
                    
                    Text(word.exampleTranslation)
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(.horizontal, 24)
                .frame(maxWidth: .infinity, alignment: .leading)
                
                Spacer()
                
                HStack {
                    Image(systemName: "arrow.triangle.2.circlepath")
                        .font(.caption)
                    Text("Tap to view word")
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
