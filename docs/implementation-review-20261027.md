# 2026年10月27日 実装監査の反映

## 描画層

Textの宣言層40は実効層13に相当します。宣言レイアウトは実効層13を上限とし、それを超える文書はLAYER_DEPTH_EXCEEDEDを返して表示・操作面の両方を生成しません。ホバーも同じ実効層上限を使います。深い文書を黙って切り詰めて背景の背面へ文字を戻す動作は廃止しました。

## 名前付き領域

GestureGuiScaffold.namedRegionsはHTML解析後の文書から領域の内容矩形を取得します。HTMLと呼出し側で領域オフセットを二重管理せず、不足・重複IDをエラーにします。

## 回帰確認

表現不能な深さで表示・操作を生成しないこと、既存の深さで文字が前面にあること、領域変更が取得座標へ反映されること、ID不足・重複の拒否をテストします。

## 検証結果

- 2026年10月27日: Gradle build成功、自動テスト513件成功（失敗・エラー・スキップ0件）。
- dev-envとfull-envで両プラグイン26.1027.1の起動完了・RCON応答・JARハッシュ一致を確認。smokeは両環境ともPASS、失敗0件。
- 設定項目の変更はなく、両環境のCC-System/config.ymlとKantanCommander/config.ymlのSHA-256は配置前後で一致。
- GUI操作の実機目視確認は未実施。座標・操作ID・状態分岐は既存の画面構築テストで確認。
- 既存DSL保存データ3件の読込警告が残っています。dev-envでは更新前ログも3件であることを確認しています。
- サーバー起動時にWindowsのOSHI性能カウンタ取得エラーも出ていますが、起動は完了しRCONとsmokeは正常です。

### dev-env

- CC-System: `173F76CD1BE8C9DA309C875465B8A762F1F49A527AE01077F3C6C045B1A87C4E`
- KantanCommander: `1147ADFADE337B6B3992D2D27DEC4F3BB1EAF465C626EA6C0BDDEB6671669650`

### full-env

- CC-System: `173F76CD1BE8C9DA309C875465B8A762F1F49A527AE01077F3C6C045B1A87C4E`
- KantanCommander: `1147ADFADE337B6B3992D2D27DEC4F3BB1EAF465C626EA6C0BDDEB6671669650`
