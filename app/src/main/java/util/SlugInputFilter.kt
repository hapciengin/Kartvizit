package com.qrtasima.util

import android.text.InputFilter
import android.text.Spanned

class SlugInputFilter : InputFilter {
    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        for (i in start until end) {
            val char = source[i]
            if (!Character.isLetterOrDigit(char) && char != '-') {
                return ""
            }
        }
        return null
    }
}