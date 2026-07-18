package com.awabi2048.ccsystem.api.item

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

fun interface ItemArgumentSuggestionProvider {
    fun suggest(arguments: List<String>): List<String>
}

data class ItemGrantDefinition(
    val id: String,
    val permission: String?,
    val maximumAmount: Int,
    val argumentSuggestions: ItemArgumentSuggestionProvider
)

data class ItemGrantRequest(
    val sender: CommandSender,
    val target: Player,
    val definition: ItemGrantDefinition,
    val amount: Int,
    val arguments: List<String>
)

data class ItemGrantResult(
    val success: Boolean,
    val grantedAmount: Int,
    val droppedAmount: Int,
    val message: String?
)

interface ItemGrantProvider {
    val owner: String

    fun definitions(): Collection<ItemGrantDefinition>

    fun grant(request: ItemGrantRequest): ItemGrantResult
}

interface ItemGrantService {
    fun register(provider: ItemGrantProvider)

    fun unregister(owner: String)

    fun definitions(): List<ItemGrantDefinition>

    fun definition(id: String): ItemGrantDefinition?

    fun grant(
        sender: CommandSender,
        target: Player,
        itemId: String,
        amount: Int,
        arguments: List<String>
    ): ItemGrantResult
}
