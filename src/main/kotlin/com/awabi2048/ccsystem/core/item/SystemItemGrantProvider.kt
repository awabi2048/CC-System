package com.awabi2048.ccsystem.core.item

import com.awabi2048.ccsystem.api.item.ItemGrantDefinition
import com.awabi2048.ccsystem.api.item.ItemGrantProvider
import com.awabi2048.ccsystem.api.item.ItemGrantRequest
import com.awabi2048.ccsystem.api.item.ItemGrantResult

/** CC-System自身が所有する検証・運用アイテムを、共通の /cc give 経路へ登録します。 */
internal class SystemItemGrantProvider : ItemGrantProvider {
    override val owner: String = "cc-system"

    override fun definitions(): Collection<ItemGrantDefinition> = listOf(
        ItemGrantDefinition(
            id = DISPLAY_PARTICLE_SAMPLE_ID,
            permission = "cc.item.give.cc-system",
            maximumAmount = 16
        ) { emptyList() }
    )

    override fun grant(request: ItemGrantRequest): ItemGrantResult {
        if (request.definition.id != DISPLAY_PARTICLE_SAMPLE_ID) {
            return ItemGrantResult(false, 0, 0, "unknown item id")
        }

        var dropped = 0
        repeat(request.amount) {
            val item = CustomItemFactory.createDisplayParticleSampleBook(request.target)
            request.target.inventory.addItem(item).values.forEach { overflow ->
                dropped += overflow.amount
                request.target.world.dropItemNaturally(request.target.location, overflow)
            }
        }
        return ItemGrantResult(true, request.amount - dropped, dropped, null)
    }

    private companion object {
        const val DISPLAY_PARTICLE_SAMPLE_ID = "cc-system.display_particle_book_sample"
    }
}
