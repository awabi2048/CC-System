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
        // テスト実行調整で所要時間内訳・実処理時間・ログ見出し・失敗理由・中断通知の16件を追加し、
        // 続く調整で内訳値と件数行を除去して短縮理由3件・待機時間・位置見出しの5件を追加し、
        // 開始・終了通知の3件を追加しました。
        // ナビゲーション調整で表示倍率リセットの1件を廃止しました。
        // 粒子表示設定の分散範囲ラベル1件と制御ブロック権限フローの拒否理由2件を追加しました。
        // 旧汎用拒否文言1件を廃止し、クリエイティブ限定の拒否理由1件を追加しました。
        // 折りたたみ操作案内（FORの折畳み・展開、IFの折畳み・展開）の4件を追加しました。
        // 破壊時の制御ブロック取得通知1件と未選択時下部のディスク取得ボタン1件を追加しました。
        // 座標の成分別指定（絶対／相対／ローカル）の選択肢3件を追加しました。
        assertEquals(841, keys.size)
        assertEquals(
            "91ce19c64d1fec4224dff4db460da43980c8f8a17111d12763de7666a93455fb",
            LocalizationCatalogContract.fingerprint("kantan_commander_clean"),
        )
        assertEquals(
            LocalizationKey.ValueType.TEXT_LIST,
            LocalizationCatalogContract.valueType("kantan_commander_clean.gui.editor.add_description"),
        )
    }
}
