package com.awabi2048.ccsystem.api.localization.generated

import com.awabi2048.ccsystem.api.localization.LocalizationKey

/** コンパイル時に値型を保証する、領域別の生成済みローカライズキーです。 */
object ContentBreweryKeys {
    @JvmField val BREWERY_ITEM_NAME_WORT: LocalizationKey<String> = LocalizationKey.text("brewery.item.name.wort", setOf("recipe"))
    @JvmField val BREWERY_ITEM_NAME_MUDDY: LocalizationKey<String> = LocalizationKey.text("brewery.item.name.muddy", setOf("recipe"))
    @JvmField val BREWERY_ITEM_NAME_FERMENTED: LocalizationKey<String> = LocalizationKey.text("brewery.item.name.fermented", setOf("product"))
    @JvmField val BREWERY_ITEM_NAME_DISTILLED: LocalizationKey<String> = LocalizationKey.text("brewery.item.name.distilled", setOf("product"))
    @JvmField val BREWERY_ITEM_NAME_DISTILLED_DEGRADED: LocalizationKey<String> = LocalizationKey.text("brewery.item.name.distilled_degraded", setOf("product"))
    @JvmField val BREWERY_ITEM_NAME_AGED: LocalizationKey<String> = LocalizationKey.text("brewery.item.name.aged", setOf("product", "stars"))
    @JvmField val BREWERY_ITEM_DATA_RECIPE: LocalizationKey<String> = LocalizationKey.text("brewery.item.data.recipe", setOf())
    @JvmField val BREWERY_ITEM_DATA_STAGE: LocalizationKey<String> = LocalizationKey.text("brewery.item.data.stage", setOf())
    @JvmField val BREWERY_ITEM_DATA_QUALITY: LocalizationKey<String> = LocalizationKey.text("brewery.item.data.quality", setOf())
    @JvmField val BREWERY_ITEM_DATA_FINAL_QUALITY: LocalizationKey<String> = LocalizationKey.text("brewery.item.data.final_quality", setOf())
    @JvmField val BREWERY_ITEM_DATA_DISTILL_COUNT: LocalizationKey<String> = LocalizationKey.text("brewery.item.data.distill_count", setOf())
    @JvmField val BREWERY_ITEM_DATA_ALCOHOL: LocalizationKey<String> = LocalizationKey.text("brewery.item.data.alcohol", setOf())
    @JvmField val BREWERY_ITEM_DATA_RATING: LocalizationKey<String> = LocalizationKey.text("brewery.item.data.rating", setOf())
    @JvmField val BREWERY_ITEM_DATA_NEXT_STAGE: LocalizationKey<String> = LocalizationKey.text("brewery.item.data.next_stage", setOf())
    @JvmField val BREWERY_ITEM_STAGE_WORT: LocalizationKey<String> = LocalizationKey.text("brewery.item.stage.wort", setOf())
    @JvmField val BREWERY_ITEM_STAGE_FERMENTED: LocalizationKey<String> = LocalizationKey.text("brewery.item.stage.fermented", setOf())
    @JvmField val BREWERY_ITEM_STAGE_DISTILLED: LocalizationKey<String> = LocalizationKey.text("brewery.item.stage.distilled", setOf())
    @JvmField val BREWERY_ITEM_STAGE_AGED: LocalizationKey<String> = LocalizationKey.text("brewery.item.stage.aged", setOf())
    @JvmField val BREWERY_ITEM_STAGE_FAILED: LocalizationKey<String> = LocalizationKey.text("brewery.item.stage.failed", setOf())
    @JvmField val BREWERY_ITEM_NEXT_STAGE_FERMENTATION: LocalizationKey<String> = LocalizationKey.text("brewery.item.next_stage.fermentation", setOf())
    @JvmField val BREWERY_ITEM_NEXT_STAGE_DISTILLATION: LocalizationKey<String> = LocalizationKey.text("brewery.item.next_stage.distillation", setOf())
    @JvmField val BREWERY_ITEM_NEXT_STAGE_AGING: LocalizationKey<String> = LocalizationKey.text("brewery.item.next_stage.aging", setOf())
    @JvmField val BREWERY_ITEM_NEXT_STAGE_COMPLETED: LocalizationKey<String> = LocalizationKey.text("brewery.item.next_stage.completed", setOf())
    @JvmField val BREWERY_ITEM_NEXT_STAGE_NONE: LocalizationKey<String> = LocalizationKey.text("brewery.item.next_stage.none", setOf())
    @JvmField val BREWERY_ITEM_FILTER_NAME: LocalizationKey<String> = LocalizationKey.text("brewery.item.filter.name", setOf())
    @JvmField val BREWERY_ITEM_FILTER_DESCRIPTION: LocalizationKey<List<String>> = LocalizationKey.textList("brewery.item.filter.description", setOf())
    @JvmField val BREWERY_DRINK_COMPLETED: LocalizationKey<String> = LocalizationKey.text("brewery.drink.completed", setOf("alcohol", "recipe"))
    @JvmField val BREWERY_CATALOG_TITLE: LocalizationKey<String> = LocalizationKey.text("brewery.catalog.title", setOf("page", "pages"))
    @JvmField val BREWERY_CATALOG_DISCOVERED: LocalizationKey<String> = LocalizationKey.text("brewery.catalog.discovered", setOf("recipe"))
    @JvmField val BREWERY_PROCESS_PREPARATION_STARTED: LocalizationKey<String> = LocalizationKey.text("brewery.process.preparation_started", setOf("recipe"))
    @JvmField val BREWERY_PROCESS_FERMENTATION_STARTED: LocalizationKey<String> = LocalizationKey.text("brewery.process.fermentation_started", setOf("recipe"))
    @JvmField val BREWERY_PROCESS_DISTILLATION_STARTED: LocalizationKey<String> = LocalizationKey.text("brewery.process.distillation_started", setOf())
    @JvmField val BREWERY_PROCESS_DISTILLATION_STOPPED: LocalizationKey<String> = LocalizationKey.text("brewery.process.distillation_stopped", setOf())
    @JvmField val BREWERY_PROCESS_BOTTLED: LocalizationKey<String> = LocalizationKey.text("brewery.process.bottled", setOf())
    @JvmField val BREWERY_PROCESS_AGED: LocalizationKey<String> = LocalizationKey.text("brewery.process.aged", setOf())
    @JvmField val BREWERY_PROCESS_BARREL_REGISTERED: LocalizationKey<String> = LocalizationKey.text("brewery.process.barrel_registered", setOf("size", "wood"))
    @JvmField val BREWERY_PROCESS_FERMENTATION_BARREL_REGISTERED: LocalizationKey<String> = LocalizationKey.text("brewery.process.fermentation_barrel_registered", setOf())
    @JvmField val BREWERY_ERROR_NO_YEAST: LocalizationKey<String> = LocalizationKey.text("brewery.error.no_yeast", setOf())
    @JvmField val BREWERY_ERROR_ALREADY_FERMENTING: LocalizationKey<String> = LocalizationKey.text("brewery.error.already_fermenting", setOf())
    @JvmField val BREWERY_ERROR_FERMENTATION_NOT_READY: LocalizationKey<String> = LocalizationKey.text("brewery.error.fermentation_not_ready", setOf())
    @JvmField val BREWERY_ERROR_AGING_NOT_READY: LocalizationKey<String> = LocalizationKey.text("brewery.error.aging_not_ready", setOf())
    @JvmField val BREWERY_ERROR_FERMENTATION_BARREL_REQUIRED: LocalizationKey<String> = LocalizationKey.text("brewery.error.fermentation_barrel_required", setOf())
    @JvmField val BREWERY_ERROR_NO_INGREDIENTS: LocalizationKey<String> = LocalizationKey.text("brewery.error.no_ingredients", setOf())
    @JvmField val BREWERY_ERROR_RECIPE_NOT_FOUND: LocalizationKey<String> = LocalizationKey.text("brewery.error.recipe_not_found", setOf())
    @JvmField val BREWERY_ERROR_INVALID_INPUT: LocalizationKey<String> = LocalizationKey.text("brewery.error.invalid_input", setOf())
    @JvmField val BREWERY_ERROR_NO_MATERIAL: LocalizationKey<String> = LocalizationKey.text("brewery.error.no_material", setOf())
    @JvmField val BREWERY_ERROR_SKILL_REQUIRED: LocalizationKey<String> = LocalizationKey.text("brewery.error.skill_required", setOf())
    @JvmField val BREWERY_ERROR_BARREL_SIZE: LocalizationKey<String> = LocalizationKey.text("brewery.error.barrel_size", setOf())
    @JvmField val BREWERY_ERROR_MACHINE_LOCKED: LocalizationKey<String> = LocalizationKey.text("brewery.error.machine_locked", setOf())
    @JvmField val BREWERY_ERROR_LEVEL_REQUIRED: LocalizationKey<String> = LocalizationKey.text("brewery.error.level_required", setOf("level"))
    @JvmField val BREWERY_ERROR_SKILLS_REQUIRED: LocalizationKey<String> = LocalizationKey.text("brewery.error.skills_required", setOf("skills"))
    @JvmField val BREWERY_ERROR_NOT_STARTED: LocalizationKey<String> = LocalizationKey.text("brewery.error.not_started", setOf())
    @JvmField val BREWERY_ERROR_BATCH_LIMIT: LocalizationKey<String> = LocalizationKey.text("brewery.error.batch_limit", setOf("capacity"))
    @JvmField val BREWERY_ERROR_SINGLE_ITEM: LocalizationKey<String> = LocalizationKey.text("brewery.error.single_item", setOf())
    @JvmField val BREWERY_ERROR_BARREL_TYPE: LocalizationKey<String> = LocalizationKey.text("brewery.error.barrel_type", setOf("types"))
    @JvmField val BREWERY_ERROR_INVALID_SLOT: LocalizationKey<String> = LocalizationKey.text("brewery.error.invalid_slot", setOf())
    @JvmField val BREWERY_ERROR_BARREL_STRUCTURE: LocalizationKey<String> = LocalizationKey.text("brewery.error.barrel_structure", setOf("actual", "expected", "location"))
    @JvmField val BREWERY_ERROR_BARREL_OVERLAP: LocalizationKey<String> = LocalizationKey.text("brewery.error.barrel_overlap", setOf())
    @JvmField val BREWERY_UI_TITLE_FERMENTATION: LocalizationKey<String> = LocalizationKey.text("brewery.ui.title.fermentation", setOf())
    @JvmField val BREWERY_UI_TITLE_DISTILLATION: LocalizationKey<String> = LocalizationKey.text("brewery.ui.title.distillation", setOf())
    @JvmField val BREWERY_UI_TITLE_AGING_BIG: LocalizationKey<String> = LocalizationKey.text("brewery.ui.title.aging_big", setOf())
    @JvmField val BREWERY_UI_TITLE_AGING_SMALL: LocalizationKey<String> = LocalizationKey.text("brewery.ui.title.aging_small", setOf())
    @JvmField val BREWERY_UI_ACTION_FERMENTATION_START: LocalizationKey<String> = LocalizationKey.text("brewery.ui.action.fermentation_start", setOf())
    @JvmField val BREWERY_UI_ACTION_FERMENTATION_RUNNING: LocalizationKey<String> = LocalizationKey.text("brewery.ui.action.fermentation_running", setOf())
    @JvmField val BREWERY_UI_ACTION_DISTILLATION_START: LocalizationKey<String> = LocalizationKey.text("brewery.ui.action.distillation_start", setOf())
    @JvmField val BREWERY_UI_ACTION_DISTILLATION_STOP: LocalizationKey<String> = LocalizationKey.text("brewery.ui.action.distillation_stop", setOf())
    @JvmField val BREWERY_UI_ACTION_START: LocalizationKey<String> = LocalizationKey.text("brewery.ui.action.start", setOf())
    @JvmField val BREWERY_UI_ACTION_STOP: LocalizationKey<String> = LocalizationKey.text("brewery.ui.action.stop", setOf())
    @JvmField val BREWERY_UI_DATA_CURRENT: LocalizationKey<String> = LocalizationKey.text("brewery.ui.data.current", setOf())
    @JvmField val BREWERY_UI_DATA_NONE: LocalizationKey<String> = LocalizationKey.text("brewery.ui.data.none", setOf())
    @JvmField val BREWERY_UI_DATA_STEP_ELAPSED: LocalizationKey<String> = LocalizationKey.text("brewery.ui.data.step_elapsed", setOf())
    @JvmField val BREWERY_UI_DATA_SESSION_RUNS: LocalizationKey<String> = LocalizationKey.text("brewery.ui.data.session_runs", setOf())
    @JvmField val BREWERY_UI_DATA_DURATION: LocalizationKey<String> = LocalizationKey.text("brewery.ui.data.duration", setOf())
    @JvmField val BREWERY_UI_DATA_ITEMS: LocalizationKey<String> = LocalizationKey.text("brewery.ui.data.items", setOf())
    @JvmField val BREWERY_UI_BARREL_BIG: LocalizationKey<String> = LocalizationKey.text("brewery.ui.barrel.big", setOf())
    @JvmField val BREWERY_UI_BARREL_SMALL: LocalizationKey<String> = LocalizationKey.text("brewery.ui.barrel.small", setOf())
    @JvmField val BREWERY_UI_AGING_CORE: LocalizationKey<String> = LocalizationKey.text("brewery.ui.aging_core", setOf())
    @JvmField val BREWERY_INTOXICATION_STATUS: LocalizationKey<String> = LocalizationKey.text("brewery.intoxication.status", setOf("alcohol"))

    internal fun all(): List<LocalizationKey<*>> = listOf(
        BREWERY_ITEM_NAME_WORT,
        BREWERY_ITEM_NAME_MUDDY,
        BREWERY_ITEM_NAME_FERMENTED,
        BREWERY_ITEM_NAME_DISTILLED,
        BREWERY_ITEM_NAME_DISTILLED_DEGRADED,
        BREWERY_ITEM_NAME_AGED,
        BREWERY_ITEM_DATA_RECIPE,
        BREWERY_ITEM_DATA_STAGE,
        BREWERY_ITEM_DATA_QUALITY,
        BREWERY_ITEM_DATA_FINAL_QUALITY,
        BREWERY_ITEM_DATA_DISTILL_COUNT,
        BREWERY_ITEM_DATA_ALCOHOL,
        BREWERY_ITEM_DATA_RATING,
        BREWERY_ITEM_DATA_NEXT_STAGE,
        BREWERY_ITEM_STAGE_WORT,
        BREWERY_ITEM_STAGE_FERMENTED,
        BREWERY_ITEM_STAGE_DISTILLED,
        BREWERY_ITEM_STAGE_AGED,
        BREWERY_ITEM_STAGE_FAILED,
        BREWERY_ITEM_NEXT_STAGE_FERMENTATION,
        BREWERY_ITEM_NEXT_STAGE_DISTILLATION,
        BREWERY_ITEM_NEXT_STAGE_AGING,
        BREWERY_ITEM_NEXT_STAGE_COMPLETED,
        BREWERY_ITEM_NEXT_STAGE_NONE,
        BREWERY_ITEM_FILTER_NAME,
        BREWERY_ITEM_FILTER_DESCRIPTION,
        BREWERY_DRINK_COMPLETED,
        BREWERY_CATALOG_TITLE,
        BREWERY_CATALOG_DISCOVERED,
        BREWERY_PROCESS_PREPARATION_STARTED,
        BREWERY_PROCESS_FERMENTATION_STARTED,
        BREWERY_PROCESS_DISTILLATION_STARTED,
        BREWERY_PROCESS_DISTILLATION_STOPPED,
        BREWERY_PROCESS_BOTTLED,
        BREWERY_PROCESS_AGED,
        BREWERY_PROCESS_BARREL_REGISTERED,
        BREWERY_PROCESS_FERMENTATION_BARREL_REGISTERED,
        BREWERY_ERROR_NO_YEAST,
        BREWERY_ERROR_ALREADY_FERMENTING,
        BREWERY_ERROR_FERMENTATION_NOT_READY,
        BREWERY_ERROR_AGING_NOT_READY,
        BREWERY_ERROR_FERMENTATION_BARREL_REQUIRED,
        BREWERY_ERROR_NO_INGREDIENTS,
        BREWERY_ERROR_RECIPE_NOT_FOUND,
        BREWERY_ERROR_INVALID_INPUT,
        BREWERY_ERROR_NO_MATERIAL,
        BREWERY_ERROR_SKILL_REQUIRED,
        BREWERY_ERROR_BARREL_SIZE,
        BREWERY_ERROR_MACHINE_LOCKED,
        BREWERY_ERROR_LEVEL_REQUIRED,
        BREWERY_ERROR_SKILLS_REQUIRED,
        BREWERY_ERROR_NOT_STARTED,
        BREWERY_ERROR_BATCH_LIMIT,
        BREWERY_ERROR_SINGLE_ITEM,
        BREWERY_ERROR_BARREL_TYPE,
        BREWERY_ERROR_INVALID_SLOT,
        BREWERY_ERROR_BARREL_STRUCTURE,
        BREWERY_ERROR_BARREL_OVERLAP,
        BREWERY_UI_TITLE_FERMENTATION,
        BREWERY_UI_TITLE_DISTILLATION,
        BREWERY_UI_TITLE_AGING_BIG,
        BREWERY_UI_TITLE_AGING_SMALL,
        BREWERY_UI_ACTION_FERMENTATION_START,
        BREWERY_UI_ACTION_FERMENTATION_RUNNING,
        BREWERY_UI_ACTION_DISTILLATION_START,
        BREWERY_UI_ACTION_DISTILLATION_STOP,
        BREWERY_UI_ACTION_START,
        BREWERY_UI_ACTION_STOP,
        BREWERY_UI_DATA_CURRENT,
        BREWERY_UI_DATA_NONE,
        BREWERY_UI_DATA_STEP_ELAPSED,
        BREWERY_UI_DATA_SESSION_RUNS,
        BREWERY_UI_DATA_DURATION,
        BREWERY_UI_DATA_ITEMS,
        BREWERY_UI_BARREL_BIG,
        BREWERY_UI_BARREL_SMALL,
        BREWERY_UI_AGING_CORE,
        BREWERY_INTOXICATION_STATUS,
    )
}
