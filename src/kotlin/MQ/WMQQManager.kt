package MQ

import com.ibm.mq.MQGetMessageOptions
import com.ibm.mq.MQMessage
import com.ibm.mq.MQQueue
import com.ibm.mq.MQQueueManager
import com.ibm.mq.constants.CMQC
import java.util.Hashtable
import java.util.concurrent.ConcurrentHashMap

class WMQQManager(req: WMQOpenRequest) {

    companion object {
        object constants {
            val DEFAULT_OPTIONS = CMQC.MQOO_BROWSE
            val REQ_READ = "rd"
            val REQ_WRITE = "wr"
            object errors {
                val QUEUE_CREATION_ERROR = "Failed to create queue"
                val QUEUE_ACCESS_ERROR = "Failed to access queue"
                val UNKNOWN_ERR = "Something went wrong"
            }
        }

        /*
        * If you read a queue by cursor, you need unique queue instance,
        * because a cursor position is the property of the queue and you will not be able
        * to reuse the existing queue from another user correctly
        * */
        fun getQueueID(req: WMQOpenRequest): Int {
            var prefix = ""
            if (req.type.equals(constants.REQ_READ)) {
                prefix = constants.REQ_READ
                if (req.message.cursor != null && req.message.cursor.isNotEmpty()) {
                    prefix += req.message.cursor
                }
            } else if (req.type.equals(constants.REQ_WRITE)) {
                prefix = constants.REQ_WRITE
            }
            val id = "${prefix}_${req.channel}_${req.queue}_${req.type}".hashCode()
            return id
        }

        fun getManagerID(req: WMQOpenRequest): Int {
            return "${req.host}_${req.port}_${req.manager}".hashCode()
        }

        fun getMessageID(req: WMQOpenRequest): Int {
            return 0
        }

        fun getGMOID(req: WMQOpenRequest): Int {
            return "${req.type}_${req.message.wait}_${req.message.cursor}".hashCode()
        }

        private fun getConnectionProps(req: WMQOpenRequest): Hashtable<*, *> {
            val props = Hashtable<String, Any>()
            props.put(CMQC.HOST_NAME_PROPERTY, req.host)
            props.put(CMQC.PORT_PROPERTY, req.port)
            props.put(CMQC.CHANNEL_PROPERTY, req.channel)
            return props
        }

        private fun setupGMO(gmo: MQGetMessageOptions, wait: Boolean, opt: Int) {
            gmo.matchOptions = CMQC.MQMO_NONE
            gmo.options = (if (wait) CMQC.MQGMO_WAIT else CMQC.MQGMO_NO_WAIT) or opt
            if (wait) {
                gmo.waitInterval = CMQC.MQWI_UNLIMITED
            }
        }
    }

    private val queues = ConcurrentHashMap<Int, MQQueue>()
    private val options = ConcurrentHashMap<Int, MQGetMessageOptions>()
    private var qMgr: MQQueueManager? = null

    init {
        qMgr = MQQueueManager(req.manager, getConnectionProps(req))
    }

    @Throws(Exception::class)
    private fun getQueue(req: WMQOpenRequest): MQQueue {
        val qid = getQueueID(req)
        if (!queues.containsKey(qid)) {
            val error = connect(req)
            if (error != null) {
                throw error
            }
        }
        val queue = queues.get(qid)
        if (queue != null) {
            return queue
        } else {
            throw Exception(constants.errors.UNKNOWN_ERR)
        }
    }

    /*
    * Streams/Readers are not used, because the MQ immediately reads the entire message into RAM.
    * */
    @Throws(Exception::class)
    fun readMessage(req: WMQOpenRequest): ByteArray {
        val queue = getQueue(req)
        val message = MQMessage()
        queue.get(message, getGMO(req))
        val b = ByteArray(message.dataLength)
        message.readFully(b)
        return b
    }

    @Throws(Exception::class)
    fun writeMessage(req: WMQOpenRequest, data: ByteArray) {
        val queue = getQueue(req)
        val message = MQMessage()
        message.write(data)
        queue.put(message)
    }

    private fun getGMO(req: WMQOpenRequest): MQGetMessageOptions {
        val id = getGMOID(req)
        var gmo = options.get(id)
        if (gmo == null) {
            gmo = MQGetMessageOptions()
            if (req.type.equals(constants.REQ_WRITE) or req.type.equals(constants.REQ_READ)) {
                if (req.message.cursor != null && req.message.cursor.isNotEmpty()) {
                    setupGMO(gmo, req.message.wait, CMQC.MQGMO_BROWSE_NEXT)
                } else {
                    setupGMO(gmo, req.message.wait, CMQC.MQGMO_BROWSE_FIRST)
                }
            } else {
                gmo.matchOptions = CMQC.MQMO_NONE
                gmo.options = CMQC.MQGMO_FAIL_IF_QUIESCING
            }
            options.put(id, gmo)
        }
        return gmo
    }

    private fun connect(req: WMQOpenRequest): Exception? {
        var queue: MQQueue? = null
        try {
            if (req.type.equals(constants.REQ_READ)) {
                queue = qMgr?.accessQueue(req.queue, constants.DEFAULT_OPTIONS)
            } else if (req.type.equals(constants.REQ_WRITE)) {
                queue = qMgr?.accessQueue(req.queue, CMQC.MQOO_OUTPUT)
            } else {
                queue = qMgr?.accessQueue(req.queue, CMQC.MQOO_INPUT_SHARED or CMQC.MQOO_BROWSE)
            }
        } catch (ex: Exception) {
            return Exception(constants.errors.QUEUE_ACCESS_ERROR, ex)
        }
        if (queue != null) {
            queues.put(getQueueID(req), queue)
            return null
        } else {
            return Exception(constants.errors.QUEUE_CREATION_ERROR)
        }
    }

}
