package com.awabi2048.ccsystem.api.localization.generated

import com.awabi2048.ccsystem.api.localization.LocalizationKey

/** コンパイル時に値型を保証する、領域別の生成済みローカライズキーです。 */
object SystemManagementKeys {
    @JvmField val MANAGEMENT_USAGE: LocalizationKey<String> = LocalizationKey.text("management.usage", setOf())
    @JvmField val MANAGEMENT_NO_PERMISSION: LocalizationKey<String> = LocalizationKey.text("management.no_permission", setOf())
    @JvmField val MANAGEMENT_PLAYER_NOT_FOUND: LocalizationKey<String> = LocalizationKey.text("management.player_not_found", setOf("player"))
    @JvmField val MANAGEMENT_CONFIG_USAGE: LocalizationKey<String> = LocalizationKey.text("management.config.usage", setOf())
    @JvmField val MANAGEMENT_CONFIG_NONE: LocalizationKey<String> = LocalizationKey.text("management.config.none", setOf())
    @JvmField val MANAGEMENT_CONFIG_LINE: LocalizationKey<String> = LocalizationKey.text("management.config.line", setOf("color", "detected", "file", "message", "owner", "required", "state"))
    @JvmField val MANAGEMENT_GIVE_USAGE: LocalizationKey<String> = LocalizationKey.text("management.give.usage", setOf())
    @JvmField val MANAGEMENT_GIVE_SUCCESS: LocalizationKey<String> = LocalizationKey.text("management.give.success", setOf("dropped", "granted", "item", "player"))
    @JvmField val MANAGEMENT_GIVE_FAILED: LocalizationKey<String> = LocalizationKey.text("management.give.failed", setOf("item", "player", "reason"))
    @JvmField val MANAGEMENT_MENU_USAGE: LocalizationKey<String> = LocalizationKey.text("management.menu.usage", setOf())
    @JvmField val MANAGEMENT_MENU_UNKNOWN: LocalizationKey<String> = LocalizationKey.text("management.menu.unknown", setOf("menu"))
    @JvmField val MANAGEMENT_MENU_TARGET_REQUIRED: LocalizationKey<String> = LocalizationKey.text("management.menu.target_required", setOf())
    @JvmField val MANAGEMENT_MENU_SELF_ONLY: LocalizationKey<String> = LocalizationKey.text("management.menu.self_only", setOf())
    @JvmField val MANAGEMENT_MENU_INVALID_ARGUMENT: LocalizationKey<String> = LocalizationKey.text("management.menu.invalid_argument", setOf("argument"))
    @JvmField val MANAGEMENT_MENU_SUCCESS: LocalizationKey<String> = LocalizationKey.text("management.menu.success", setOf("menu", "player"))
    @JvmField val MANAGEMENT_MENU_FAILED: LocalizationKey<String> = LocalizationKey.text("management.menu.failed", setOf("menu", "player"))
    @JvmField val MANAGEMENT_PARTICLE_USAGE: LocalizationKey<String> = LocalizationKey.text("management.particle.usage", setOf())
    @JvmField val MANAGEMENT_PARTICLE_TYPE_GLOBAL: LocalizationKey<String> = LocalizationKey.text("management.particle.type.global", setOf())
    @JvmField val MANAGEMENT_PARTICLE_TYPE_OWNER: LocalizationKey<String> = LocalizationKey.text("management.particle.type.owner", setOf())
    @JvmField val MANAGEMENT_PARTICLE_TYPE_PER_TICK: LocalizationKey<String> = LocalizationKey.text("management.particle.type.per-tick", setOf())
    @JvmField val MANAGEMENT_PARTICLE_TYPE_EMISSION: LocalizationKey<String> = LocalizationKey.text("management.particle.type.emission", setOf())
    @JvmField val MANAGEMENT_PARTICLE_STATUS_HEADER: LocalizationKey<String> = LocalizationKey.text("management.particle.status_header", setOf("used"))
    @JvmField val MANAGEMENT_PARTICLE_STATUS_LINE: LocalizationKey<String> = LocalizationKey.text("management.particle.status_line", setOf("limit", "maximum", "minimum", "type"))
    @JvmField val MANAGEMENT_PARTICLE_INVALID_LIMIT: LocalizationKey<String> = LocalizationKey.text("management.particle.invalid_limit", setOf("maximum", "minimum", "type"))
    @JvmField val MANAGEMENT_PARTICLE_SAVE_FAILED: LocalizationKey<String> = LocalizationKey.text("management.particle.save_failed", setOf())
    @JvmField val MANAGEMENT_PARTICLE_CHANGED: LocalizationKey<String> = LocalizationKey.text("management.particle.changed", setOf("limit", "type"))
    @JvmField val MANAGEMENT_DEBUG_USAGE: LocalizationKey<String> = LocalizationKey.text("management.debug.usage", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_ENABLED: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_enabled", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_DISABLED: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_disabled", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_EMPTY_BOOK: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_empty_book", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_REJECTED: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_rejected", setOf("detail"))
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_INVALID_JSON: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_invalid_json", setOf("detail"))
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_CACHED: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_cached", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_NOT_CACHED: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_not_cached", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_HEADER: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_choice.header", setOf("field"))
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_BLOCK: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_choice.block", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_MOTION_STATIC: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_choice.motion.static", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_MOTION_INERTIAL: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_choice.motion.inertial", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_MOTION_BALLISTIC: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_choice.motion.ballistic", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_MOTION_BUOYANT: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_choice.motion.buoyant", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_MOTION_DRIFT: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_choice.motion.drift", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_MOTION_BURST: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_choice.motion.burst", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_MOTION_ORBIT: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_choice.motion.orbit", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_MOTION_ATTRACT: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_choice.motion.attract", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_COLLISION_NONE: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_choice.collision.none", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_COLLISION_REMOVE: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_choice.collision.remove", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_COLLISION_STOP: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_choice.collision.stop", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_COLLISION_SLIDE: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_choice.collision.slide", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_COLLISION_BOUNCE: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_choice.collision.bounce", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_VISIBILITY_NORMAL: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_choice.visibility.normal", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_VISIBILITY_FORCE: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_choice.visibility.force", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_OPEN_HOVER: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.open_hover", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_HEADER: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.header", setOf("genre", "page", "total"))
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_PREVIOUS: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.previous", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_NEXT: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.next", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_NAVIGATION_HOVER: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.navigation_hover", setOf("genre"))
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_TEXTURES_ENTRY_BLOCK: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.textures_entry_block", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_TEXTURES_ENTRY_WEIGHT: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.textures_entry_weight", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_SCALE_INITIAL: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.scale_initial", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_SCALE_PEAK: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.scale_peak", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_SCALE_PEAK_PROGRESS: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.scale_peak_progress", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_SCALE_SCALE_IN_TICKS: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.scale_scale_in_ticks", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_SCALE_VARIATION: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.scale_variation", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_ROTATION_RANDOM_INITIAL: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.rotation_random_initial", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_ROTATION_ANGULAR_VELOCITY: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.rotation_angular_velocity", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_ROTATION_VARIATION: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.rotation_variation", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_LIFETIME_TICKS: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.lifetime_ticks", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_LIFETIME_VARIATION: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.lifetime_variation", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_LIFETIME_FADE_OUT_TICKS: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.lifetime_fade_out_ticks", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_LIFETIME_FADE_VARIATION: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.lifetime_fade_variation", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_LIFETIME_SPAWN_DELAY: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.lifetime_spawn_delay", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_MOTION_PRESET: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.motion_preset", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_MOTION_INITIAL_VELOCITY: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.motion_initial_velocity", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_MOTION_ACCELERATION: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.motion_acceleration", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_MOTION_RETENTION: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.motion_retention", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_MOTION_TURBULENCE: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.motion_turbulence", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_MOTION_FREQUENCY: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.motion_frequency", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_MOTION_RADIAL_SPEED: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.motion_radial_speed", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_MOTION_SPAWN_RADIUS: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.motion_spawn_radius", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_MOTION_ORBIT_SPEED: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.motion_orbit_speed", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_MOTION_RADIAL_PULL: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.motion_radial_pull", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_MOTION_ATTRACTION: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.motion_attraction", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_MOTION_MAX_SPEED: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.motion_max_speed", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_COLLISION_MODE: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.collision_mode", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_COLLISION_RESTITUTION: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.collision_restitution", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_EMISSION_OFFSET: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.emission_offset", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_EMISSION_DELTA: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.emission_delta", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_EMISSION_SPEED: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.emission_speed", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_EMISSION_COUNT: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.emission_count", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_EMISSION_VISIBILITY: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_guide.field.emission_visibility", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_FIX_CONFIRM_REQUIRED: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_fix.confirm_required", setOf("fields"))
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_FIX_APPLY: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_fix.apply", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_FIX_APPLY_HOVER: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_fix.apply_hover", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_FIX_APPLIED: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_fix.applied", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_TEST_FIX_EXPIRED: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_test_fix.expired", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_SAMPLE_ITEM_NAME: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_sample_item.name", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_SAMPLE_ITEM_DESCRIPTION: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_sample_item.description", setOf())
    @JvmField val MANAGEMENT_DEBUG_PARTICLE_SAMPLE_ITEM_OPERATION: LocalizationKey<String> = LocalizationKey.text("management.debug.particle_sample_item.operation", setOf())

    internal fun all(): List<LocalizationKey<*>> = listOf(
        MANAGEMENT_USAGE,
        MANAGEMENT_NO_PERMISSION,
        MANAGEMENT_PLAYER_NOT_FOUND,
        MANAGEMENT_CONFIG_USAGE,
        MANAGEMENT_CONFIG_NONE,
        MANAGEMENT_CONFIG_LINE,
        MANAGEMENT_GIVE_USAGE,
        MANAGEMENT_GIVE_SUCCESS,
        MANAGEMENT_GIVE_FAILED,
        MANAGEMENT_MENU_USAGE,
        MANAGEMENT_MENU_UNKNOWN,
        MANAGEMENT_MENU_TARGET_REQUIRED,
        MANAGEMENT_MENU_SELF_ONLY,
        MANAGEMENT_MENU_INVALID_ARGUMENT,
        MANAGEMENT_MENU_SUCCESS,
        MANAGEMENT_MENU_FAILED,
        MANAGEMENT_PARTICLE_USAGE,
        MANAGEMENT_PARTICLE_TYPE_GLOBAL,
        MANAGEMENT_PARTICLE_TYPE_OWNER,
        MANAGEMENT_PARTICLE_TYPE_PER_TICK,
        MANAGEMENT_PARTICLE_TYPE_EMISSION,
        MANAGEMENT_PARTICLE_STATUS_HEADER,
        MANAGEMENT_PARTICLE_STATUS_LINE,
        MANAGEMENT_PARTICLE_INVALID_LIMIT,
        MANAGEMENT_PARTICLE_SAVE_FAILED,
        MANAGEMENT_PARTICLE_CHANGED,
        MANAGEMENT_DEBUG_USAGE,
        MANAGEMENT_DEBUG_PARTICLE_TEST_ENABLED,
        MANAGEMENT_DEBUG_PARTICLE_TEST_DISABLED,
        MANAGEMENT_DEBUG_PARTICLE_TEST_EMPTY_BOOK,
        MANAGEMENT_DEBUG_PARTICLE_TEST_REJECTED,
        MANAGEMENT_DEBUG_PARTICLE_TEST_INVALID_JSON,
        MANAGEMENT_DEBUG_PARTICLE_TEST_CACHED,
        MANAGEMENT_DEBUG_PARTICLE_TEST_NOT_CACHED,
        MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_HEADER,
        MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_BLOCK,
        MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_MOTION_STATIC,
        MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_MOTION_INERTIAL,
        MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_MOTION_BALLISTIC,
        MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_MOTION_BUOYANT,
        MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_MOTION_DRIFT,
        MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_MOTION_BURST,
        MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_MOTION_ORBIT,
        MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_MOTION_ATTRACT,
        MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_COLLISION_NONE,
        MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_COLLISION_REMOVE,
        MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_COLLISION_STOP,
        MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_COLLISION_SLIDE,
        MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_COLLISION_BOUNCE,
        MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_VISIBILITY_NORMAL,
        MANAGEMENT_DEBUG_PARTICLE_TEST_CHOICE_VISIBILITY_FORCE,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_OPEN_HOVER,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_HEADER,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_PREVIOUS,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_NEXT,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_NAVIGATION_HOVER,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_TEXTURES_ENTRY_BLOCK,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_TEXTURES_ENTRY_WEIGHT,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_SCALE_INITIAL,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_SCALE_PEAK,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_SCALE_PEAK_PROGRESS,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_SCALE_SCALE_IN_TICKS,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_SCALE_VARIATION,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_ROTATION_RANDOM_INITIAL,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_ROTATION_ANGULAR_VELOCITY,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_ROTATION_VARIATION,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_LIFETIME_TICKS,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_LIFETIME_VARIATION,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_LIFETIME_FADE_OUT_TICKS,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_LIFETIME_FADE_VARIATION,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_LIFETIME_SPAWN_DELAY,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_MOTION_PRESET,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_MOTION_INITIAL_VELOCITY,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_MOTION_ACCELERATION,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_MOTION_RETENTION,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_MOTION_TURBULENCE,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_MOTION_FREQUENCY,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_MOTION_RADIAL_SPEED,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_MOTION_SPAWN_RADIUS,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_MOTION_ORBIT_SPEED,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_MOTION_RADIAL_PULL,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_MOTION_ATTRACTION,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_MOTION_MAX_SPEED,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_COLLISION_MODE,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_COLLISION_RESTITUTION,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_EMISSION_OFFSET,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_EMISSION_DELTA,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_EMISSION_SPEED,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_EMISSION_COUNT,
        MANAGEMENT_DEBUG_PARTICLE_TEST_GUIDE_FIELD_EMISSION_VISIBILITY,
        MANAGEMENT_DEBUG_PARTICLE_TEST_FIX_CONFIRM_REQUIRED,
        MANAGEMENT_DEBUG_PARTICLE_TEST_FIX_APPLY,
        MANAGEMENT_DEBUG_PARTICLE_TEST_FIX_APPLY_HOVER,
        MANAGEMENT_DEBUG_PARTICLE_TEST_FIX_APPLIED,
        MANAGEMENT_DEBUG_PARTICLE_TEST_FIX_EXPIRED,
        MANAGEMENT_DEBUG_PARTICLE_SAMPLE_ITEM_NAME,
        MANAGEMENT_DEBUG_PARTICLE_SAMPLE_ITEM_DESCRIPTION,
        MANAGEMENT_DEBUG_PARTICLE_SAMPLE_ITEM_OPERATION,
    )
}
