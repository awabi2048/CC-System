package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.features.misc.listener.BinderCommandExecutor;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BinderCommandExecutorTest {
    @Test
    void rendersTargetNameAndUuidWithoutPersistentTargetState() {
        UUID playerId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        List<String> executedCommands = new ArrayList<>();
        Player player = player("Operator", playerId, executedCommands);
        Player target = player("Target", targetId);

        BinderCommandExecutor.INSTANCE.execute(
                player,
                List.of("tell %target_name% %target_uuid% from %player_name% %player_uuid%"),
                target
        );

        assertEquals(List.of("tell Target " + targetId + " from Operator " + playerId), executedCommands);
    }

    private static Player player(String name, UUID uuid) {
        return player(name, uuid, null);
    }

    private static Player player(String name, UUID uuid, List<String> executedCommands) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> name;
                    case "getUniqueId" -> uuid;
                    case "performCommand" -> {
                        if (executedCommands != null) executedCommands.add((String) args[0]);
                        yield true;
                    }
                    case "toString" -> name;
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }
}
