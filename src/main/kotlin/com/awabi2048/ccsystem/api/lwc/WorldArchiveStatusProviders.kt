package com.awabi2048.ccsystem.api.lwc

import java.util.concurrent.CopyOnWriteArrayList

/** 登録型のワールドアーカイブ状態プロバイダ公開API。 */
object WorldArchiveStatusProviders {
    private val providers = CopyOnWriteArrayList<WorldArchiveStatusProvider>()

    @JvmStatic
    fun register(provider: WorldArchiveStatusProvider) {
        providers.removeIf { it.getId() == provider.getId() }
        providers.add(provider)
    }

    @JvmStatic
    fun unregister(provider: WorldArchiveStatusProvider) {
        providers.removeIf { it === provider || it.getId() == provider.getId() }
    }

    @JvmStatic
    fun getProviders(): List<WorldArchiveStatusProvider> = providers.toList()

    @JvmStatic
    fun isArchived(worldName: String): Boolean = providers.any { it.isArchived(worldName) }
}
