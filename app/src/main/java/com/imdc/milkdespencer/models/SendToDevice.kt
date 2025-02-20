package com.imdc.milkdespencer.models

class SendToDevice {
    @JvmField
    var weight = 0f
    @JvmField
    var settemperature = 0f
    @JvmField
    var curtemperature = 0f
    var lowweight = 0f
    var highweight = 0f
    var isStatus = false
    var isCalib = false

    /// Added new parameter on 31-12-2024
    var isCIP = false
}
