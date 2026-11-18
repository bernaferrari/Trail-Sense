import AppIntents
import TrailSenseShared

struct StartBacktrackIntent: AppIntent {
    static let title: LocalizedStringResource = "Start Backtrack"
    static let openAppWhenRun = true

    @MainActor
    func perform() async throws -> some IntentResult {
        IosAppActionsKt.requestStartBacktrack()
        return .result()
    }
}

struct SaveSpotIntent: AppIntent {
    static let title: LocalizedStringResource = "Save This Spot"
    static let openAppWhenRun = true

    @MainActor
    func perform() async throws -> some IntentResult {
        IosAppActionsKt.requestSaveCurrentPlace()
        return .result()
    }
}

struct TrailSenseShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: StartBacktrackIntent(),
            phrases: ["Start Backtrack in \(.applicationName)"],
            shortTitle: "Start Backtrack",
            systemImageName: "point.topleft.down.to.point.bottomright.curvepath"
        )
        AppShortcut(
            intent: SaveSpotIntent(),
            phrases: ["Save this spot in \(.applicationName)"],
            shortTitle: "Save This Spot",
            systemImageName: "mappin"
        )
    }
}
