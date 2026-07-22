package com.example.data.network

import com.example.data.model.MangaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AnilistClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val graphqlUrl = "https://graphql.anilist.co"

    suspend fun getTrendingManga(page: Int = 1, perPage: Int = 20): List<MangaItem> = withContext(Dispatchers.IO) {
        val query = """
            query (${'$'}page: Int, ${'$'}perPage: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                media(type: MANGA, sort: TRENDING_DESC) {
                  id
                  title { romaji english native }
                  coverImage { extraLarge large medium }
                  bannerImage
                  description(asHtml: false)
                  status
                  averageScore
                  genres
                  chapters
                  volumes
                  format
                  startDate { year }
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("page", page)
            put("perPage", perPage)
        }

        executeGraphQL(query, variables)
    }

    suspend fun getPopularManga(page: Int = 1, perPage: Int = 20): List<MangaItem> = withContext(Dispatchers.IO) {
        val query = """
            query (${'$'}page: Int, ${'$'}perPage: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                media(type: MANGA, sort: POPULARITY_DESC) {
                  id
                  title { romaji english native }
                  coverImage { extraLarge large medium }
                  bannerImage
                  description(asHtml: false)
                  status
                  averageScore
                  genres
                  chapters
                  volumes
                  format
                  startDate { year }
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("page", page)
            put("perPage", perPage)
        }

        executeGraphQL(query, variables)
    }

    suspend fun searchManga(
        searchQuery: String? = null,
        genre: String? = null,
        page: Int = 1,
        perPage: Int = 24
    ): List<MangaItem> = withContext(Dispatchers.IO) {
        val query = """
            query (${'$'}search: String, ${'$'}genre: String, ${'$'}page: Int, ${'$'}perPage: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                media(type: MANGA, search: ${'$'}search, genre: ${'$'}genre, sort: [POPULARITY_DESC]) {
                  id
                  title { romaji english native }
                  coverImage { extraLarge large medium }
                  bannerImage
                  description(asHtml: false)
                  status
                  averageScore
                  genres
                  chapters
                  volumes
                  format
                  startDate { year }
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            if (!searchQuery.isNull_or_blank()) put("search", searchQuery)
            if (!genre.isNullOrEmpty() && genre != "All") put("genre", genre)
            put("page", page)
            put("perPage", perPage)
        }

        executeGraphQL(query, variables)
    }

    private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()

    suspend fun getMangaDetails(id: Int): MangaItem? = withContext(Dispatchers.IO) {
        val query = """
            query (${'$'}id: Int) {
              Media(id: ${'$'}id, type: MANGA) {
                id
                title { romaji english native }
                coverImage { extraLarge large medium }
                bannerImage
                description(asHtml: false)
                status
                averageScore
                genres
                chapters
                volumes
                format
                startDate { year }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("id", id)
        }

        val requestJson = JSONObject().apply {
            put("query", query)
            put("variables", variables)
        }

        val request = Request.Builder()
            .url(graphqlUrl)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                val json = JSONObject(body)
                val media = json.optJSONObject("data")?.optJSONObject("Media") ?: return@use null
                parseMediaObject(media)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun executeGraphQL(query: String, variables: JSONObject): List<MangaItem> {
        val requestJson = JSONObject().apply {
            put("query", query)
            put("variables", variables)
        }

        val request = Request.Builder()
            .url(graphqlUrl)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList()
                val body = response.body?.string() ?: return@use emptyList()
                val json = JSONObject(body)
                val mediaArray = json.optJSONObject("data")
                    ?.optJSONObject("Page")
                    ?.optJSONArray("media") ?: return@use emptyList()

                val resultList = mutableListOf<MangaItem>()
                for (i in 0 until mediaArray.length()) {
                    val mediaObj = mediaArray.optJSONObject(i) ?: continue
                    parseMediaObject(mediaObj)?.let { resultList.add(it) }
                }
                resultList
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseMediaObject(obj: JSONObject): MangaItem? {
        val id = obj.optInt("id", -1)
        if (id == -1) return null

        val titleObj = obj.optJSONObject("title")
        val romaji = titleObj?.optString("romaji") ?: "Unknown Title"
        val english = titleObj?.optString("english")?.takeIf { it != "null" && it.isNotBlank() }
        val coverObj = obj.optJSONObject("coverImage")
        val cover = coverObj?.optString("extraLarge")
            ?: coverObj?.optString("large")
            ?: coverObj?.optString("medium")
            ?: ""

        val banner = obj.optString("bannerImage").takeIf { it != "null" && it.isNotBlank() }
        var rawDesc = obj.optString("description")
        if (rawDesc == "null") rawDesc = ""
        // Clean HTML tags from description
        val cleanDesc = rawDesc.replace(Regex("<[^>]*>"), "").trim()

        val status = obj.optString("status", "RELEASING").replace("_", " ")
        val score = if (obj.has("averageScore") && !obj.isNull("averageScore")) obj.optInt("averageScore") else null

        val genresArray = obj.optJSONArray("genres")
        val genres = mutableListOf<String>()
        if (genresArray != null) {
            for (i in 0 until genresArray.length()) {
                genres.add(genresArray.optString(i))
            }
        }

        val chapters = if (obj.has("chapters") && !obj.isNull("chapters")) obj.optInt("chapters") else null
        val volumes = if (obj.has("volumes") && !obj.isNull("volumes")) obj.optInt("volumes") else null
        val format = obj.optString("format", "MANGA").replace("_", " ")
        val startDateObj = obj.optJSONObject("startDate")
        val startYear = if (startDateObj != null && startDateObj.has("year") && !startDateObj.isNull("year")) startDateObj.optInt("year") else null

        return MangaItem(
            id = id,
            titleRomaji = romaji,
            titleEnglish = english,
            coverImage = cover,
            bannerImage = banner,
            description = cleanDesc,
            status = status,
            score = score,
            genres = genres,
            chapters = chapters,
            volumes = volumes,
            format = format,
            startYear = startYear
        )
    }
}
