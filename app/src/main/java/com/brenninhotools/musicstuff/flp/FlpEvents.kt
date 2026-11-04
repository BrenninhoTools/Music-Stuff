package com.brenninhotools.musicstuff.flp

internal object FlpEvents {
    const val WORD_START = 64
    const val DWORD_START = 128
    const val TEXT_START = 192

    const val CHANNEL_TYPE = 21
    const val TIME_SIG_NUMERATOR = 17
    const val TIME_SIG_DENOMINATOR = 18

    const val CHANNEL_NEW = 64
    const val PATTERN_NEW = 65
    const val TEMPO_COARSE = 66
    const val TEMPO_FINE = 93
    const val ARRANGEMENT_NEW = 99

    const val TEMPO = 156
    const val SHORT_DWORD = 172

    const val CHANNEL_NAME_LEGACY = 192
    const val PATTERN_NAME = 193
    const val TITLE = 194
    const val FL_VERSION = 199
    const val PLUGIN_INTERNAL_NAME = 201
    const val PLUGIN_NAME = 203
    const val PATTERN_NOTES = 224
    const val MIXER_PARAMS = 225
    const val ARRANGEMENT_PLAYLIST = 233
    const val INSERT_FLAGS = 236
    const val ARRANGEMENT_NAME = 241
}
