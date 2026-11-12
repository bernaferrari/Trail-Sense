package com.kylecorry.trail_sense.tools.beacons.domain

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.icons.trail.*

/** The artwork drawn for this beacon icon. */
val BeaconIcon.icon: ImageVector
    get() = when (this) {
        BeaconIcon.Trail -> TrailIcons.Trail
        BeaconIcon.Climbing -> TrailIcons.Climbing
        BeaconIcon.SkiLift -> TrailIcons.SkiLift
        BeaconIcon.Fishing -> TrailIcons.Fishing
        BeaconIcon.View -> TrailIcons.Binoculars
        BeaconIcon.TrailFork -> TrailIcons.TrailFork
        BeaconIcon.Fire -> TrailIcons.CategoryFire
        BeaconIcon.Hunting -> TrailIcons.Deer
        BeaconIcon.Mountain -> TrailIcons.Altitude
        BeaconIcon.Triangle -> TrailIcons.Triangle
        BeaconIcon.Volcano -> TrailIcons.Volcano
        BeaconIcon.Cave -> TrailIcons.Cave
        BeaconIcon.BodyOfWater -> TrailIcons.TideTable
        BeaconIcon.River -> TrailIcons.River
        BeaconIcon.Waterfall -> TrailIcons.Waterfall
        BeaconIcon.Beach -> TrailIcons.Beach
        BeaconIcon.TideHigh -> TrailIcons.TideHigh
        BeaconIcon.TideLow -> TrailIcons.TideLow
        BeaconIcon.TideHalf -> TrailIcons.TideHalf
        BeaconIcon.Tree -> TrailIcons.Tree
        BeaconIcon.Field -> TrailIcons.Grass
        BeaconIcon.Swamp -> TrailIcons.Swamp
        BeaconIcon.Plant -> TrailIcons.CategoryNatural
        BeaconIcon.Track -> TrailIcons.Paw
        BeaconIcon.Food -> TrailIcons.CategoryFood
        BeaconIcon.Picnic -> TrailIcons.Picnic
        BeaconIcon.Bench -> TrailIcons.Bench
        BeaconIcon.WaterRefill -> TrailIcons.CategoryWater
        BeaconIcon.Restroom -> TrailIcons.Restrooms
        BeaconIcon.Phone -> TrailIcons.Phone
        BeaconIcon.Trash -> TrailIcons.Delete
        BeaconIcon.CellSignal -> TrailIcons.SignalCellular3
        BeaconIcon.CellTower -> TrailIcons.CellTower
        BeaconIcon.WiFi -> TrailIcons.Wifi
        BeaconIcon.FirstAid -> TrailIcons.CategoryMedical
        BeaconIcon.Power -> TrailIcons.TorchOn
        BeaconIcon.Music -> TrailIcons.Music
        BeaconIcon.Tent -> TrailIcons.CategoryShelter
        BeaconIcon.Shelter -> TrailIcons.Shelter
        BeaconIcon.House -> TrailIcons.House
        BeaconIcon.Building -> TrailIcons.Building
        BeaconIcon.Cabin -> TrailIcons.Cabin
        BeaconIcon.Barn -> TrailIcons.Barn
        BeaconIcon.Historic -> TrailIcons.Ruins
        BeaconIcon.Cemetery -> TrailIcons.Grave
        BeaconIcon.FireTower -> TrailIcons.FireTower
        BeaconIcon.Bridge -> TrailIcons.Bridge
        BeaconIcon.Lighthouse -> TrailIcons.Lighthouse
        BeaconIcon.VisitorCenter -> TrailIcons.VisitorCenter
        BeaconIcon.RangerStation -> TrailIcons.RangerStation
        BeaconIcon.Animal -> TrailIcons.Paw
        BeaconIcon.Bird -> TrailIcons.Bird
        BeaconIcon.Mammal -> TrailIcons.Deer
        BeaconIcon.Reptile -> TrailIcons.Lizard
        BeaconIcon.Amphibian -> TrailIcons.Frog
        BeaconIcon.Fish -> TrailIcons.Fish
        BeaconIcon.Invertebrate -> TrailIcons.Ant
        BeaconIcon.Fungus -> TrailIcons.Mushroom
        BeaconIcon.Rock -> TrailIcons.Gem
        BeaconIcon.Weather -> TrailIcons.Cloud
        BeaconIcon.Road -> TrailIcons.Road
        BeaconIcon.Car -> TrailIcons.Car
        BeaconIcon.Boat -> TrailIcons.Boat
        BeaconIcon.Bike -> TrailIcons.Bike
        BeaconIcon.Alert -> TrailIcons.AlertSimple
        BeaconIcon.Flag -> TrailIcons.Flag
        BeaconIcon.Information -> TrailIcons.HelpSimple
        BeaconIcon.Map -> TrailIcons.Maps
        BeaconIcon.Sign -> TrailIcons.Sign
        BeaconIcon.Arrow -> TrailIcons.Arrow
        BeaconIcon.TrailCam -> TrailIcons.Trailcam
        BeaconIcon.Trap -> TrailIcons.Trap
    }
