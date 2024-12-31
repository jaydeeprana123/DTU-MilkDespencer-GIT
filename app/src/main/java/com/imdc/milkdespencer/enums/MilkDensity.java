package com.imdc.milkdespencer.enums;

public enum MilkDensity {
    CowMilk("Cow Milk", 1.0), BuffaloMilk("Buffalo Milk", 1.0);
//    CowMilk("Cow Milk", 1.025), BuffaloMilk("Buffalo Milk", 1.030);


    private final String milkType;
    private final double density; // Density in g/mL

    MilkDensity(String milkType, double density) {
        this.milkType = milkType;
        this.density = density;
    }

    public String getMilkType() {
        return milkType;
    }

    public double getDensity() {
        return density;
    }
}
