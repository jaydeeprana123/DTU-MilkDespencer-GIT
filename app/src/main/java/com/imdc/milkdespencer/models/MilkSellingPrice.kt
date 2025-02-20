package com.imdc.milkdespencer.models

class MilkSellingPrice(// Getters and setters
    var description: String, var unit: String, var price: Double
) {

    override fun toString(): String {
        return "MilkSellingPrice{" +
                "description='" + description + '\'' +
                ", unit='" + unit + '\'' +
                ", price=" + price +
                '}'
    }
}
