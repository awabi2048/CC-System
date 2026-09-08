# CC-System
for KotaServer/CrafterCrossing

## 宣言的Gesture GUIのレイアウト契約

KantanCommanderの[issue #25](https://github.com/awabi2048/KantanCommander/issues/25)に対応する共通基盤です。
既存の低レベル描画APIは維持し、宣言文書を解決してから`GestureGuiView`へ変換します。

### 26.908.4の修正範囲

- レイアウト契約版は2です。`effectiveClip`は`GestureGuiClip`型となり、無制限・矩形・空を区別します。既存の低レベルGesture GUI契約版18、HTMLプロファイル版1は変更しません。
- `CLIP`では矩形の交差を子へ継承します。空になった領域は、子が`VISIBLE`でも復活しません。
- Blockは描画矩形と操作面を切り抜きます。枠線は元の四辺を切り抜き、切断面に新しい枠を追加しません。
- TextとItemは部分切り抜きができないため、表示矩形全体が領域内に収まる場合だけ残します。消えた表示に紐づく操作面も除去します。
- 通常のTextとItemでは割当矩形を表示領域として扱います。完全なフォント・アイテムモデルの実寸測定は行わないため、呼び出し側は文言・素材・倍率に十分な幅と高さを指定してください。
- Custom rendererはTextとItemの表示矩形を`GestureGuiCustomRenderResult.visualBounds`へ指定します。キーは名前空間付与前のvisual ID、矩形は画面中央原点です。CLIP内で矩形が不足した表示は診断して除去します。Blockは自身の幅と高さを使用します。
- Custom出力のvisual、操作面、ホバー内のvisual参照を同じ名前空間へ変換します。切り抜き後に消えた参照を残しません。
- BoxとColumnのPercent高さは親の内容高さを基準にします。Fractionは固定・割合・Auto寸法、margin、gapを差し引いた残余を重み配分します。親自身がAutoで測定中の場合の相対高さは引き続き診断対象です。
- HTMLの`gesture-viewport`は既定でCLIPです。明示的な`overflow: visible`で解除できます。
- 背景付きHTML容器は、外箱だけにID・margin・絶対位置・寸法指定を適用し、内箱に内容配置・paddingを適用します。
- 未対応または適用対象外のCSS、不正な数値を診断します。`font-size: 0`等の入力で解析を例外終了させません。
- compilerの診断にはLayout Engineの診断も含めます。

### 26.908.5の追加範囲

- レイアウト契約版は3です。宣言ノードへ`gestureGuard`・`hover`・`consumeInput`を追加しました。既存の低レベルGesture GUI契約版18、HTMLプロファイル版1は変更しません。
- `gestureGuard`は入力時再評価のためそのまま低レベル要素へ受渡し、画面再構築は不要です。操作面を伴わないguardは`GUARD_WITHOUT_ACTION`で診断します。
- `hover`は解決済みbounds基準の自動位置（上・下・中央＋間隔）または固定座標で低レベルhoverTextへ変換します。tooltipは意図的な逸脱として扱い、元ノードのclipでは切りません。置換対象は自visual・指定ノード・なしを選べ、未知参照は`UNKNOWN_VISUAL`で診断します。
- `consumeInput`はactionなしの無音消費面（余白解除・バリア等）を作ります。受付gestureは空集合へ正規化し、`EMPTY_HITBOX`・`UNKNOWN_ACTION`の対象外です。
- Custom要素にも`gestureGuard`・`consumeInput`を追加し、通常ノードと同じ規則で生成します。
- compilerは表示確定と操作面・ホバー生成を2走査に分け、ホバーの前方ノード参照を解決します。Custom rendererの実行は1回です。

### 26.908.7の修正範囲

- 同一層の文言・品目と背景のz-fightingを、層ではなくrenderer側の微小持上げ（`TEXT_ITEM_SURFACE_LIFT`＝0.0005ブロック）で解消します。層順序は不変のため、既存画面の前後関係に影響しません。

### 26.908.8の追加範囲

- HTMLプロファイル版は2です。`gestures`属性（明示列挙＋`click`・`main_hand`略記）、CSS `color`（限定色名）、CSS `line-width`（正整数）を追加します。未指定時の既定は変えず、不正値は診断します。

### 見た目と操作の維持方針

参照元は既存の宣言APIとKantanCommanderの`clipMapVisual`／`clipMapElement`です。呼び出し側が指定する素材・文言・寸法・情報順・操作の意味は維持します。背景追加による幅の縮小や絶対位置の二重適用、領域外に残る描画・操作だけを修正します。新しい画面やナビゲーション導線は追加しません。

### 検証

`GestureGuiLayoutRegressionTest`でレビューの再現入力をHTML解析から描画・操作面まで通し、通常ノード・Custom出力・入れ子・背景・数値検証を確認します。Mavenの`clean verify`で全テストとJAR生成を実行します。今回の作業範囲はビルドテストまでで、サーバー配置と起動確認は含みません。

2026年9月8日の実行結果は396件成功、失敗・エラー・スキップ0件です。うち今回追加した回帰テストは15件です。
