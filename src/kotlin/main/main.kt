package main

import io.vertx.core.Vertx
import io.vertx.core.VertxOptions

fun main(args: Array<String>) {
    val vo = VertxOptions()
    vo.setMaxWorkerExecuteTime(Long.MAX_VALUE)
    val vertx = Vertx.vertx(vo)
    val hd = HttpDispatcher(getPort(args))
    vertx.deployVerticle(hd)
}

fun getPort(args: Array<String>): Int {
    val port: Int = if (args.isNotEmpty()) args[0].toInt() else 3000
    println("Running on port: $port")
    return port
}