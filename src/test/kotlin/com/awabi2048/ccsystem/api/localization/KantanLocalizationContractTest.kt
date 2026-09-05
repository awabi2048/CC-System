package com.awabi2048.ccsystem.api.localization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KantanLocalizationContractTest {
    @Test
    fun `Kantan GUI contract retains every typed key`() {
        val keys = LocalizationCatalogContract.keys()
            .filter { it.startsWith("kantan_commander_clean.") }

        // fd8fc1bの型付き値ソース5キーに、f5180f4の複製操作キーを加えた
        // 統合後の公開キー総数を固定します。fingerprintも同じ公開契約の全件を
        // 反映するため、片方のブランチの期待値だけを残さないよう更新します。
        assertEquals(726, keys.size)
        assertEquals(
            "e9eb9c130fc802c99cba38f490a92de92e24e309c0e002aea942e79a06b106f0",
            LocalizationCatalogContract.fingerprint("kantan_commander_clean"),
        )
        assertEquals(
            LocalizationKey.ValueType.TEXT_LIST,
            LocalizationCatalogContract.valueType("kantan_commander_clean.gui.editor.add_description"),
        )
    }
}
