package com.awabi2048.ccsystem.api.localization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KantanLocalizationContractTest {
    @Test
    fun `Kantan GUI contract retains every typed key`() {
        val keys = LocalizationCatalogContract.keys()
            .filter { it.startsWith("kantan_commander_clean.") }

        // fd8fc1bの型付き値ソース、f5180f4の複製操作、b225febの制御ブロック状態、
        // 5a30ab5の粒子設定、8c22d71のGesture GUI操作、d05a242のテスト実行ラベル、
        // および一覧更新失敗・スキャン不完全のエラー分離キーを統合した公開キー総数とfingerprintを固定します。
        assertEquals(783, keys.size)
        assertEquals(
            "cf835eb7f4fd4a1af78513da6aa61ad7678651341bbf721fe96651fda36d3e3f",
            LocalizationCatalogContract.fingerprint("kantan_commander_clean"),
        )
        assertEquals(
            LocalizationKey.ValueType.TEXT_LIST,
            LocalizationCatalogContract.valueType("kantan_commander_clean.gui.editor.add_description"),
        )
    }
}
