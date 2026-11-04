package com.kylecorry.trail_sense.tools.beacons.domain

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.data.Identifiable

// Last ID: 72
enum class BeaconIcon(
    override val id: Long,
    val icon: ImageVector,
    val isUserSelectable: Boolean = true
) : Identifiable {
    // Activities
    Trail(26, TrailIcons.Trail),
    Climbing(30, TrailIcons.Climbing),
    SkiLift(9, TrailIcons.SkiLift),
    Fishing(11, TrailIcons.Fishing),
    View(25, TrailIcons.Binoculars),
    TrailFork(28, TrailIcons.TrailFork),
    Fire(29, TrailIcons.CategoryFire),
    Hunting(34, TrailIcons.Deer),

    // Natural features
    Mountain(1, TrailIcons.Altitude),
    Triangle(69, TrailIcons.Triangle),
    Volcano(65, TrailIcons.Volcano),
    Cave(71, TrailIcons.Cave),
    BodyOfWater(17, TrailIcons.TideTable),
    River(18, TrailIcons.River),
    Waterfall(20, TrailIcons.Waterfall),
    Beach(19, TrailIcons.Beach),
    TideHigh(48, TrailIcons.TideHigh, isUserSelectable = false),
    TideLow(49, TrailIcons.TideLow, isUserSelectable = false),
    TideHalf(50, TrailIcons.TideHalf, isUserSelectable = false),
    Tree(21, TrailIcons.Tree),
    Field(37, TrailIcons.Grass),
    Swamp(66, TrailIcons.Swamp),
    Plant(38, TrailIcons.CategoryNatural),
    Track(62, TrailIcons.Paw),

    // Amenities
    Food(12, TrailIcons.CategoryFood),
    Picnic(36, TrailIcons.Picnic),
    Bench(67, TrailIcons.Bench),
    WaterRefill(6, TrailIcons.CategoryWater),
    Restroom(7, TrailIcons.Restrooms),
    Phone(8, TrailIcons.Phone),
    Trash(16, TrailIcons.Delete),
    CellSignal(22, TrailIcons.SignalCellular3),
    CellTower(47, TrailIcons.CellTower),
    WiFi(23, TrailIcons.Wifi),
    FirstAid(24, TrailIcons.CategoryMedical),
    Power(32, TrailIcons.TorchOn),
    Music(46, TrailIcons.Music),

    // Buildings
    Tent(5, TrailIcons.CategoryShelter),
    Shelter(70, TrailIcons.Shelter),
    House(10, TrailIcons.House),
    Building(39, TrailIcons.Building),
    Cabin(31, TrailIcons.Cabin),
    Barn(40, TrailIcons.Barn),
    Historic(33, TrailIcons.Ruins),
    Cemetery(41, TrailIcons.Grave),
    FireTower(42, TrailIcons.FireTower),
    Bridge(43, TrailIcons.Bridge),
    Lighthouse(44, TrailIcons.Lighthouse),
    VisitorCenter(45, TrailIcons.VisitorCenter),
    RangerStation(68, TrailIcons.RangerStation),

    // Animals and Field Guide Tags
    Animal(51, TrailIcons.Paw, isUserSelectable = false),
    Bird(52, TrailIcons.Bird, isUserSelectable = false),
    Mammal(53, TrailIcons.Deer, isUserSelectable = false),
    Reptile(54, TrailIcons.Lizard, isUserSelectable = false),
    Amphibian(55, TrailIcons.Frog, isUserSelectable = false),
    Fish(56, TrailIcons.Fish, isUserSelectable = false),
    Invertebrate(57, TrailIcons.Ant, isUserSelectable = false),
    Fungus(58, TrailIcons.Mushroom, isUserSelectable = false),
    Rock(59, TrailIcons.Gem, isUserSelectable = false),
    Weather(60, TrailIcons.Cloud, isUserSelectable = false),

    // Vehicles
    Road(27, TrailIcons.Road),
    Car(2, TrailIcons.Car),
    Boat(3, TrailIcons.Boat),
    Bike(4, TrailIcons.Bike),


    // Other
    Alert(13, TrailIcons.AlertSimple),
    Flag(72, TrailIcons.Flag),
    Information(14, TrailIcons.HelpSimple),
    Map(15, TrailIcons.Maps),
    Sign(35, TrailIcons.Sign),
    Arrow(61, TrailIcons.Arrow, isUserSelectable = false),
    TrailCam(63, TrailIcons.Trailcam),
    Trap(64, TrailIcons.Trap)
}
