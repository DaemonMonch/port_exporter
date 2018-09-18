package com.slmmzd

import io.vertx.core.net.NetSocket

data class Addr(val host:String,val port:Int)

class PortStatus(val name:String,val addr: Addr,var netSocket: NetSocket? = null) {
    @Volatile private var status:Short = 0
    @Volatile  var checking = false

    fun ok() {
        this.status = 1
        this.checking = false
    }

    fun fail() {
        this.status = 0
        this.checking = false
    }

    fun checking() {
        this.checking = true
    }

    fun isOk() = this.status == 1.toShort()
 /*   override fun toString(): String {
        return "PortStatus(name='$name', addr=$addr, netSocket=$netSocket, status=$status)"
    }*/


}
