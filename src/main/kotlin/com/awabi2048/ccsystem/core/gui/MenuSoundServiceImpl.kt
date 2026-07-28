package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuClickType
import com.awabi2048.ccsystem.api.gui.MenuSound
import com.awabi2048.ccsystem.api.gui.MenuSoundProvider
import com.awabi2048.ccsystem.api.gui.MenuSoundService
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import java.util.concurrent.ConcurrentHashMap

/**
 * MenuSoundService の標準実装。
 *
 * 登録されたプロバイダからメニュー固有の音設定を引き、未登録の場合は共通デフォルト音を鳴らす。
 * 音名の解決は org.bukkit.Sound の enum 名と NamespacedKey 両方を許容する。
 */
class MenuSoundServiceImpl : MenuSoundService {
    private val providers = ConcurrentHashMap<String, MenuSoundProvider>()

    /** メニュー固有設定が無いときの開封音 */
    private val defaultOpenSound = MenuSound("minecraft:block.note_block.hat", pitch = 1.2f)

    /** クリック種別ごとのデフォルト音（メニュー固有設定が無いとき） */
    private val clickSound = MenuSound("UI_BUTTON_CLICK", pitch = 2.0f)

    override fun play(player: Player, sound: MenuSound) {
        playSound(player, sound)
    }

    override fun onMenuOpen(player: Player, menuId: String?) {
        // 開封音は「閉じた状態からメニューを開く」瞬間だけに限定する。
        // GUI間の戻る/ページ送り/再描画ではクリック音だけを残し、チェスト開封音の連続再生を避ける。
        if (player.openInventory.topInventory.type != InventoryType.CRAFTING) return
        val sound = (menuId?.let { resolveOpenSound(it) }) ?: defaultOpenSound
        playSound(player, sound)
    }

    override fun onMenuClick(player: Player, menuId: String?, clickType: MenuClickType) {
        playSound(player, clickSound)
    }

    override fun onMenuIconClick(player: Player, menuId: String, iconId: String, clickType: MenuClickType) {
        playSound(player, clickSound)
    }

    override fun onGenericClick(player: Player) {
        playSound(player, clickSound)
    }

    override fun registerProvider(provider: MenuSoundProvider) {
        providers[provider.sourceId] = provider
    }

    override fun unregisterProvider(sourceId: String) {
        providers.remove(sourceId)
    }

    private fun resolveOpenSound(menuId: String): MenuSound? {
        // 複数プロバイダが登録されている場合は最初に見つかった設定を採用（登録順は保証しない）
        return providers.values.firstNotNullOfOrNull { it.openSound(menuId) }
    }

    private fun playSound(player: Player, sound: MenuSound) {
        val resolved = resolveSound(sound.sound) ?: return
        player.playSound(player.location, resolved, sound.volume, sound.pitch)
    }

    /**
     * Sound を enum 名（"UI_BUTTON_CLICK"）または NamespacedKey（"minecraft:ui.button.click"）で解決する。
     * 解決失敗時は null を返し、再生をスキップする。
     */
    private fun resolveSound(name: String): Sound? {
        val raw = name.trim()
        val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT)
        val candidateKeys = buildList {
            NamespacedKey.fromString(raw.lowercase())?.let(::add)
            if (!raw.contains(":")) {
                add(NamespacedKey.minecraft(raw.lowercase().replace('_', '.')))
            }
        }
        return candidateKeys.firstNotNullOfOrNull { registry.get(it) }
    }
}
