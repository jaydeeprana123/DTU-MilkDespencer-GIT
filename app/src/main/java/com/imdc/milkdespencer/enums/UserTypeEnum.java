package com.imdc.milkdespencer.enums;

public enum UserTypeEnum {
    ADMIN(0),

    END_USER(1),

    CUSTOMER_ADMIN(2);

    private final int userTypeValue;

    // Constructor
    UserTypeEnum(int userTypeValue) {
        this.userTypeValue = userTypeValue;
    }

    // Getter method
    public int value() {
        return userTypeValue;
    }
}
