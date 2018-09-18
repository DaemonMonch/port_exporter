package com.slmmzd

import io.vertx.config.ConfigChange
import io.vertx.config.ConfigRetriever
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.config.ConfigRetrieverOptions
import io.vertx.kotlin.config.ConfigStoreOptions
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

class Config(val configPath:String,vertx:Vertx) {
    private val fileStore = ConfigStoreOptions(
            type = "file",
            format = "yaml",
            config = json {
                obj("path" to configPath)
            })

    private val retriever = ConfigRetriever.create(vertx, ConfigRetrieverOptions( stores = listOf(fileStore),scanPeriod = 10_000))

    fun configInitialed(handler: (AsyncResult<JsonObject>) -> Unit){
        this.retriever.getConfig(handler)

    }

    fun configChanged(handler:(ConfigChange) -> Unit){
        this.retriever.listen(handler)
    }


}