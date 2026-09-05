package com.awabi2048.ccsystem.api.localization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KantanLocalizationContractTest {
    @Test
    fun `Kantan GUI contract retains every typed key`() {
        val keys = LocalizationCatalogContract.keys()
            .filter { it.startsWith("kantan_commander_clean.") }

        // fd8fc1bの型付き値ソース、f5180f4の複製操作、b225febの制御ブロック状態、
        // 5a30ab5の粒子設定、8c22d71のGesture GUI操作、d05a242のテスト実行ラベルを
        // 統合した公開キー総数とfingerprintを固定します。
        assertEquals(781, keys.size)
        assertEquals(
            "bda1f404f6cde22916499131a255f488ba516ba9e3822d9ffce6436a05261685",
            LocalizationCatalogContract.fingerprint("kantan_commander_clean"),
        )
        assertEquals(
            LocalizationKey.ValueType.TEXT_LIST,
            LocalizationCatalogContract.valueType("kantan_commander_clean.gui.editor.add_description"),
        )
    }
}
