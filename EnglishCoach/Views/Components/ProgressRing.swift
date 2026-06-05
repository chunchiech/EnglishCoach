import SwiftUI

public struct ProgressRing: View {
    public var progress: Double // 0.0 to 1.0
    public var size: CGFloat = 160
    public var strokeWidth: CGFloat = 16
    public var primaryColor: Color = .purple
    public var secondaryColor: Color = Color(.systemGray6)
    
    public init(
        progress: Double,
        size: CGFloat = 160,
        strokeWidth: CGFloat = 16,
        primaryColor: Color = .purple,
        secondaryColor: Color = Color(.systemGray6)
    ) {
        self.progress = progress
        self.size = size
        self.strokeWidth = strokeWidth
        self.primaryColor = primaryColor
        self.secondaryColor = secondaryColor
    }
    
    public var body: some View {
        ZStack {
            Circle()
                .stroke(secondaryColor, lineWidth: strokeWidth)
            
            Circle()
                .trim(from: 0.0, to: CGFloat(min(progress, 1.0)))
                .stroke(
                    LinearGradient(
                        colors: [primaryColor, primaryColor.opacity(0.65)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    style: StrokeStyle(lineWidth: strokeWidth, lineCap: .round)
                )
                .rotationEffect(Angle(degrees: -90))
                .animation(.spring(response: 0.6, dampingFraction: 0.8), value: progress)
            
            VStack(spacing: 4) {
                Text("\(Int(progress * 100))%")
                    .font(.system(size: size * 0.22, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)
                Text("Today's Progress")
                    .font(.system(size: size * 0.08, weight: .medium, design: .rounded))
                    .foregroundColor(.secondary)
            }
        }
        .frame(width: size, height: size)
    }
}
