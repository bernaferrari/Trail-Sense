package com.kylecorry.trail_sense.tools.beacons.domain

/**
 * The outline a beacon wears everywhere it appears: its row in the list, the destination card while navigating to it,
 * and its waypoint on the compass rim. [id] is what is stored; Cookie is the default (and what every beacon saved
 * before shapes existed wears).
 */
enum class BeaconShape(val id: Int) {
    Cookie(0),
    Circle(1),
    Square(2),
    Clover(3),
    Sunny(4),
    Flower(5),
    Gem(6),
    Puffy(7);

    companion object {
        fun from(id: Int?): BeaconShape = entries.firstOrNull { it.id == id } ?: Cookie
    }
}
