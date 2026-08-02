package com.awabi2048.ccsystem.api.gui

import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MenuCapabilityCompositionTest {
    @Test
    fun `legacy presentation remains full item and generated copy ABI remains usable`() {
        val presentation = MenuCapabilityPresentation(item(Material.STONE))

        assertEquals(MenuCapabilityCompositionMode.FULL_ITEM, presentation.compositionMode)
        assertEquals(presentation, presentation.copy())
        assertEquals(4, MenuCapabilityPresentation::class.java.declaredConstructors
            .filterNot { it.isSynthetic }
            .maxOf { it.parameterCount })
    }

    @Test
    fun `host augmentation owns only copied embedded blocks`() {
        val sourceLines = mutableListOf<GuiLoreLine>(GuiLoreLine.Text("augmentation"))
        val presentation = MenuCapabilityPresentation.hostAugmentation(listOf(GuiLoreBlock(sourceLines)))
        sourceLines += GuiLoreLine.Text("mutated")

        assertEquals(MenuCapabilityCompositionMode.HOST_AUGMENTATION, presentation.compositionMode)
        assertEquals(Material.AIR, presentation.item.material)
        assertEquals(GuiNameSpec.Empty, presentation.item.name)
        assertEquals(GuiLoreSpec.None, presentation.item.lore)
        assertEquals(1, presentation.embeddedLoreBlocks.single().lines.size)
        assertTrue(MenuCapabilityPresentationValidator.violations(presentation).isEmpty())
        assertEquals(
            MenuCapabilityCompositionMode.HOST_AUGMENTATION,
            presentation.copyPreservingCompositionMetadata().compositionMode,
        )
    }

    @Test
    fun `host augmentation validator rejects completed item ownership and empty blocks`() {
        val invalid = MenuCapabilityPresentation.hostAugmentation(
            listOf(GuiLoreBlock(listOf(GuiLoreLine.Text("augmentation")))),
        ).copy(item = item(Material.DIAMOND))
        invalid.compositionMode = MenuCapabilityCompositionMode.HOST_AUGMENTATION

        assertTrue(MenuCapabilityPresentationValidator.violations(invalid).contains("HOST_AUGMENTATION_OWNS_MATERIAL"))
        val empty = MenuCapabilityPresentation(item(Material.AIR))
        empty.compositionMode = MenuCapabilityCompositionMode.HOST_AUGMENTATION
        assertThrows(IllegalArgumentException::class.java) {
            MenuCapabilityPresentationValidator.requireValid(empty)
        }
    }

    @Test
    fun `composer records non-circular host augmentation action and completed fingerprints`() {
        val presentation = MenuCapabilityPresentation.hostAugmentation(
            listOf(GuiLoreBlock(listOf(GuiLoreLine.Text("augmentation")))),
        )
        val resolved = ResolvedMenuCapability("test:augmentation", presentation, emptyList()).also {
            it.compositionMode = MenuCapabilityCompositionMode.HOST_AUGMENTATION
            it.augmentationSource = MenuCapabilityAugmentationSource(it.capabilityId, presentation.embeddedLoreBlocks)
        }
        val host = listOf(GuiLoreBlock(listOf(GuiLoreLine.Data("world", "alpha", "§f"))))
        val actions = listOf(GuiLoreLine.Interaction(null, setOf(ClickType.LEFT), "change"))

        val result = MenuCapabilityComposer.composeHostAugmentation(resolved, item(Material.CHEST), host, actions)
        val blocks = (result.lore as GuiLoreSpec.Blocks).blocks

        assertEquals(listOf(host.single(), presentation.embeddedLoreBlocks.single()), blocks.take(2))
        assertEquals(actions, blocks.last().lines)
        assertEquals("test:augmentation", result.snapshot.contributorCapabilityId)
        assertEquals(MenuCapabilityInsertionBoundary.AFTER_HOST_BLOCKS_BEFORE_ACTIONS, result.snapshot.insertionBoundary)
        assertNotEquals(result.snapshot.hostBlocksFingerprint, result.snapshot.completedCompositionFingerprint)
        assertEquals(1, result.snapshot.augmentationBlockFingerprints.size)
    }

    @Test
    fun `resolved metadata copy preserves composition contract`() {
        val presentation = MenuCapabilityPresentation.hostAugmentation(
            listOf(GuiLoreBlock(listOf(GuiLoreLine.Text("augmentation")))),
        )
        val source = MenuCapabilityAugmentationSource("test:augmentation", presentation.embeddedLoreBlocks)
        val resolved = ResolvedMenuCapability("test:augmentation", presentation, emptyList()).also {
            it.compositionMode = MenuCapabilityCompositionMode.HOST_AUGMENTATION
            it.augmentationSource = source
        }

        val copied = resolved.copyPreservingResolutionMetadata()

        assertEquals(MenuCapabilityCompositionMode.HOST_AUGMENTATION, copied.compositionMode)
        assertEquals(source.fingerprint, copied.augmentationSource?.fingerprint)
        assertNull(copied.unavailableReason)
    }

    private fun item(material: Material) = GuiItemSpec(
        material,
        GuiNameSpec.Empty,
        GuiLoreSpec.None,
        GuiElementRole.CONTENT,
        1,
    )
}
