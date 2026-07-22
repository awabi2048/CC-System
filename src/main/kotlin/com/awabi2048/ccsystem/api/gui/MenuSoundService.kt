package com.awabi2048.ccsystem.api.gui

import org.bukkit.entity.Player

/**
 * メニューの効果音再生を統一する基盤サービス。
 *
 * 明示的再生方式を採用: 各メニューの open() で明示的に onMenuOpen を呼び、
 * 同一画面の再描画（ページ送り・ソート更新等）では呼び出し側が抑制する。
 * 間接的なタイトル/ホルダー比較には依存しない。
 */
interface MenuSoundService {
    /** 解決済みの任意メニュー音を再生する。 */
    fun play(player: Player, sound: MenuSound)

    /**
     * メニューを開いたときの効果音を再生する。
     * 新しい画面を開いたときに呼ぶ。再描画（リフレッシュ）時は呼ばない。
     *
     * @param player 対象プレイヤー
     * @param menuId メニュー識別子（デフォルト音の上書き設定の参照用。未対応の場合は共通音が鳴る）
     */
    fun onMenuOpen(player: Player, menuId: String? = null)

    /**
     * メニュー内のボタンをクリックしたときの効果音を再生する。
     *
     * @param player 対象プレイヤー
     * @param menuId メニュー識別子（メニュー固有音の参照用）
     * @param clickType クリックの種別（確定/キャンセル/ページ送り/通常 等）
     */
    fun onMenuClick(player: Player, menuId: String? = null, clickType: MenuClickType = MenuClickType.DEFAULT)

    /**
     * メニュー内の特定アイコンをクリックしたときの効果音を再生する。
     *
     * menuId / iconId の組み合わせでプラグイン固有設定を優先し、未設定なら clickType の共通音へ落とす。
     */
    fun onMenuIconClick(
        player: Player,
        menuId: String,
        iconId: String,
        clickType: MenuClickType = MenuClickType.DEFAULT
    )

    /**
     * メニュー外の補助的操作（コピー通知・管理操作等）のクリック音。
     * onMenuClick の簡易版で、メニュー文脈を持たない汎用クリック音を再生する。
     */
    fun onGenericClick(player: Player)

    /**
     * プラグインが自身のメニュー音設定（開封音・クリック音のオーバーライド）を登録する。
     * 登録された設定は menuId で参照され、未登録のメニューには共通デフォルト音が使われる。
     *
     * 既存の設定がある場合は上書きされる。アンロード時は [unregisterProvider] で解除すること。
     */
    fun registerProvider(provider: MenuSoundProvider)

    /**
     * 登録済みのメニュー音設定プロバイダを解除する。
     */
    fun unregisterProvider(sourceId: String)
}

/**
 * メニュー音のカスタマイズを提供するプロバイダ。
 * 各プラグイン（MWM, cc-content 等）が実装し、メニュー固有の音設定を cc-system に伝える。
 */
interface MenuSoundProvider {
    /** プロバイダの識別子（プラグイン名等）。登録の重複排除と解除に用いる。 */
    val sourceId: String

    /**
     * 指定メニューの開封音設定を取得する。
     * @return 設定が存在すれば [MenuSound]，なければ null（共通デフォルト音が使われる）
     */
    fun openSound(menuId: String): MenuSound?

    /**
     * 指定メニュー・クリック種別の音設定を取得する。
     * @return 設定が存在すれば [MenuSound]，なければ null（クリック種別のデフォルト音が使われる）
     */
    fun clickSound(menuId: String, clickType: MenuClickType): MenuSound?

    /**
     * 指定メニュー・アイコンIDの音設定を取得する。
     * メニュー設定がアイコンID単位で管理される既存プラグインの移行に使う。
     */
    fun iconSound(menuId: String, iconId: String): MenuSound?

    /**
     * メニュー文脈を持たない汎用クリック音を取得する。
     */
    fun genericClickSound(): MenuSound?
}

/**
 * 効果音1件の指定。
 */
data class MenuSound(
    /** Sound の名前（例: "UI_BUTTON_CLICK", "minecraft:ui.button.click"）。解決失敗時は無視される。 */
    val sound: String,
    /** ピッチ */
    val pitch: Float = 1.0f,
    /** 音量 */
    val volume: Float = 1.0f,
)

/**
 * メニュークリックの種別。メニュー固有の音設定が無い場合のデフォルト音選択に用いる。
 */
enum class MenuClickType {
    /** 通常のボタンクリック */
    DEFAULT,
    /** 確定・実行 */
    CONFIRM,
    /** キャンセル・戻る */
    CANCEL,
    /** ページ送り（前/次） */
    NAVIGATION,
    /** 情報表示 */
    INFO,
}
