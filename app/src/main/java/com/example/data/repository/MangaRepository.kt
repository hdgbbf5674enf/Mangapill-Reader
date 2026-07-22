package com.example.data.repository

import com.example.data.local.BookmarkEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.MangaDao
import com.example.data.model.ChapterItem
import com.example.data.model.ChapterPage
import com.example.data.model.MangaItem
import com.example.data.network.AnilistClient
import com.example.data.network.MangapillClient
import kotlinx.coroutines.flow.Flow

class MangaRepository(
    private val anilistClient: AnilistClient,
    private val mangapillClient: MangapillClient,
    private val mangaDao: MangaDao
) {
    // AniList API
    suspend fun getTrendingManga(page: Int = 1): List<MangaItem> {
        return anilistClient.getTrendingManga(page)
    }

    suspend fun getPopularManga(page: Int = 1): List<MangaItem> {
        return anilistClient.getPopularManga(page)
    }

    suspend fun searchManga(query: String? = null, genre: String? = null, page: Int = 1): List<MangaItem> {
        return anilistClient.searchManga(searchQuery = query, genre = genre, page = page)
    }

    suspend fun getMangaDetails(id: Int): MangaItem? {
        return anilistClient.getMangaDetails(id)
    }

    // Mangapill Source
    suspend fun getChapters(mangaTitle: String, totalChapters: Int?): List<ChapterItem> {
        return mangapillClient.getChaptersForManga(mangaTitle, totalChapters)
    }

    suspend fun getChapterPages(chapter: ChapterItem, mangaTitle: String): List<ChapterPage> {
        return mangapillClient.getChapterPages(chapter, mangaTitle)
    }

    // Local Database - Bookmarks
    val allBookmarks: Flow<List<BookmarkEntity>> = mangaDao.getAllBookmarks()

    fun isBookmarked(mangaId: Int): Flow<Boolean> = mangaDao.isBookmarked(mangaId)

    suspend fun toggleBookmark(manga: MangaItem, currentlyBookmarked: Boolean) {
        if (currentlyBookmarked) {
            mangaDao.deleteBookmark(manga.id)
        } else {
            mangaDao.insertBookmark(
                BookmarkEntity(
                    mangaId = manga.id,
                    title = manga.displayTitle,
                    coverImage = manga.coverImage,
                    genres = manga.genres.joinToString(", "),
                    status = manga.status,
                    score = manga.score ?: 0
                )
            )
        }
    }

    // Local Database - History
    val allHistory: Flow<List<HistoryEntity>> = mangaDao.getAllHistory()

    fun getHistoryForManga(mangaId: Int): Flow<HistoryEntity?> = mangaDao.getHistoryForManga(mangaId)

    suspend fun saveReadingProgress(
        manga: MangaItem,
        chapter: ChapterItem,
        pageNumber: Int,
        totalPages: Int
    ) {
        mangaDao.insertHistory(
            HistoryEntity(
                mangaId = manga.id,
                title = manga.displayTitle,
                coverImage = manga.coverImage,
                chapterId = chapter.id,
                chapterTitle = chapter.title,
                pageNumber = pageNumber,
                totalPages = totalPages,
                lastReadAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteHistory(mangaId: Int) {
        mangaDao.deleteHistory(mangaId)
    }

    suspend fun clearHistory() {
        mangaDao.clearAllHistory()
    }
}
