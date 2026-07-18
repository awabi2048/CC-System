package com.awabi2048.ccsystem.core.resource

import java.io.File

object ResourceWorldLifecycleRuntime {
    lateinit var service: ResourceWorldLifecycleServiceImpl
        private set

    fun initialize(dataFolder: File, failureHandler: (String, Throwable) -> Unit) {
        service = ResourceWorldLifecycleServiceImpl(
            ResourceWorldGenerationStore(File(dataFolder, "data/resource_world_generations.properties").toPath()),
            listenerFailureHandler = failureHandler
        )
    }
}
