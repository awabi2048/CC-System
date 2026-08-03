package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuRoute
import com.awabi2048.ccsystem.api.gui.MenuRouteResultOpener
import java.lang.reflect.Proxy
import java.util.UUID
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MenuNavigationServiceImplTest {
    @Test
    fun `opener virtual machine error is never converted to an operation failure`() {
        val navigation = MenuNavigationServiceImpl()
        val route = MenuRoute("test", "opener-vm-error")
        navigation.registerResultOpener(route.owner, route.id, MenuRouteResultOpener { _, _ ->
            throw OutOfMemoryError("test")
        })

        assertThrows(OutOfMemoryError::class.java) {
            navigation.openResult(player(), route)
        }
    }

    @Test
    fun `confirmation cancellation restores the menu before confirmation and keeps outer history`() {
        val navigation = MenuNavigationServiceImpl()
        val player = player(UUID.randomUUID())
        val root = MenuRoute("test", "root")
        val beforeConfirmation = MenuRoute("test", "settings")
        val firstConfirmation = MenuRoute("test", "confirmation-one")
        val secondConfirmation = MenuRoute("test", "confirmation-two")
        listOf(root, beforeConfirmation, firstConfirmation, secondConfirmation).forEach { route ->
            navigation.registerOpener(route.owner, route.id) { _, _ -> true }
        }

        assertTrue(navigation.openRoot(player, root))
        assertTrue(navigation.pushAndOpen(player, root, beforeConfirmation))
        assertTrue(navigation.pushAndOpen(player, beforeConfirmation, firstConfirmation))
        assertTrue(navigation.pushAndOpen(player, firstConfirmation, secondConfirmation))

        val result = navigation.restoreAndOpenResult(
            player,
            beforeConfirmation,
            listOf(root),
        )

        assertTrue(result.successful)
        assertEquals(beforeConfirmation, navigation.currentRoute(player))
        assertEquals(listOf(root), navigation.breadcrumbs(player))
    }

    private fun player(): Player = player(UUID.randomUUID())

    private fun player(playerId: UUID): Player = Proxy.newProxyInstance(
        Player::class.java.classLoader,
        arrayOf(Player::class.java),
    ) { proxy, method, arguments ->
        when (method.name) {
            "getUniqueId" -> playerId
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === arguments?.singleOrNull()
            "toString" -> "TestPlayer"
            else -> throw UnsupportedOperationException(method.name)
        }
    } as Player
}
