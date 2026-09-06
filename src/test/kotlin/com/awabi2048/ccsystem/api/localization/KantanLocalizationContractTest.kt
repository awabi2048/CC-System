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
        // テスト実行調整で所要時間内訳・実処理時間・ログ見出し・失敗理由・中断通知の16件を追加しました。
        assertEquals(774, keys.size)
        assertEquals(
            "606b746216bc604070cd72b422a6a6c77e65b8c14b63fd5f0c4f5351211edc2a",
            LocalizationCatalogContract.fingerprint("kantan_commander_clean"),
        )
        assertEquals(
            LocalizationKey.ValueType.TEXT_LIST,
            LocalizationCatalogContract.valueType("kantan_commander_clean.gui.editor.add_description"),
        )
    }
}
