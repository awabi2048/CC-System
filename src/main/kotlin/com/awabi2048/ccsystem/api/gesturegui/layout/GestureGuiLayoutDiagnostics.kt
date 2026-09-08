package com.awabi2048.ccsystem.api.gesturegui.layout

/**
 * レイアウト検証のエラーコードです。
 *
 * LLM 生成・修正との相性を考え、コードは英字固定＋パラメータ付きにします。
 * ユーザー可視ローカライズ対象ではなく、開発者向けの構造化診断として扱います。
 */
enum class GestureGuiLayoutErrorCode {
    /** 親内容域からの逸脱です（CLIP により切り抜かれます）。 */
    LAYOUT_OVERFLOW,

    /** REJECT 領域での逸脱です。 */
    REJECT_OVERFLOW,

    /** 不正なサイズ指定の解決結果です（非有限・非正など）。 */
    INVALID_SIZE,

    /** フロー内の非 Text リーフに Auto が指定されました。明示サイズを指定してください。 */
    AUTO_SIZE_UNSUPPORTED,

    /** 空の hitbox です（action あり・受付 gesture なし等）。 */
    EMPTY_HITBOX,

    /** clip 後に visual が消えた interaction の残存です。 */
    CLIPPED_INTERACTION,

    /** 安定IDの重複です。 */
    DUPLICATE_ID,

    /** 未登録の action 参照です。 */
    UNKNOWN_ACTION,

    /** 未対応の HTML タグです（HTML frontend 用に予約）。 */
    UNSUPPORTED_TAG,

    /** 未対応の CSS プロパティ・値です（HTML frontend 用に予約）。 */
    UNSUPPORTED_PROPERTY,

    /** Custom renderer が未登録です。 */
    CUSTOM_RENDERER_MISSING,

    /** 切り抜きに必要な Custom Text / Item の表示矩形がありません。 */
    CUSTOM_VISUAL_BOUNDS_MISSING,

    /** Custom 操作面の表示参照が renderer の出力に存在しません。 */
    UNKNOWN_VISUAL,

    /** HTML の構文異常です（対応する範囲で復旧し、診断します）。 */
    HTML_PARSE_ERROR,

    /**
     * 操作面を伴わない guard です。
     * guard は操作面の入力時判定にのみ用いるため、action・消費面なしでは評価されません。
     */
    GUARD_WITHOUT_ACTION,
}

/**
 * 構造化された1件の診断です。message は英語固定形式とします。
 * source には将来 "editor.html:42" のような発生源を入れます（直接構築時は null）。
 */
data class GestureGuiLayoutDiagnostic(
    val code: GestureGuiLayoutErrorCode,
    val nodeId: String?,
    val parentId: String?,
    val message: String,
    val source: String? = null,
) {
    init {
        require(message.isNotBlank()) { "layout diagnostic message must not be blank" }
        require(source == null || source.isNotBlank()) { "layout diagnostic source must not be blank" }
    }
}
