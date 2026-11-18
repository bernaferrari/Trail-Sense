import SwiftUI
import TrailSenseShared

@main
struct TrailSenseApp: App {
    var body: some Scene {
        WindowGroup {
            SharedContent()
                .ignoresSafeArea(.container)
                .onOpenURL { url in
                    if url.scheme == "trailsense", url.host == "backtrack" {
                        IosAppActionsKt.openBacktrack()
                    }
                }
        }
    }
}

private struct SharedContent: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        return MainViewControllerKt.MainViewController(
            databasePath: documents.appendingPathComponent("notes.sqlite").path,
            turnBackScheduler: makeTurnBackScheduler(),
            backtrackActivity: BacktrackLiveActivity()
        )
    }

    private func makeTurnBackScheduler() -> IosTurnBackScheduler? {
        if #available(iOS 26, *) { return TurnBackScheduler() }
        return nil
    }

    func updateUIViewController(_ controller: UIViewController, context: Context) {}
}
