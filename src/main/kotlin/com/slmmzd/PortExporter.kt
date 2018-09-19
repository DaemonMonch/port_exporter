package com.slmmzd

import com.slmmzd.config.SignalReloadConfig
import io.vertx.core.Vertx
import io.vertx.kotlin.core.VertxOptions
import io.prometheus.client.Gauge
import io.prometheus.client.hotspot.DefaultExports
import io.prometheus.client.vertx.MetricsHandler
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router

val logger = io.vertx.core.logging.LoggerFactory.getLogger("portExportKt")
fun main(args: Array<String>) {
    val configPath = parseConfigPath(args)
    if(configPath == null){
        return
    }
    val vertx = Vertx.vertx(VertxOptions(eventLoopPoolSize = 1,internalBlockingPoolSize = 2))
    val config = SignalReloadConfig(configPath, vertx)
    val checker = PortChecker(config,vertx)
    DefaultExports.initialize();

    config.configInitialed {
        if(it.succeeded()){
            val c = it.result()
            println(c)
            check(checker, c)
            setupTask(vertx, checker, c)


            createMetricsEndPoint(vertx,c.getString("host","0.0.0.0"),c.getInteger("port",9333))

        }else{
            it.cause().printStackTrace()
            vertx.close()
        }
    }

    config.configChanged {
        check(checker, it)
        setupTask(vertx, checker, it)
    }

}

@Volatile var lastTimerId:Long = 0

private fun setupTask(vertx: Vertx, checker: PortChecker, config: JsonObject) {
    val checkInterval = config.getLong("checkInterval", 10_000)
    vertx.cancelTimer(lastTimerId)
    lastTimerId = vertx.setPeriodic(checkInterval, {
        check(checker, config)
    })
}

private fun check(checker: PortChecker, config: JsonObject) {
    checker.check(config)
    writeToRegistry(checker)
}

fun createMetricsEndPoint(vertx: Vertx,host:String,port:Int) {
    var server = vertx.createHttpServer()

    var router = Router.router(vertx)

    router.route("/metrics").handler(MetricsHandler())
    logger.info("exporter url http://${host}:$port/metrics ")
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
        logger.info("need config file path -c")
        return null
    }

    return args[1]
}
