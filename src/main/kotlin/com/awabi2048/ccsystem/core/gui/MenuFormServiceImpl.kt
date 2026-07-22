package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuActionResult
import com.awabi2048.ccsystem.api.gui.MenuActionSoundPolicy
import com.awabi2048.ccsystem.api.gui.MenuClickType
import com.awabi2048.ccsystem.api.gui.MenuCustomFormRequest
import com.awabi2048.ccsystem.api.gui.MenuFormInput
import com.awabi2048.ccsystem.api.gui.MenuFormResponse
import com.awabi2048.ccsystem.api.gui.MenuFormService
import com.awabi2048.ccsystem.api.gui.MenuSimpleFormRequest
import com.awabi2048.ccsystem.api.gui.MenuSoundPolicy
import com.awabi2048.ccsystem.api.gui.MenuSoundService
import com.awabi2048.ccsystem.api.gui.MenuUpdate
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.geysermc.cumulus.form.CustomForm
import org.geysermc.cumulus.form.SimpleForm
import org.geysermc.cumulus.util.FormImage
import org.geysermc.floodgate.api.FloodgateApi

internal class MenuFormServiceImpl(
    private val plugin: JavaPlugin,
    private val sounds: MenuSoundService,
    private val runtime: MenuRuntimeServiceImpl
) : MenuFormService {
    override fun isAvailable(player: Player): Boolean =
        Bukkit.getPluginManager().isPluginEnabled("floodgate") &&
            runCatching { FloodgateApi.getInstance().isFloodgatePlayer(player.uniqueId) }.getOrDefault(false)

    override fun show(player: Player, request: MenuSimpleFormRequest): Boolean {
        if (!isAvailable(player) || request.buttons.isEmpty()) return false
        val builder = SimpleForm.builder().title(request.title).content(request.content)
        request.buttons.forEach { button ->
            if (button.imagePath.isNullOrBlank()) builder.button(button.label) else {
                builder.button(button.label, FormImage.Type.PATH, button.imagePath)
            }
        }
        builder.validResultHandler { response ->
            onMainThread {
                val button = request.buttons.getOrNull(response.clickedButtonId()) ?: return@onMainThread
                val result = if (button.enabled) {
                    runCatching { request.handler.handle(player, MenuFormResponse(text = mapOf("button" to button.id))) }
                        .getOrElse { MenuActionResult.Rejected() }
                } else MenuActionResult.Rejected()
                applyResult(player, result, button.sound, request.sounds)
            }
        }
        val closeHandler = request.onClosed
        if (closeHandler != null) {
            builder.closedOrInvalidResultHandler(Runnable {
                onMainThread { applyResult(player, closeHandler.handle(player, MenuFormResponse()), request.sounds, request.sounds) }
            })
        }
        return runCatching { FloodgateApi.getInstance().sendForm(player.uniqueId, builder.build()) }.getOrDefault(false)
    }

    override fun show(player: Player, request: MenuCustomFormRequest): Boolean {
        if (!isAvailable(player) || request.inputs.isEmpty()) return false
        val builder = CustomForm.builder().title(request.title)
        request.inputs.forEach { input ->
            when (input) {
                is MenuFormInput.Text -> builder.input(input.label, input.placeholder, input.defaultValue)
                is MenuFormInput.Toggle -> builder.toggle(input.label, input.defaultValue)
            }
        }
        builder.validResultHandler { response ->
            onMainThread {
                val text = mutableMapOf<String, String>()
                val toggles = mutableMapOf<String, Boolean>()
                request.inputs.forEachIndexed { index, input ->
                    when (input) {
                        is MenuFormInput.Text -> text[input.id] = response.asInput(index).orEmpty()
                        is MenuFormInput.Toggle -> toggles[input.id] = response.asToggle(index)
                    }
                }
                val result = runCatching { request.handler.handle(player, MenuFormResponse(text, toggles)) }
                    .getOrElse { MenuActionResult.Rejected() }
                applyResult(player, result, request.sounds, request.sounds)
            }
        }
        val closeHandler = request.onClosed
        if (closeHandler != null) {
            builder.closedOrInvalidResultHandler(Runnable {
                onMainThread { applyResult(player, closeHandler.handle(player, MenuFormResponse()), request.sounds, request.sounds) }
            })
        }
        return runCatching { FloodgateApi.getInstance().sendForm(player.uniqueId, builder.build()) }.getOrDefault(false)
    }

    private fun applyResult(
        player: Player,
        result: MenuActionResult,
        actionSounds: MenuActionSoundPolicy,
        requestSounds: MenuActionSoundPolicy
    ) {
        when (result) {
            is MenuActionResult.Success -> {
                play(player, result.sound, MenuSoundPolicyResolver.successPolicy(actionSounds, requestSounds))
                applyUpdate(player, result.update)
            }
            is MenuActionResult.Rejected -> {
                result.message?.let(player::sendMessage)
                play(player, result.sound, MenuSoundPolicyResolver.rejectedPolicy(actionSounds, requestSounds))
            }
            MenuActionResult.Ignored -> Unit
        }
    }

    private fun applyUpdate(player: Player, update: MenuUpdate) {
        when (update) {
            MenuUpdate.None, MenuUpdate.Close -> Unit
            MenuUpdate.Refresh -> Unit
            MenuUpdate.Back -> runtime.back(player)
            is MenuUpdate.Navigate -> runtime.open(player, update.route)
            is MenuUpdate.Replace -> runtime.open(player, update.route)
        }
    }

    private fun play(player: Player, policy: MenuSoundPolicy, fallback: MenuSoundPolicy) {
        val sound = MenuSoundPolicyResolver.resolve(policy, fallback, MenuClickType.DEFAULT) ?: return
        sounds.play(player, sound)
    }

    private fun onMainThread(action: () -> Unit) {
        Bukkit.getScheduler().runTask(plugin, Runnable(action))
    }
}
