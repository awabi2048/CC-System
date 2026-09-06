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
        // 簡易GUI（統合版専用インベントリ）対応で起動条件・タイマー等の設定項目キー30件を廃止し、
        // 簡易GUI用5件を追加しました。
        assertEquals(758, keys.size)
        assertEquals(
            "a9ef9f3b12b2ab964b1e1008d8429ce7004cce4d8e72e702b983f8cfeb746ddc",
            LocalizationCatalogContract.fingerprint("kantan_commander_clean"),
        )
        assertEquals(
            LocalizationKey.ValueType.TEXT_LIST,
            LocalizationCatalogContract.valueType("kantan_commander_clean.gui.editor.add_description"),
        )
    }
}
