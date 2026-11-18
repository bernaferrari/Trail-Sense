import AlarmKit
import Foundation
import SwiftUI
import TrailSenseShared

@available(iOS 26, *)
final class TurnBackScheduler: IosTurnBackScheduler {
    private struct Reminder {
        let id: UUID
        let date: Date
        let title: String

        init(id: UUID, milliseconds: Int64, title: String) {
            self.id = id
            self.date = Date(timeIntervalSince1970: Double(milliseconds) / 1_000)
            self.title = title
        }
    }

    private let turnBackID = UUID(uuidString: "D1638220-B8A2-4120-8F52-29B7B007CD01")!
    private let returnID = UUID(uuidString: "D1638220-B8A2-4120-8F52-29B7B007CD02")!
    private var pending: Task<Void, Never>?

    func schedule(
        turnBackEpochMillis: Int64,
        returnEpochMillis: Int64,
        turnBackTitle: String,
        returnTitle: String,
        completion: IosAlarmCompletion
    ) {
        let reminders = [
            Reminder(id: turnBackID, milliseconds: turnBackEpochMillis, title: turnBackTitle),
            Reminder(id: returnID, milliseconds: returnEpochMillis, title: returnTitle)
        ]
        let previous = pending
        previous?.cancel()
        pending = Task { @MainActor in
            await previous?.value
            do {
                try Task.checkCancellation()
                cancelReminders()
                guard try await AlarmManager.shared.requestAuthorization() == .authorized else {
                    completion.complete(scheduled: false)
                    return
                }
                for reminder in reminders {
                    try Task.checkCancellation()
                    try await scheduleAlarm(reminder)
                }
                try Task.checkCancellation()
                completion.complete(scheduled: true)
            } catch {
                cancelReminders()
                completion.complete(scheduled: false)
            }
        }
    }

    func cancel() {
        pending?.cancel()
        cancelReminders()
    }

    private func cancelReminders() {
        try? AlarmManager.shared.cancel(id: turnBackID)
        try? AlarmManager.shared.cancel(id: returnID)
    }

    private struct Metadata: AlarmMetadata {}

    private func scheduleAlarm(_ reminder: Reminder) async throws {
        let date = max(reminder.date, Date().addingTimeInterval(1))
        let attributes = AlarmAttributes(
            presentation: AlarmPresentation(alert: .init(
                title: LocalizedStringResource(stringLiteral: reminder.title),
                stopButton: .init(text: "Stop", textColor: .white, systemImageName: "stop.fill")
            )),
            metadata: Metadata(),
            tintColor: .orange
        )
        _ = try await AlarmManager.shared.schedule(
            id: reminder.id,
            configuration: .alarm(schedule: .fixed(date), attributes: attributes)
        )
    }
}
