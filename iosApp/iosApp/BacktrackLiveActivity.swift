import ActivityKit
import Foundation
import TrailSenseShared

final class BacktrackLiveActivity: IosBacktrackActivity {
    func start() {
        guard ActivityAuthorizationInfo().areActivitiesEnabled,
              Activity<BacktrackAttributes>.activities.isEmpty else { return }
        do {
            _ = try Activity.request(
                attributes: BacktrackAttributes(startedAt: Date()),
                content: ActivityContent(state: .init(), staleDate: nil),
                pushType: nil
            )
        } catch {
            NSLog("Trail Sense could not start the Backtrack Live Activity: %@", error.localizedDescription)
        }
    }

    func end() {
        let active = Activity<BacktrackAttributes>.activities
        Task {
            for activity in active {
                await activity.end(nil, dismissalPolicy: .immediate)
            }
        }
    }
}
