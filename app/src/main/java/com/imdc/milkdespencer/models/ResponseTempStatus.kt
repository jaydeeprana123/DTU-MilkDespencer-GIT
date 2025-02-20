package com.imdc.milkdespencer.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class ResponseTempStatus {
    @JvmField
    @SerializedName("temperature")
    @Expose
    var temperature: Double? = null

    @JvmField
    @SerializedName("compressor")
    @Expose
    var compressor: Boolean? = null

    @JvmField
    @SerializedName("agitator")
    @Expose
    var agitator: Boolean? = null

    @JvmField
    @SerializedName("lowlevel")
    @Expose
    var lowlevel: Boolean? = null

    @JvmField
    @SerializedName("connectivity")
    @Expose
    var connectivity: Boolean? = null
}