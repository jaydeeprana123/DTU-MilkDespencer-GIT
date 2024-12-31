package com.imdc.milkdespencer.models;

public class MilkSellingPrice {

    private String description;
    private String unit;
    private double price;

    public MilkSellingPrice(String description, String unit, double price) {
        this.description = description;
        this.unit = unit;
        this.price = price;
    }

    // Getters and setters

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "MilkSellingPrice{" +
                "description='" + description + '\'' +
                ", unit='" + unit + '\'' +
                ", price=" + price +
                '}';
    }
}
