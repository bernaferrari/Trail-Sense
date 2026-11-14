import SwiftUI
import TrailSenseShared

@main
struct TrailSenseApp: App {
    var body: some Scene {
        WindowGroup { SharedContent() }
    }
}

private struct SharedContent: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        return MainViewControllerKt.MainViewController(databasePath: documents.appendingPathComponent("notes.sqlite").path)
    }

    func updateUIViewController(_ controller: UIViewController, context: Context) {}
}
