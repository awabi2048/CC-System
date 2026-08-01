package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuReversibleStateProviderDefinition
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateProviderRegistry
import java.util.concurrent.ConcurrentHashMap

internal class MenuReversibleStateProviderRegistryImpl : MenuReversibleStateProviderRegistry {
    private val definitions = ConcurrentHashMap<String, MenuReversibleStateProviderDefinition>()

    override fun register(definition: MenuReversibleStateProviderDefinition) {
        check(definitions.putIfAbsent(definition.providerId, definition) == null) {
            "Reversible state provider is already registered: ${definition.providerId}"
        }
    }

    override fun unregister(owner: String, id: String) {
        definitions.remove("$owner:$id")
    }

    override fun unregisterOwner(owner: String) {
        definitions.entries.removeIf { it.value.owner == owner }
    }

    override fun definition(providerId: String): MenuReversibleStateProviderDefinition? = definitions[providerId]

    override fun definitions(): List<MenuReversibleStateProviderDefinition> =
        definitions.values.sortedBy(MenuReversibleStateProviderDefinition::providerId)
}
