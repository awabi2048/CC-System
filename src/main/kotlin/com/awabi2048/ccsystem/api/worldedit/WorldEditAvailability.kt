package com.awabi2048.ccsystem.api.worldedit

import org.bukkit.Bukkit

/**
 * WorldEdit系プラグインの有効判定です。
 *
 * WorldEdit APIへ触れないため、WorldEdit不在時も安全に呼び出せます。
 * WorldEdit型に触る [WorldEditSelectionSupport] へ触る前に、
 * この判定でガードしてください。
 */
object WorldEditAvailability {
    /** WorldEditまたはFastAsyncWorldEditが有効かを返します。 */
    fun isAvailable(): Boolean {
        val manager = Bukkit.getPluginManager()
        return manager.isPluginEnabled("WorldEdit") || manager.isPluginEnabled("FastAsyncWorldEdit")
    }
}
