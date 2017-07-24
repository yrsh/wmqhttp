package main

import MQ.WMQOpenRequest
import MQ.WMQService
import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler

class HttpDispatcher(val port: Int): AbstractVerticle() {

    companion object {
        object constants {
            val wmpParamsKey = "wmqp"
        }
    }

    override fun start() {
        val server = vertx.createHttpServer()
        val router = Router.router(vertx)
        router.get("/*").handler(this::headerCheckHandler)
        router.post("/*").handler(this::headerCheckHandler)
        router.get("/wmq").blockingHandler(this::readHandler)
        router.post("/wmq").handler(BodyHandler.create())
        router.post("/wmq").blockingHandler(this::writeHandler)
        server.requestHandler(router::accept)
        server.listen(port)
    }

    private fun headerCheckHandler(ctx: RoutingContext) {
        val res = ctx.response()
        try {
            val params = ctx.request().headers().get("wmq-params")
            val wmqp = Helpers.parseJSON(params, WMQOpenRequest::class.java)
            ctx.put(constants.wmpParamsKey, wmqp)
            ctx.next()
        } catch (ex: Exception) {
            res.setStatusCode(400).end(ex.toString())
        }
    }

    private fun writeHandler(ctx: RoutingContext) {
        val res = ctx.response()
        val wmqp = ctx.get<WMQOpenRequest>(constants.wmpParamsKey)
        try {
            WMQService.writeMessage(wmqp, ctx.body.bytes)
            res.setStatusCode(200).end()
        } catch (ex: Exception) {
            res.setStatusCode(400).end(ex.toString())
        }
    }

    private fun readHandler(ctx: RoutingContext) {
        val res = ctx.response()
        val wmqp = ctx.get<WMQOpenRequest>(constants.wmpParamsKey)
        try {
            val data = WMQService.readMessage(wmqp)
            res.setStatusCode(200).end(Buffer.buffer(data))
        } catch (ex: Exception) {
            res.setStatusCode(400).end(ex.toString())
        }
    }


}

