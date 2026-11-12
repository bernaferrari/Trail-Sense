package com.kylecorry.trail_sense.tools.beacons.domain

import com.kylecorry.trail_sense.shared.data.Identifiable

// Last ID: 72
/** A symbol a beacon can wear. Persisted by [id]; the artwork for each lives with the UI (BeaconIcon.icon). */
enum class BeaconIcon(
    override val id: Long,
    val isUserSelectable: Boolean = true
) : Identifiable {
    // Activities
    Trail(26),
    Climbing(30),
    SkiLift(9),
    Fishing(11),
    View(25),
    TrailFork(28),
    Fire(29),
    Hunting(34),

    // Natural features
    Mountain(1),
    Triangle(69),
    Volcano(65),
    Cave(71),
    BodyOfWater(17),
    River(18),
    Waterfall(20),
    Beach(19),
    TideHigh(48, isUserSelectable = false),
    TideLow(49, isUserSelectable = false),
    TideHalf(50, isUserSelectable = false),
    Tree(21),
    Field(37),
    Swamp(66),
    Plant(38),
    Track(62),

    // Amenities
    Food(12),
    Picnic(36),
    Bench(67),
    WaterRefill(6),
    Restroom(7),
    Phone(8),
    Trash(16),
    CellSignal(22),
    CellTower(47),
    WiFi(23),
    FirstAid(24),
    Power(32),
    Music(46),

    // Buildings
    Tent(5),
    Shelter(70),
    House(10),
    Building(39),
    Cabin(31),
    Barn(40),
    Historic(33),
    Cemetery(41),
    FireTower(42),
    Bridge(43),
    Lighthouse(44),
    VisitorCenter(45),
    RangerStation(68),

    // Animals and Field Guide Tags
    Animal(51, isUserSelectable = false),
    Bird(52, isUserSelectable = false),
    Mammal(53, isUserSelectable = false),
    Reptile(54, isUserSelectable = false),
    Amphibian(55, isUserSelectable = false),
    Fish(56, isUserSelectable = false),
    Invertebrate(57, isUserSelectable = false),
    Fungus(58, isUserSelectable = false),
    Rock(59, isUserSelectable = false),
    Weather(60, isUserSelectable = false),

    // Vehicles
    Road(27),
    Car(2),
    Boat(3),
    Bike(4),


    // Other
    Alert(13),
    Flag(72),
    Information(14),
    Map(15),
    Sign(35),
    Arrow(61, isUserSelectable = false),
    TrailCam(63),
    Trap(64)
}
