package com.imdc.milkdespencer.models.Response

import java.io.Serializable

class ResponseOTP : Serializable {
    var data: List<Data>? = null
    var sender: String? = null
    var createdDateTime: String? = null
    var source: String? = null
    var id: String? = null
    var body: String? = null
    var type: String? = null
    var totalCount: Int? = null
    var error: Any? = null

    class Data : Serializable {
        var recipient: String? = null
        var message_id: String? = null
    }
}
