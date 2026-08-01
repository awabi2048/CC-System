package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuReversibleStateProviderDefinition
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateProviderRegistration
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateProviderRegistry
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

internal class MenuReversibleStateProviderRegistryImpl : MenuReversibleStateProviderRegistry {
    private val registrations = ConcurrentHashMap<String, MenuReversibleStateProviderRegistration>()
    private val invalidationListeners = CopyOnWriteArrayList<(MenuReversibleStateProviderRegistration) -> Unit>()

    override fun register(definition: MenuReversibleStateProviderDefinition) {
        val registration = MenuReversibleStateProviderRegistration(definition, UUID.randomUUID())
        check(registrations.putIfAbsent(definition.providerId, registration) == null) {
            "Reversible state provider is already registered: ${definition.providerId}"
        }
    }

    override fun unregister(owner: String, id: String) {
        registrations.remove("$owner:$id")?.let(::notifyInvalidated)
    }

    override fun unregisterOwner(owner: String) {
        val invalidated = mutableListOf<MenuReversibleStateProviderRegistration>()
        registrations.entries.removeIf { entry ->
            if (entry.value.definition.owner != owner) return@removeIf false
            invalidated += entry.value
            true
        }
        invalidated.forEach(::notifyInvalidated)
    }

    override fun definition(providerId: String): MenuReversibleStateProviderDefinition? =
        registrations[providerId]?.definition

    override fun registration(providerId: String): MenuReversibleStateProviderRegistration? = registrations[providerId]

    override fun addInvalidationListener(
        listener: (MenuReversibleStateProviderRegistration) -> Unit,
    ): AutoCloseable {
        invalidationListeners += listener
        return AutoCloseable { invalidationListeners.remove(listener) }
    }

    override fun definitions(): List<MenuReversibleStateProviderDefinition> =
        registrations.values
            .map(MenuReversibleStateProviderRegistration::definition)
            .sortedBy(MenuReversibleStateProviderDefinition::providerId)

    private fun notifyInvalidated(registration: MenuReversibleStateProviderRegistration) {
        invalidationListeners.forEach { listener -> listener(registration) }
    }
}
