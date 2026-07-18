package com.awabi2048.ccsystem.core.resource

import java.io.File

object NaturalOriginRuntime {
    lateinit var registry: NaturalOriginRegistryImpl
        private set

    fun initialize(dataFolder: File) {
        registry = NaturalOriginRegistryImpl(
            File(dataFolder, "data/natural_origin_registry.properties").toPath(),
            ResourceWorldLifecycleRuntime.service
        )
    }
}
