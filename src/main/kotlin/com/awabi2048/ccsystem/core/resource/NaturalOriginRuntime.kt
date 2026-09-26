package com.awabi2048.ccsystem.core.resource

import com.awabi2048.ccsystem.CCSystem
import org.bukkit.Bukkit
import java.io.File

object NaturalOriginRuntime {
    lateinit var registry: NaturalOriginRegistryImpl
        private set

    fun initialize(dataFolder: File) {
        registry = NaturalOriginRegistryImpl(
            File(dataFolder, "data/natural_origin_registry.properties").toPath(),
            ResourceWorldLifecycleRuntime.service
        )
        // 生成チャンク記録は件数が多いため、変更のあったときだけ非同期で1秒毎に書き込む。
        Bukkit.getScheduler().runTaskTimerAsynchronously(
            CCSystem.instance,
            Runnable { registry.flush() },
            20L,
            20L,
        )
    }

    fun shutdown() {
        if (::registry.isInitialized) registry.flush()
    }
}
