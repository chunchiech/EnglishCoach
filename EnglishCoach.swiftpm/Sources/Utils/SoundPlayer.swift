import Foundation
import AVFoundation

@MainActor
public final class SoundPlayer: NSObject, ObservableObject {
    public static let shared = SoundPlayer()
    
    private let synthesizer = AVSpeechSynthesizer()
    private let voice = AVSpeechSynthesisVoice(language: "en-US")
    
    @Published public private(set) var isSpeaking: Bool = false
    @Published public private(set) var currentlyPlayingText: String? = nil
    
    override private init() {
        super.init()
        synthesizer.delegate = self
        configureAudioSession()
    }
    
    private func configureAudioSession() {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .spokenAudio, options: [.duckOthers])
            try session.setActive(true, options: .notifyOthersOnDeactivation)
        } catch {
            print("Failed to configure AVAudioSession: \(error.localizedDescription)")
        }
    }
    
    public func speak(_ text: String) {
        speakWord(text)
    }
    
    public func speakWord(_ word: String) {
        let trimmed = word.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        playText(trimmed, rate: 0.48)
    }
    
    public func speakSentence(_ sentence: String) {
        let trimmed = sentence.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        playText(trimmed, rate: 0.45) // Slightly measured pace for clear full sentences
    }
    
    public func stopSpeaking() {
        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }
        self.isSpeaking = false
        self.currentlyPlayingText = nil
    }
    
    private func playText(_ text: String, rate: Float) {
        // Prevent speech queue piling up: cancel current speech immediately
        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }
        
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = voice
        utterance.rate = rate
        utterance.pitchMultiplier = 1.0
        utterance.volume = 1.0
        
        configureAudioSession()
        
        self.isSpeaking = true
        self.currentlyPlayingText = text
        
        synthesizer.speak(utterance)
    }
}

// MARK: - AVSpeechSynthesizerDelegate
extension SoundPlayer: AVSpeechSynthesizerDelegate {
    nonisolated public func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        let text = utterance.speechString
        Task { @MainActor [weak self] in
            guard let self = self else { return }
            if self.currentlyPlayingText == text {
                self.isSpeaking = false
                self.currentlyPlayingText = nil
            }
        }
    }
    
    nonisolated public func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didCancel utterance: AVSpeechUtterance) {
        let text = utterance.speechString
        Task { @MainActor [weak self] in
            guard let self = self else { return }
            if self.currentlyPlayingText == text {
                self.isSpeaking = false
                self.currentlyPlayingText = nil
            }
        }
    }
}

