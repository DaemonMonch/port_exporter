package com.slmmzd.config

import io.vertx.config.ConfigChange
import io.vertx.core.AsyncResult
import io.vertx.core.json.JsonObject

interface Config {
    fun configInitialed(handler: (AsyncResult<JsonObject>) -> Unit)

    fun configChanged(handler:(JsonObject) -> Unit)
}