package com.kylecorry.andromeda.widgets

public data class Shortcut(
    val id: String,
    val label: String,
    val longLabel: String? = null,
    val attributes: Map<String, String> = emptyMap(),
) {
    init { require(id.isNotBlank() && label.isNotBlank()) }
}

/** Requests platform pinning; a true result means the request was accepted, not that the user pinned it. */
public class ShortcutPinning(
    private val supported: () -> Boolean,
    private val requestPin: (Shortcut) -> Boolean,
) {
    public fun isSupported(): Boolean = supported()
    public fun request(shortcut: Shortcut): Boolean = supported() && requestPin(shortcut)
}
