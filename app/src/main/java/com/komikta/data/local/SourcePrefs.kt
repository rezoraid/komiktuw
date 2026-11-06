package com.komikta.data.local

import android.content.Context
import com.komikta.core.network.ApiConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SourceChoice(val source: String, val lang: String)

data class SourceOption(val id: String, val label: String)

data class LangOption(val code: String, val label: String)

@Singleton
class SourcePrefs @Inject constructor(@ApplicationContext context: Context) {
    private val sp = context.getSharedPreferences("source_prefs", Context.MODE_PRIVATE)
    private val _choice = MutableStateFlow(
        SourceChoice(
            source = sp.getString("source", null)?.takeIf { s -> SOURCES.any { it.id == s } } ?: ApiConfig.DEFAULT_SOURCE,
            lang = sp.getString("lang", null)?.takeIf { l -> LANGUAGES.any { it.code == l } } ?: DEFAULT_LANG
        )
    )
    val choice: StateFlow<SourceChoice> = _choice.asStateFlow()
    val source: String get() = _choice.value.source

    fun langFor(source: String): String? = if (source == MANGAPLUS) _choice.value.lang else null

    fun select(source: String, lang: String) {
        val next = SourceChoice(source, lang)
        if (next == _choice.value) return
        sp.edit().putString("source", source).putString("lang", lang).apply()
        _choice.value = next
    }

    companion object {
        const val MANGAPLUS = "mangaplus"
        fun languageLabel(code: String): String = LANGUAGES.firstOrNull { it.code == code }?.label ?: code
        const val DEFAULT_LANG = "eng"
        val SOURCES = listOf(SourceOption("lumos", "Lumos"), SourceOption(MANGAPLUS, "MANGA Plus"))
        val LANGUAGES = listOf(
            LangOption("eng", "English"), LangOption("ind", "Indonesia"), LangOption("esp", "Español"),
            LangOption("ptb", "Português (BR)"), LangOption("fra", "Français"), LangOption("deu", "Deutsch"),
            LangOption("rus", "Русский"), LangOption("tha", "ไทย"), LangOption("vie", "Tiếng Việt")
        )
    }
}
