package com.jummania

import androidx.annotation.Keep


/**
 * Defines different visual styles for chess piece symbols.
 *
 * @property value Integer code associated with each symbol style.
 *
 * Created by Jummania on 11/04/2025
 * Email: sharifuddinjumman@gmail.com
 * Dhaka, Bangladesh
 */
@Keep
enum class SymbolStyle(private val value: Int) {

    /** System default or standard font. */
    STANDARD(0),

    /** Classic style using chess_alpha.ttf font. */
    CLASSIC(1),

    /** Merida style using chess_merida_unicode.ttf font. */
    MERIDA(2),

    /** Symbola style using symbola.ttf font. */
    SYMBOLA(3);

    internal companion object {

        /**
         * Returns the [SymbolStyle] corresponding to the given [value].
         * Defaults to [STANDARD] if not found.
         */
        @Keep
        fun fromInt(value: Int): SymbolStyle {
            return when (value) {
                1 -> CLASSIC
                2 -> MERIDA
                3 -> SYMBOLA
                else -> STANDARD
            }
        }
    }
}