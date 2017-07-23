package MQ

data class WMQOpenRequest (
        val host: String,
        val port: Int,
        val manager: String,
        var channel: String,
        val queue: String,
        val type: String, //read, write, delete
        val message: MessageRequest
)
