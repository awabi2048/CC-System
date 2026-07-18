package com.awabi2048.ccsystem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GuiWordingRestorationResourceTest {
    @Test
    void japaneseResourcesContainRestoredWording() throws IOException {
        var root = Path.of("src/main/resources/lang/ja_jp/content");
        var gui = read(root.resolve("gui.yml"));
        var arena = read(root.resolve("arena.yml"));
        var sukima = read(root.resolve("sukima_dungeon.yml"));
        var items = read(root.resolve("custom_items.yml"));

        assertTrue(gui.contains("おあげちゃんが疲れてしまうので、連続で行うことはできません"));
        assertTrue(gui.contains("職業に就くと、おあげ神社への奉納が行えるようになります"));
        assertTrue(gui.contains("今日のタスクがすべて完了しました！"));
        assertTrue(arena.contains("allowed_difficulty_suffix: まで"));
        assertTrue(arena.contains("mob_kills_unit: 体"));
        assertTrue(arena.contains("barrier_restarts_unit: 回"));
        assertTrue(arena.contains("{amount} 個"));
        assertTrue(sukima.contains("ポータルが別の世界に繋がってしまうことがあります。"));
        assertTrue(sukima.contains("gui_info_name: '&aInfo'"));
        assertTrue(items.contains("item_name: §6Storage Box"));
        assertTrue(items.contains("unselected: 未選択"));

        assertFalse(gui.contains("連続実行不可"));
        assertFalse(sukima.contains("上位のしおりほど、望んだ傾向の世界に行きやすくなります"));
    }

    @Test
    void englishResourcesDefineEveryNewLocaleSpecificKey() throws IOException {
        var root = Path.of("src/main/resources/lang/en_us/content");
        var gui = read(root.resolve("gui.yml"));
        var arena = read(root.resolve("arena.yml"));
        var sukima = read(root.resolve("sukima_dungeon.yml"));
        var items = read(root.resolve("custom_items.yml"));

        assertTrue(gui.contains("wording:"));
        assertTrue(gui.contains("exchange_limit_unit: items"));
        assertTrue(arena.contains("allowed_difficulty_suffix: or below"));
        assertTrue(arena.contains("details_line: '{unit_price} × {amount} items = {subtotal}'"));
        assertTrue(sukima.contains("gui_style_action: Click to switch"));
        assertTrue(sukima.contains("gui_theme_action: Click to change"));
        assertTrue(items.contains("unselected: Not selected"));
        assertTrue(items.contains("description_3: After registering, Shift-click to store items"));
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
