package com.slmmzd.config

import io.vertx.config.ConfigRetriever
import io.vertx.config.yaml.YamlProcessor
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.config.ConfigRetrieverOptions
import io.vertx.kotlin.config.ConfigStoreOptions
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import sun.misc.Signal

class SignalReloadConfig(val configPath:String,vertx: Vertx) : Config {
    val logger = io.vertx.core.logging.LoggerFactory.getLogger(javaClass)
    private val fileStore = ConfigStoreOptions(
            type = "file",
            format = "yaml",
            config = json {
                obj("path" to configPath)
            })

    private val retriever = ConfigRetriever.create(vertx, ConfigRetrieverOptions( stores = listOf(fileStore)))
    private val handlers:MutableList<(JsonObject) -> Unit> = mutableListOf()

    init {
        Signal.handle(Signal("HUP"),{
            logger.info("SIGHUP received reload config")
            vertx.executeBlocking({f:Future<JsonObject> ->
                vertx.fileSystem().readFile(configPath,{
                    if(it.succeeded()){
                        val root = YamlProcessor.YAML_MAPPER.readTree(it.result().toString("utf-8"))
                        val json = JsonObject(root.toString())
                        f.complete(json)
                    }else{
                        f.fail(it.cause())
                    }
                })
            },{
                if(it.succeeded()){
                    val r = it.result()
                    synchronized(this.handlers,{
                        this.handlers.forEach {
                            it.invoke(r)
                        }
                    })
                }

            })
        })
    }

    override fun configInitialed(handler: (AsyncResult<JsonObject>) -> Unit) {
        this.retriever.getConfig(handler)
    }

    override fun configChanged(handler: (JsonObject) -> Unit) {
        synchronized(this.handlers,{
            this.handlers.add(handler)
        })
    }
}