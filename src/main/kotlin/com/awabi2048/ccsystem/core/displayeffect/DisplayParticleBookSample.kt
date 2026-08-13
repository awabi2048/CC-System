package com.awabi2048.ccsystem.core.displayeffect

/**
 * 本と羽ペンによる動作確認をすぐ始められる、編集可能な初期パーティクル設定です。
 * 各要素はBookMetaの物理ページ上限を避けるため、トップレベル項目単位で分離しています。
 */
internal object DisplayParticleBookSample {
    val pages: List<String> = listOf(
        "\"textures\":[{\"block\":\"minecraft:orange_concrete\",\"weight\":3},{\"block\":\"minecraft:yellow_concrete\",\"weight\":1}]",
        "\"scale\":{\"initial\":0.05,\"peak\":0.12,\"peak_progress\":0.3,\"scale_in_ticks\":3,\"variation\":0.2}",
        "\"rotation\":{\"random_initial\":true,\"angular_velocity\":[0.08,0.13,0.05],\"variation\":0.3}",
        "\"lifetime\":{\"ticks\":24,\"variation\":2,\"fade_out_ticks\":7,\"fade_variation\":1,\"spawn_delay\":2}",
        "\"motion\":{\"preset\":\"burst\",\"initial_velocity\":[0,0.03,0],\"radial_speed\":0.15}",
        "\"collision\":{\"mode\":\"remove\"}",
        "\"emission\":{\"offset\":[0,1,0],\"delta\":[0.1,0.1,0.1],\"speed\":0.02,\"count\":4,\"visibility\":\"force\"}"
    )
}
