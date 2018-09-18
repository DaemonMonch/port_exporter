package com.slmmzd

import io.vertx.core.net.NetSocket

data class Addr(val host:String,val port:Int)

class PortStatus(val name:String,val addr: Addr,var netSocket: NetSocket? = null) {
    @Volatile private var status:Short = 0

    fun ok() {
        this.status = 1
    }

    fun fail() {
        this.status = 0
    }

    fun connecting() {
        this.status = 3
    }

    fun checking() = this.status == 3.toShort()
    fun isOk() = this.status == 1.toShort()
 /*   override fun toString(): String {
        return "PortStatus(name='$name', addr=$addr, netSocket=$netSocket, status=$status)"
    }*/


}
