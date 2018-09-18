package com.slmmzd

import io.vertx.core.Vertx
import io.vertx.kotlin.core.VertxOptions
import io.prometheus.client.Gauge
import io.prometheus.client.hotspot.DefaultExports
import io.prometheus.client.vertx.MetricsHandler
import io.vertx.ext.web.Router


fun main(args: Array<String>) {
    val configPath = parseConfigPath(args)
    if(configPath == null){
        return
    }
    val vertx = Vertx.vertx(VertxOptions(eventLoopPoolSize = 1,internalBlockingPoolSize = 2))
    val config = Config(configPath,vertx)
    val checker = PortChecker(config,vertx)
    DefaultExports.initialize();

    config.configInitialed {
        if(it.succeeded()){
            val c = it.result()
            println(c)
            checker.check(c)
            writeToRegistry(checker)

            val checkInterval = c.getLong("checkInterval",10_000)
            vertx.setPeriodic(checkInterval,{
                checker.check(c)
                writeToRegistry(checker)
            })


            createMetricsEndPoint(vertx,c.getString("host","0.0.0.0"),c.getInteger("port",9333))

        }else{
            it.cause().printStackTrace()
            vertx.close()
        }
    }

}

fun createMetricsEndPoint(vertx: Vertx,host:String,port:Int) {
    var server = vertx.createHttpServer()

    var router = Router.router(vertx)

    router.route("/metrics").handler(MetricsHandler())
    server.requestHandler({router.accept(it)}).listen(port,host)
}

val gauge = Gauge.build()
        .name("port_status").labelNames("name","port").help("status of ports").register()

fun writeToRegistry(portStatusRepos: PortStatusRepos) {
    val portStatus = portStatusRepos.getPortStatus()
    portStatus.forEach {

        val metric = gauge.labels(it.name, it.addr.port.toString())
        if(it.isOk())
            metric.set(1.0)
        else
            metric.set(0.0)
    }
}

fun parseConfigPath(args: Array<String>): String? {
    if (args.size < 2 || args[0] != "-c") {
        println("need config file path -c")
        return null
    }

    return args[1]
}
