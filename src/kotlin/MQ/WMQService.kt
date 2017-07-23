package MQ

import java.util.concurrent.ConcurrentHashMap

object WMQService {

    /*
    * It seems like WMQ can't operate multiple QMs instances normally. It causes some errors.
    * So we have to reuse connection managers and add queues to existing QMs, if it's possible.
    * */
    val managers = ConcurrentHashMap<Int, WMQQManager>()

    private fun getManager(req: WMQOpenRequest): WMQQManager {
        val id = WMQQManager.getManagerID(req)
        var manager = managers.get(id)
        if (manager == null) {
            manager = WMQQManager(req)
            managers.put(id, manager)
        }
        return manager
    }

    @Throws(Exception::class)
    fun readMessage(req: WMQOpenRequest): ByteArray {
        return getManager(req).readMessage(req)
    }

    @Throws(Exception::class)
    fun writeMessage(req: WMQOpenRequest, data: ByteArray) {
        return getManager(req).writeMessage(req, data)
    }
}