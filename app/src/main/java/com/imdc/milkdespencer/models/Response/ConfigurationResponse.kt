package com.imdc.milkdespencer.models.Response

import com.google.gson.annotations.SerializedName

class ConfigurationResponse {
    // Getters and Setters
    @SerializedName("Data")
    var data: List<Datum> = ArrayList()

    @SerializedName("JsonRequestBehavior")
    var jsonRequestBehavior: Long = 0
}
