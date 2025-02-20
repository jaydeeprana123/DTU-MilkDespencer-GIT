package com.imdc.milkdespencer.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

///{"currentweight":0.699044347,"setweight":3,"status":false}
class ResponseMilkDispense {
    @JvmField
    @SerializedName("currentweight")
    @Expose
    var currentWeight: Double? = null

    @JvmField
    @SerializedName("setweight")
    @Expose
    var setWeight: Double? = null

    @JvmField
    @SerializedName("status")
    @Expose
    var status: Boolean? = null
}
