package com.slmmzd

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.JsonArray
import io.vertx.kotlin.core.net.NetClientOptions
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class PortChecker(val config:Config,vertx: Vertx) :PortStatusRepos{
    val logger = io.vertx.core.logging.LoggerFactory.getLogger(javaClass)
    private val portStatus = ConcurrentHashMap<Addr,PortStatus>()
    private val netClient =  vertx.createNetClient(NetClientOptions(connectTimeout = 5_000,logActivity = true))
    private val connCount = AtomicInteger()


    fun check(config: JsonObject) {
        logger.info("remain conn count ${connCount.get()}")
        config.getJsonArray("targets", JsonArray()).forEach {
            val o = it as JsonObject
            check(o.getString("name"),o.getString("addr"))
        }

    }

    fun check(name:String,addr:String) {
        val adr = addr.split(":")
        val ps = this.portStatus.computeIfAbsent(Addr(adr[0], adr[1].toInt()), { PortStatus(name, it) })
        check(ps)
    }
    private fun check(portStatus: PortStatus) {
        if(portStatus.checking)
            return
        logger.info("check ${portStatus.addr}")
        val netSocket = portStatus.netSocket
        if(netSocket == null){
            portStatus.checking()
            val addr = portStatus.addr
            this.netClient.connect(addr.port,addr.host,{
                if(it.succeeded()){
                    connCount.incrementAndGet()
                    val conn = it.result()
                    portStatus.ok()
                    portStatus.netSocket = conn

                    conn.closeHandler({
                        connCount.decrementAndGet()
                        portStatus.netSocket = null

                    })
                    conn.exceptionHandler {
                        connCount.decrementAndGet()
                        portStatus.netSocket = null

                    }
                }else{
                    logger.info(it.cause())
                    failPortStatus(portStatus)
                }
            })
        }
    }

    override fun getPortStatus(): List<PortStatus> {
        val m = mutableListOf<PortStatus>()

        m.addAll(this.portStatus.values)
        return m
    }

    private fun failPortStatus(portStatus: PortStatus) {
        portStatus.fail()
        portStatus.netSocket = null
    }

}