package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuRoute
import com.awabi2048.ccsystem.api.gui.MenuRouteResultOpener
import java.lang.reflect.Proxy
import java.util.UUID
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertThrows
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

    private fun player(): Player = Proxy.newProxyInstance(
        Player::class.java.classLoader,
        arrayOf(Player::class.java),
    ) { proxy, method, arguments ->
        when (method.name) {
            "getUniqueId" -> UUID.randomUUID()
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === arguments?.singleOrNull()
            "toString" -> "TestPlayer"
            else -> throw UnsupportedOperationException(method.name)
        }
    } as Player
}
