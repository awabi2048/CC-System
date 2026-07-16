package com.awabi2048.ccsystem.features.lwcx.service

import com.awabi2048.ccsystem.api.lwc.WorldArchiveStatusProviders
import com.awabi2048.ccsystem.features.lwcx.gateway.LwcGateway
import com.awabi2048.ccsystem.features.lwcx.gateway.LwcXGateway
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.concurrent.CompletableFuture

class LwcExpansionService(private val plugin: JavaPlugin) {
    private val snapshotStore = LwcStatusSnapshotStore(File(plugin.dataFolder, "data/lwc_expansion/status.properties"))

    fun isAvailable(): Boolean = gatewayOrNull() != null

    fun cleanupWorld(worldName: String, callback: (Int?, String?) -> Unit) {
        val gateway = gatewayOrNull()
        if (gateway == null) {
            callbackOnMain(callback, null, "unavailable")
            return
        }

        CompletableFuture.supplyAsync { gateway.loadProtectionHandles(worldName) }
            .thenAccept { handles ->
                // LWCのProtection.removeはイベント、キャッシュ、DB更新を伴うためmainで実行する。
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    var removed = 0
                    val failures = mutableListOf<String>()
                    handles.forEach { handle ->
                        try {
                            handle.remove()
                            removed++
                        } catch (error: Exception) {
                            // 全件を試行したうえで失敗を返し、部分削除を成功扱いにしない。
                            failures += "${handle.record.id}: ${error.message ?: error::class.simpleName}"
                        }
                    }
                    if (failures.isEmpty()) {
                        callback(removed, null)
                    } else {
                        callback(null, "cleanup_failed ($removed/${handles.size} removed): ${failures.joinToString(", ")}")
                    }
                })
            }
            .exceptionally { error ->
                callbackOnMain(callback, null, error.cause?.message ?: error.message ?: "cleanup_failed")
                null
            }
    }

    fun loadStatus(callback: (LwcStatusReport?, String?) -> Unit) {
        val gateway = gatewayOrNull()
        if (gateway == null) {
            callbackOnMain(callback, null, "unavailable")
            return
        }

        val existingWorlds = Bukkit.getWorlds().mapTo(mutableSetOf()) { it.name }
        CompletableFuture.supplyAsync { gateway.loadProtectionRecords() }
            .thenAccept { records ->
                // ワールドとアーカイブ状態はBukkit/API境界としてmainで確定し、重いDB結果処理は非同期で続ける。
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    val archivedWorlds = records.asSequence()
                        .map { it.world }
                        .filter(WorldArchiveStatusProviders::isArchived)
                        .toSet()
                    CompletableFuture.supplyAsync {
                        val previous = snapshotStore.load()
                        val report = LwcStatusAggregator.aggregate(records, existingWorlds, archivedWorlds)
                        snapshotStore.save(report.snapshot)
                        LwcStatusAggregator.withPreviousSnapshot(report, previous)
                    }.thenAccept { report ->
                        callbackOnMain(callback, report, null)
                    }.exceptionally { error ->
                        callbackOnMain(callback, null, error.cause?.message ?: error.message ?: "status_failed")
                        null
                    }
                })
            }
            .exceptionally { error ->
                callbackOnMain(callback, null, error.cause?.message ?: error.message ?: "status_failed")
                null
            }
    }

    fun loadRemainedInfo(callback: (List<LwcWorldSummary>?, String?) -> Unit) {
        val gateway = gatewayOrNull()
        if (gateway == null) {
            callbackOnMain(callback, null, "unavailable")
            return
        }

        val existingWorlds = Bukkit.getWorlds().mapTo(mutableSetOf()) { it.name }
        CompletableFuture.supplyAsync { gateway.loadProtectionRecords() }
            .thenAccept { records ->
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    val archivedWorlds = records.asSequence()
                        .map { it.world }
                        .filter(WorldArchiveStatusProviders::isArchived)
                        .toSet()
                    CompletableFuture.supplyAsync {
                        LwcStatusAggregator.aggregate(records, existingWorlds, archivedWorlds).remainingMissingWorlds
                    }.thenAccept { worlds -> callbackOnMain(callback, worlds, null) }
                        .exceptionally { error ->
                            callbackOnMain(callback, null, error.cause?.message ?: error.message ?: "lookup_failed")
                            null
                        }
                })
            }
            .exceptionally { error ->
                callbackOnMain(callback, null, error.cause?.message ?: error.message ?: "lookup_failed")
                null
            }
    }

    private fun gatewayOrNull(): LwcGateway? {
        if (!plugin.server.pluginManager.isPluginEnabled("LWC")) return null
        // LWC導入済みなのに接続生成が失敗した場合は、未導入扱いに変換せず呼び出し元へ知らせる。
        return LwcXGateway()
    }

    private fun <T> callbackOnMain(callback: (T?, String?) -> Unit, value: T?, error: String?) {
        if (Bukkit.isPrimaryThread()) {
            callback(value, error)
        } else {
            Bukkit.getScheduler().runTask(plugin, Runnable { callback(value, error) })
        }
    }
}
