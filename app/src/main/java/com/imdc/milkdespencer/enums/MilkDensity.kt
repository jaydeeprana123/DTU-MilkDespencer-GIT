package com.imdc.milkdespencer.enums

enum class MilkDensity(//    CowMilk("Cow Milk", 1.025), BuffaloMilk("Buffalo Milk", 1.030);
    val milkType: String, // Density in g/mL
    val density: Double
) {
    CowMilk("Cow Milk", 1.0),
    BuffaloMilk("Buffalo Milk", 1.0)

}
