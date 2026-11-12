package com.kylecorry.trail_sense.tools.field_guide.ui

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag

class FieldGuideTagNameMapper {

    fun getName(tag: FieldGuidePageTag): String {
        return when (tag) {
            FieldGuidePageTag.Africa -> getAppService<AppStrings>().get(Res.string.africa)
            FieldGuidePageTag.Antarctica -> getAppService<AppStrings>().get(Res.string.antarctica)
            FieldGuidePageTag.Asia -> getAppService<AppStrings>().get(Res.string.asia)
            FieldGuidePageTag.Australia -> getAppService<AppStrings>().get(Res.string.australia)
            FieldGuidePageTag.Europe -> getAppService<AppStrings>().get(Res.string.europe)
            FieldGuidePageTag.NorthAmerica -> getAppService<AppStrings>().get(Res.string.north_america)
            FieldGuidePageTag.SouthAmerica -> getAppService<AppStrings>().get(Res.string.south_america)
            FieldGuidePageTag.Plant -> getAppService<AppStrings>().get(Res.string.plant)
            FieldGuidePageTag.Animal -> getAppService<AppStrings>().get(Res.string.animal)
            FieldGuidePageTag.Fungus -> getAppService<AppStrings>().get(Res.string.fungus)
            FieldGuidePageTag.Bird -> getAppService<AppStrings>().get(Res.string.bird)
            FieldGuidePageTag.Mammal -> getAppService<AppStrings>().get(Res.string.mammal)
            FieldGuidePageTag.Reptile -> getAppService<AppStrings>().get(Res.string.reptile)
            FieldGuidePageTag.Amphibian -> getAppService<AppStrings>().get(Res.string.amphibian)
            FieldGuidePageTag.Fish -> getAppService<AppStrings>().get(Res.string.fish)
            FieldGuidePageTag.Invertebrate -> getAppService<AppStrings>().get(Res.string.invertebrate)
            FieldGuidePageTag.Rock -> getAppService<AppStrings>().get(Res.string.rock)
            FieldGuidePageTag.Insect -> getAppService<AppStrings>().get(Res.string.insect)
            FieldGuidePageTag.Arachnid -> getAppService<AppStrings>().get(Res.string.arachnid)
            FieldGuidePageTag.Crustacean -> getAppService<AppStrings>().get(Res.string.crustacean)
            FieldGuidePageTag.Mollusk -> getAppService<AppStrings>().get(Res.string.mollusk)
            FieldGuidePageTag.Sponge -> getAppService<AppStrings>().get(Res.string.sponge)
            FieldGuidePageTag.Coral -> getAppService<AppStrings>().get(Res.string.coral)
            FieldGuidePageTag.Jellyfish -> getAppService<AppStrings>().get(Res.string.jellyfish)
            FieldGuidePageTag.Worm -> getAppService<AppStrings>().get(Res.string.worm)
            FieldGuidePageTag.Echinoderm -> getAppService<AppStrings>().get(Res.string.echinoderm)
            FieldGuidePageTag.Other -> getAppService<AppStrings>().get(Res.string.other)
            FieldGuidePageTag.Forest -> getAppService<AppStrings>().get(Res.string.forest)
            FieldGuidePageTag.Desert -> getAppService<AppStrings>().get(Res.string.desert)
            FieldGuidePageTag.Grassland -> getAppService<AppStrings>().get(Res.string.grassland)
            FieldGuidePageTag.Wetland -> getAppService<AppStrings>().get(Res.string.wetland)
            FieldGuidePageTag.Mountain -> getAppService<AppStrings>().get(Res.string.mountain)
            FieldGuidePageTag.Urban -> getAppService<AppStrings>().get(Res.string.urban)
            FieldGuidePageTag.Marine -> getAppService<AppStrings>().get(Res.string.marine)
            FieldGuidePageTag.Freshwater -> getAppService<AppStrings>().get(Res.string.freshwater)
            FieldGuidePageTag.Cave -> getAppService<AppStrings>().get(Res.string.cave)
            FieldGuidePageTag.Tundra -> getAppService<AppStrings>().get(Res.string.tundra)
            FieldGuidePageTag.Diurnal -> getAppService<AppStrings>().get(Res.string.diurnal)
            FieldGuidePageTag.Nocturnal -> getAppService<AppStrings>().get(Res.string.nocturnal)
            FieldGuidePageTag.Crepuscular -> getAppService<AppStrings>().get(Res.string.crepuscular)
            FieldGuidePageTag.Edible -> getAppService<AppStrings>().get(Res.string.edible)
            FieldGuidePageTag.Inedible -> getAppService<AppStrings>().get(Res.string.inedible)
            FieldGuidePageTag.Dangerous -> getAppService<AppStrings>().get(Res.string.dangerous)
            FieldGuidePageTag.Crafting -> getAppService<AppStrings>().get(Res.string.crafting)
            FieldGuidePageTag.Medicinal -> getAppService<AppStrings>().get(Res.string.medicinal)
            FieldGuidePageTag.Weather -> getAppService<AppStrings>().get(Res.string.weather)
        }
    }

}
