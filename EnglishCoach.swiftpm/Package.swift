// swift-tools-version: 5.9
import PackageDescription
import AppleProductTypes

let package = Package(
    name: "EnglishCoach",
    platforms: [
        .iOS(.v16)
    ],
    products: [
        .iOSApplication(
            name: "EnglishCoach",
            targets: ["App"],
            bundleIdentifier: "com.andy.EnglishCoach",
            displayVersion: "1.0",
            bundleVersion: "1",
            appIcon: .asset("AppIcon"),
            accentColor: .presetColor(.purple),
            supportedDeviceFamilies: [
                .phone,
                .pad
            ],
            supportedInterfaceOrientations: [
                .portrait
            ]
        )
    ],
    targets: [
        .executableTarget(
            name: "App",
            path: "Sources",
            resources: [
                .process("Resources")
            ]
        )
    ]
)
