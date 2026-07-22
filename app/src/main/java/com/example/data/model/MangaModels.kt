package com.example.data.model

data class MangaItem(
    val id: Int,
    val titleRomaji: String,
    val titleEnglish: String?,
    val coverImage: String,
    val bannerImage: String?,
    val description: String,
    val status: String,
    val score: Int?,
    val genres: List<String>,
    val chapters: Int?,
    val volumes: Int?,
    val format: String?,
    val startYear: Int?
) {
    val displayTitle: String
        get() = titleEnglish?.takeIf { it.isNotBlank() } ?: titleRomaji
}

data class ChapterItem(
    val id: String,
    val title: String,
    val chapterNumber: String,
    val releaseDate: String? = null,
    val mangapillUrl: String? = null
)

data class ChapterPage(
    val pageNumber: Int,
    val imageUrl: String
)

enum class ReaderMode {
    VERTICAL_CONTINUOUS,
    HORIZONTAL_PAGED
}

enum class ReaderBackground {
    BLACK,
    WHITE,
    SEPIA
}
