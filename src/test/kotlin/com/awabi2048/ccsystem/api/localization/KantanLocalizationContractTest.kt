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
        // コマンド実行位置の名称・説明・操作案内・警告の4件を追加しました。
        // 対象・位置・移動先の再編に伴う詳細設定・参照・向き指定等の9件を追加し、
        // Dialog操作ラベルの色指定を&形式から§形式へ統一しました。
        // 向き複写の選択肢・説明の2件を追加しました。
        // プリセットホバー案内の専用キー18件を追加しました。
        // ビューポートの未完了表示キー1件を追加しました。
        // Entity操作再編（#32）に伴う操作コマンド・選択肢・設定項目・警告の67件を追加し、
        // 既存のエンティティ操作コマンド文言をアクション側へ整理しました。
        // 部分適用件数表示（F4）の1件を追加しました。
        // アイテム配置コマンド（issue #21）のコマンド名・説明・配置先選択肢・
        // 項目名・説明・操作案内・警告の19件を追加し、ENTITY_ACTIONの説明から
        // /item・/tag相当の記載を外してride/dismount/motionに限定しました。
        // アイテム配置の上書きトグル移設に伴い、配置先タブ用の固定ラベル1件を追加しました。
        // #8の引数・戻り値に伴い、値を返すコマンド名・説明の2件を追加しました。
        // #7の起動方法に伴い、起動方法名・ダイアログ説明・起動種別8件・警告の11件を追加しました。
        // #8の指摘対応でディスク設定（取り出しと引数定義）のキー7件を追加しました。
        // #22のENTITY scope選択肢の1件を追加しました。
        // Mannequin対応で固有項目11件・選択肢9件の20件を追加しました。
        // Mannequin説明文の1件を追加しました。
        // 統合後の件数は1034件とし、指紋を確定します。
        assertEquals(1034, keys.size)
        assertEquals(
            "cf086e932a2fd75a0297de5c88479488c6798d9aab1a86592abd49feddc3c884",
            LocalizationCatalogContract.fingerprint("kantan_commander_clean"),
        )
        assertEquals(
            LocalizationKey.ValueType.TEXT_LIST,
            LocalizationCatalogContract.valueType("kantan_commander_clean.gui.editor.add_description"),
        )
    }
}
