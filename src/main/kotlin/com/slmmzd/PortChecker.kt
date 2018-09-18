package com.slmmzd

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.JsonArray
import io.vertx.kotlin.core.net.NetClientOptions
import java.util.concurrent.ConcurrentHashMap

class PortChecker(val config:Config,vertx: Vertx) :PortStatusRepos{
    private val portStatus = ConcurrentHashMap<Addr,PortStatus>()
    private val netClient =  vertx.createNetClient(NetClientOptions(connectTimeout = 5_000,logActivity = true))

    fun check(config: JsonObject) {
        config.getJsonArray("targets", JsonArray()).forEach {
            val o = it as JsonObject
            check(o.getString("name"),o.getString("addr"))
        }
    }

    fun check(name:String,addr:String) {
        val adr = addr.split(":")
        val ps = this.portStatus.computeIfAbsent(Addr(adr[0], adr[1].toInt()), { PortStatus(name, it) })
        if(ps.checking())
            return
        check(ps)
    }
    private fun check(portStatus: PortStatus) {
        val netSocket = portStatus.netSocket
        if(netSocket == null){
            portStatus.connecting()
            val addr = portStatus.addr
            this.netClient.connect(addr.port,addr.host,{
                if(it.succeeded()){
                    val conn = it.result()
                    portStatus.ok()
                    conn.closeHandler({
                        failPortStatus(portStatus)
                    })
                    conn.exceptionHandler { failPortStatus(portStatus) }
                }else{
                    println(it.cause())
                    portStatus.fail()
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