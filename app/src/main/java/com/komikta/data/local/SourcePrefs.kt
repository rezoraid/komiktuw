package com.komikta.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SourceChoice(val source: String, val lang: String)

data class SourceOption(val id: String, val label: String)

data class LangOption(val code: String, val label: String)

/**
 * Semua sumber (Lumos, MANGA Plus, ...) digabung jadi satu daftar: [source] selalu [ALL].
 * Yang bisa dipilih pengguna hanya bahasa baca MANGA Plus, lewat halaman Detail.
 */
@Singleton
class SourcePrefs @Inject constructor(@ApplicationContext context: Context) {
    private val sp = context.getSharedPreferences("source_prefs", Context.MODE_PRIVATE)
    private val _choice = MutableStateFlow(
        SourceChoice(
            source = ALL,
            lang = sp.getString("lang", null)?.takeIf { l -> LANGUAGES.any { it.code == l } } ?: DEFAULT_LANG
        )
    )
    val choice: StateFlow<SourceChoice> = _choice.asStateFlow()
    val source: String get() = ALL

    fun langFor(source: String): String? = if (source == MANGAPLUS) _choice.value.lang else null

    fun selectLang(lang: String) {
        if (lang == _choice.value.lang || LANGUAGES.none { it.code == lang }) return
        sp.edit().putString("lang", lang).apply()
        _choice.value = SourceChoice(ALL, lang)
    }

    companion object {
        /** Id gabungan: Home / Explore / pencarian mengambil dari semua sumber sekaligus. */
        const val ALL = "all"
        const val MANGAPLUS = "mangaplus"
        const val DEFAULT_LANG = "eng"
        val SOURCES = listOf(SourceOption("lumos", "Lumos"), SourceOption(MANGAPLUS, "MANGA Plus"))
        val LANGUAGES = listOf(
            LangOption("eng", "English"), LangOption("ind", "Indonesia"), LangOption("esp", "Español"),
            LangOption("ptb", "Português (BR)"), LangOption("fra", "Français"), LangOption("deu", "Deutsch"),
            LangOption("rus", "Русский"), LangOption("tha", "ไทย"), LangOption("vie", "Tiếng Việt")
        )

        fun languageLabel(code: String): String = LANGUAGES.firstOrNull { it.code == code }?.label ?: code
    }
}
