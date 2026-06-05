import Foundation
import AVFoundation

public class SoundPlayer {
    public static let shared = SoundPlayer()
    private let synthesizer = AVSpeechSynthesizer()
    
    private init() {}
    
    public func speak(_ text: String) {
        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }
        
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: "en-US")
        utterance.rate = 0.48 // Balanced, natural pace for language learners
        utterance.pitchMultiplier = 1.0
        utterance.volume = 1.0
        
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: [])
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            print("Failed to set up audio session: \(error)")
        }
        
        synthesizer.speak(utterance)
    }
}
