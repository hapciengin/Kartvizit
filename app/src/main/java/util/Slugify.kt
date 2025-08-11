package com.qrtasima.util

import java.text.Normalizer
import java.util.Locale
import java.util.regex.Pattern

object Slugify {
    private val NONLATIN: Pattern = Pattern.compile("[^\\w-]")
    private val WHITESPACE: Pattern = Pattern.compile("[\\s]")

    fun from(input: String): String {
        var slug = input
            .replace("İ", "I")
            .replace("ı", "i")
            .replace("Ö", "O")
            .replace("ö", "o")
            .replace("Ü", "U")
            .replace("ü", "u")
            .replace("Ş", "S")
            .replace("ş", "s")
            .replace("Ç", "C")
            .replace("ç", "c")
            .replace("Ğ", "G")
            .replace("ğ", "g")

        val nowhitespace = WHITESPACE.matcher(slug).replaceAll("-")
        val normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
        slug = NONLATIN.matcher(normalized).replaceAll("")
        return slug.lowercase(Locale.ENGLISH)
    }
}