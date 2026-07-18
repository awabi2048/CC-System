package com.awabi2048.ccsystem.core.item

import com.awabi2048.ccsystem.api.item.ItemGrantDefinition
import com.awabi2048.ccsystem.api.item.ItemGrantProvider
import com.awabi2048.ccsystem.api.item.ItemGrantRequest
import com.awabi2048.ccsystem.api.item.ItemGrantResult
import com.awabi2048.ccsystem.api.item.ItemGrantService
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ItemGrantServiceImpl : ItemGrantService {
    private data class Registered(
        val provider: ItemGrantProvider,
        val definition: ItemGrantDefinition
    )

    private val byId = ConcurrentHashMap<String, Registered>()

    override fun register(provider: ItemGrantProvider) {
        unregister(provider.owner)
        val definitions = provider.definitions().toList()
        require(definitions.isNotEmpty()) { "Item grant provider has no definitions: ${provider.owner}" }
        definitions.forEach { definition ->
            require(definition.id.matches(Regex("[a-z0-9_.:-]+"))) {
                "Invalid item grant id: ${definition.id}"
            }
            require(definition.maximumAmount > 0) { "maximumAmount must be positive: ${definition.id}" }
            val previous = byId.putIfAbsent(definition.id, Registered(provider, definition))
            require(previous == null) { "Duplicate item grant id: ${definition.id}" }
        }
    }

    override fun unregister(owner: String) {
        byId.entries.removeIf { it.value.provider.owner == owner }
    }

    override fun definitions(): List<ItemGrantDefinition> =
        byId.values.map(Registered::definition).sortedBy(ItemGrantDefinition::id)

    override fun definition(id: String): ItemGrantDefinition? = byId[id]?.definition

    override fun grant(
        sender: CommandSender,
        target: Player,
        itemId: String,
        amount: Int,
        arguments: List<String>
    ): ItemGrantResult {
        val registered = byId[itemId] ?: return ItemGrantResult(false, 0, 0, "unknown item id")
        if (amount !in 1..registered.definition.maximumAmount) {
            return ItemGrantResult(false, 0, 0, "invalid amount")
        }
        val permission = registered.definition.permission
        if (permission != null && !sender.hasPermission(permission) && !sender.isOp) {
            return ItemGrantResult(false, 0, 0, "no permission")
        }
        return runCatching {
            registered.provider.grant(
                ItemGrantRequest(sender, target, registered.definition, amount, arguments)
            )
        }.getOrElse { ItemGrantResult(false, 0, 0, it.message ?: "grant failed") }
    }
}
