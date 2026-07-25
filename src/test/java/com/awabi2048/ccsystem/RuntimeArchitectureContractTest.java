package com.awabi2048.ccsystem;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuntimeArchitectureContractTest {
    private static final String ALLOWLIST_RESOURCE = "/architecture/runtime-ui-legacy-allowlist.txt";
    private static final Map<String, Pattern> RULES = Map.of(
        "CREATE_INVENTORY", Pattern.compile("Bukkit\\s*\\.\\s*createInventory"),
        "INVENTORY_CLICK_EVENT", Pattern.compile("InventoryClickEvent"),
        "DIALOG_CREATE", Pattern.compile("Dialog\\s*\\.\\s*create"),
        "CUMULUS_FORM", Pattern.compile("org\\.geysermc\\.cumulus|(?:SimpleForm|CustomForm|ModalForm)\\s*\\.\\s*builder"),
        "MANUAL_CLICK_SOUND", Pattern.compile("playClickSound|playAdminClickSound")
    );

    @Test
    void Runtimeを迂回するUI実装を増やさない() throws Exception {
        Path projectRoot = Path.of("").toAbsolutePath().normalize();
        Path sourceRoot = projectRoot.resolve("src/main");
        Set<String> actual = new TreeSet<>();

        try (var paths = Files.walk(sourceRoot)) {
            for (Path source : paths.filter(Files::isRegularFile)
                .filter(RuntimeArchitectureContractTest::isSourceFile)
                .toList()) {
                String content = Files.readString(source);
                String relative = projectRoot.relativize(source).toString().replace('\\', '/');
                RULES.forEach((id, pattern) -> {
                    long count = pattern.matcher(content).results().count();
                    if (count > 0) {
                        actual.add(id + "|" + count + "|" + relative);
                    }
                });
            }
        }

        assertEquals(
            expectedRecords(),
            actual,
            "Runtime迂回実装が変化しました。新規追加は禁止です。移行で削減した場合だけ許可リストも同時に減らしてください。"
        );
    }

    private static boolean isSourceFile(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".kt") || name.endsWith(".java");
    }

    private static Set<String> expectedRecords() throws Exception {
        InputStream stream = RuntimeArchitectureContractTest.class.getResourceAsStream(ALLOWLIST_RESOURCE);
        if (stream == null) {
            throw new IllegalStateException("Runtime移行の一時許可リストがありません: " + ALLOWLIST_RESOURCE);
        }
        try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .collect(Collectors.toCollection(TreeSet::new));
        }
    }
}
