package com.awabi2048.ccsystem.api

data class I18nValidationResult(
    val errors: List<String>,
    val errorsByFeature: Map<String, List<String>> = emptyMap()
) {
    val hasErrors: Boolean = errors.isNotEmpty()

    fun errorsFor(feature: String): List<String> {
        return errorsByFeature[feature].orEmpty()
    }
}
