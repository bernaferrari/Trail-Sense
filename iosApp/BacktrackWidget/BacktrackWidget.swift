import ActivityKit
import SwiftUI
import WidgetKit

@main
struct BacktrackWidget: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: BacktrackAttributes.self) { context in
            HStack(spacing: 12) {
                Image(systemName: "location.fill")
                    .foregroundStyle(.green)
                    .accessibilityHidden(true)
                VStack(alignment: .leading) {
                    Text("Backtrack").font(.headline)
                    Text("Recording your route").font(.subheadline)
                }
                Spacer()
                Text(context.attributes.startedAt, style: .timer)
                    .monospacedDigit()
                    .multilineTextAlignment(.trailing)
            }
            .padding()
            .widgetURL(URL(string: "trailsense://backtrack"))
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Label("Backtrack", systemImage: "location.fill")
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text(context.attributes.startedAt, style: .timer).monospacedDigit()
                }
            } compactLeading: {
                Image(systemName: "location.fill")
            } compactTrailing: {
                Text(context.attributes.startedAt, style: .timer).monospacedDigit()
            } minimal: {
                Image(systemName: "location.fill")
            }
            .widgetURL(URL(string: "trailsense://backtrack"))
        }
    }
}
