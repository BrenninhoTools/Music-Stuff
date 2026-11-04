package com.brenninhotools.musicstuff.generate

enum class SongPart(val label: String) {
    KICK("Kick"),
    SNARE("Snare"),
    HATS("Hats"),
    BASS("Bass"),
    CHORDS("Chords"),
    MELODY("Melody"),
}

enum class SongStyle(
    val label: String,
    val defaultBpm: Int,
    val minor: Boolean,
    val sevenths: Boolean,
    val density: Double,
    val kick: String,
    val snare: String,
    val hats: String,
    val bass: String,
    val progressions: List<List<Int>>,
) {
    FUNKIN(
        label = "Funkin'",
        defaultBpm = 160,
        minor = true,
        sevenths = false,
        density = 0.7,
        kick = "x.......x.x.....",
        snare = "....x.......x...",
        hats = "x.x.x.x.x.x.x.x.",
        bass = "x..x..x.x..x..x.",
        progressions = listOf(listOf(0, 5, 2, 6), listOf(0, 3, 5, 4), listOf(0, 6, 5, 6)),
    ),
    TRAP(
        label = "Trap",
        defaultBpm = 140,
        minor = true,
        sevenths = false,
        density = 0.45,
        kick = "x.....x...x.....",
        snare = "........x.......",
        hats = "x.x.x.xxx.x.xxxx",
        bass = "x.....x...x.....",
        progressions = listOf(listOf(0, 0, 5, 4), listOf(0, 3, 0, 6), listOf(0, 5, 3, 4)),
    ),
    HOUSE(
        label = "House",
        defaultBpm = 124,
        minor = false,
        sevenths = true,
        density = 0.55,
        kick = "x...x...x...x...",
        snare = "....x.......x...",
        hats = "..x...x...x...x.",
        bass = "..x...x...x.x.x.",
        progressions = listOf(listOf(5, 3, 0, 4), listOf(0, 4, 5, 3), listOf(1, 4, 0, 5)),
    ),
    LOFI(
        label = "Lo-fi",
        defaultBpm = 82,
        minor = false,
        sevenths = true,
        density = 0.35,
        kick = "x.....x..x......",
        snare = "....x.......x...",
        hats = "x.x.x.x.x.x.x.x.",
        bass = "x.....x.....x...",
        progressions = listOf(listOf(1, 4, 0, 5), listOf(0, 5, 1, 4), listOf(3, 2, 1, 4)),
    ),
    CHIPTUNE(
        label = "Chiptune",
        defaultBpm = 150,
        minor = false,
        sevenths = false,
        density = 0.85,
        kick = "x...x.x.x...x...",
        snare = "....x.......x..x",
        hats = "xxxxxxxxxxxxxxxx",
        bass = "x.x.x.x.x.x.x.x.",
        progressions = listOf(listOf(0, 4, 5, 3), listOf(0, 3, 4, 3), listOf(5, 3, 0, 4)),
    ),
}

val KEY_NAMES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

val SONG_LENGTHS = listOf(8, 16, 32)

data class SongSettings(
    val style: SongStyle = SongStyle.FUNKIN,
    val root: Int = 9,
    val bpm: Int = SongStyle.FUNKIN.defaultBpm,
    val bars: Int = 16,
    val seed: Long = 1L,
    val partSeeds: Map<SongPart, Long> = emptyMap(),
) {
    val keyLabel: String get() = "${KEY_NAMES[root]} ${if (style.minor) "minor" else "major"}"
    val title: String get() = "${style.label} in $keyLabel"
}
