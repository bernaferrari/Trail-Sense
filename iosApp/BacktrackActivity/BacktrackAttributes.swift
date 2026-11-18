import ActivityKit
import Foundation

struct BacktrackAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {}
    let startedAt: Date
}
