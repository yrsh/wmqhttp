package MQ

data class MessageRequest(
        val wait: Boolean,
        val cursor: String?
)
