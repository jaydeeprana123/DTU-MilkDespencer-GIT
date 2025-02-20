package com.imdc.milkdespencer.enums

enum class UserTypeEnum // Constructor
    (private val userTypeValue: Int) {
    ADMIN(0),
    END_USER(1),
    CUSTOMER_ADMIN(2);

    // Getter method
    fun value(): Int {
        return userTypeValue
    }
}
