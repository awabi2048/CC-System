package com.awabi2048.ccsystem.api.gesturegui.layout

/**
 * ページ分割の純粋計算です。
 *
 * 1ページあたりの項目数（行数×列数）は画面ファミリーごとの契約として呼び出し側が
 * 決め、ここではページ数・現在ページの丸め・一覧の切り出し位置だけを扱います。
 * レイアウト上の座標とは独立しており、足場（[GestureGuiScaffold]）や
 * ページャ部品（[GestureGuiComponents.pager]）と組み合わせて使います。
 */
object GestureGuiPagination {
    /** ページ分割の結果です。`start`/`endExclusive` は項目一覧の切り出し位置です。 */
    data class Page(
        val index: Int,
        val count: Int,
        val start: Int,
        val endExclusive: Int,
        val pageSize: Int,
    )

    /**
     * 項目数・1ページあたり行数・列数・要求ページから表示ページを決めます。
     * 要求ページが範囲外なら最近傍へ丸め、項目0件でもページ数は1を維持します。
     */
    fun plan(itemCount: Int, rowsPerPage: Int, columns: Int = 1, requestedPage: Int = 0): Page {
        require(itemCount >= 0) { "項目数は0以上である必要があります: $itemCount" }
        require(rowsPerPage > 0) { "1ページあたり行数は1以上である必要があります: $rowsPerPage" }
        require(columns > 0) { "列数は1以上である必要があります: $columns" }
        val pageSize = rowsPerPage * columns
        val count = ((itemCount + pageSize - 1) / pageSize).coerceAtLeast(1)
        val index = requestedPage.coerceIn(0, count - 1)
        val start = index * pageSize
        return Page(
            index = index,
            count = count,
            start = start,
            endExclusive = (start + pageSize).coerceAtMost(itemCount),
            pageSize = pageSize,
        )
    }

    /** 項目一覧の表示ページぶんを切り出します。 */
    fun <T> pageOf(items: List<T>, page: Page): List<T> = items.subList(page.start, page.endExclusive)
}
