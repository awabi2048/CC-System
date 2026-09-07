package com.awabi2048.ccsystem.api.gesturegui.html

import org.bukkit.block.data.BlockData
import org.bukkit.inventory.ItemStack

/**
 * 制限付き HTML frontend の見た目解決口です。
 *
 * HTML/CSS 自体は構造・配置・文言だけを表し、素材・アイテム等のサーバー資源は
 * 呼び出し側指定とします（Gesture GUI 方針）。素材名は正規化済み
 *（小文字・`_` 区切り、例 `black_concrete`）で渡します。
 */
interface GestureGuiHtmlEnvironment {
    /**
     * `<button>` の背景を返します。
     * @param background CSS `background` の素材名、未指定時は null です。
     */
    fun buttonBackground(background: String?, classes: Set<String>, id: String?): BlockData

    /**
     * 容器の CSS `background` に対する背景を返します。
     * null の場合は背景なしとして扱います。
     */
    fun containerBackground(background: String, classes: Set<String>, id: String?): BlockData?

    /** CSS `border` 用の枠素材を返します。null の場合は枠なしとして扱います。 */
    fun outlineBlock(classes: Set<String>, id: String?): BlockData?

    /** `<mc-block material="...">` 用の素材を返します。 */
    fun blockData(material: String, classes: Set<String>, id: String?): BlockData

    /** `<mc-item src="...">` 用の表示品を返します。 */
    fun itemStack(reference: String, classes: Set<String>, id: String?): ItemStack

    /** 文言の論理文字サイズを返します。タグ既定の上書きに使います。 */
    fun textSize(tag: String, classes: Set<String>, id: String?): Double
}
