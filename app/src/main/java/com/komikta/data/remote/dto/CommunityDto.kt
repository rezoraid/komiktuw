package com.komikta.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CommunityUserDto(
    val id: String = "",
    val username: String = "",
    val name: String = "",
    val picture: String = "",
    val role: String = "USER",
    val level: Int = 1,
    val email: String? = null,
    val xp: Int = 0,
    val xpCurrentLevel: Int = 0,
    val xpNextLevel: Int = 0,
    val usernameChangeableAt: Long = 0L,
    val commentCount: Int = -1,
    val joinedAt: Long = 0L,
    val publicNo: Long = 0L,
    val readMinutes: Long = -1L,
    val monthsJoined: Int = -1,
    /** Chapter yang benar-benar dibaca (dihitung server dari waktu baca aktif). -1 = belum dimuat. */
    val chaptersRead: Int = -1,
    // Hanya terisi untuk panel OWNER.
    val banned: Boolean = false,
    val hasAvatar: Boolean = false,
    val lastLoginAt: Long = 0L
)

@Serializable
data class AuthResponse(val token: String, val user: CommunityUserDto)

@Serializable
data class MeResponse(val user: CommunityUserDto, val token: String? = null)

@Serializable
data class CommentDto(
    val id: String,
    val parentId: String? = null,
    val replyTo: String? = null,
    val body: String = "",
    val deleted: Boolean = false,
    val edited: Boolean = false,
    val createdAt: Long = 0L,
    val likeCount: Int = 0,
    val replyCount: Int = 0,
    val liked: Boolean = false,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val user: CommunityUserDto = CommunityUserDto()
)

@Serializable
data class CommentPageDto(
    val items: List<CommentDto> = emptyList(),
    val hasMore: Boolean = false,
    val page: Int = 1,
    val total: Int = 0
)

@Serializable
data class CommentResultDto(
    val comment: CommentDto,
    val xpGained: Int = 0,
    val xpReason: String? = null,
    val user: CommunityUserDto? = null
)

@Serializable
data class CommentEditDto(val comment: CommentDto)

@Serializable
data class ChapterCountsDto(val counts: Map<String, Int> = emptyMap())

@Serializable
data class LikeResultDto(val liked: Boolean = false, val likeCount: Int = 0)

@Serializable
data class OkDto(val ok: Boolean = true)

@Serializable
data class ReportedCommentDto(
    val id: String = "",
    val body: String = "",
    val deleted: Boolean = false,
    val targetKey: String = "",
    val reportCount: Int = 0,
    val user: CommunityUserDto? = null
)

@Serializable
data class ReportDto(
    val id: String,
    val reason: String = "",
    val note: String = "",
    val status: String = "open",
    val createdAt: Long = 0L,
    val reporter: CommunityUserDto? = null,
    val comment: ReportedCommentDto? = null
)

@Serializable
data class ReportPageDto(
    val items: List<ReportDto> = emptyList(),
    val hasMore: Boolean = false,
    val page: Int = 1
)

@Serializable
data class ResolveDto(val ok: Boolean = true, val status: String = "")

@Serializable
data class PublicReadingHistoryDto(
    val user: CommunityUserDto = CommunityUserDto(),
    val items: List<PublicReadingProgressDto> = emptyList()
)

@Serializable
data class PublicReadingProgressDto(
    val source: String = "",
    val slug: String = "",
    val chapter: String = "",
    val comicTitle: String = "",
    val chapterTitle: String = "",
    val coverUrl: String = "",
    val updatedAt: Long = 0L
)

@Serializable
data class ProfileResponseDto(
    val user: CommunityUserDto = CommunityUserDto(),
    val self: Boolean = false
)

/** Satu baris aktivitas profil. kind: history | comment | like. */
@Serializable
data class ActivityItemDto(
    val kind: String = "",
    val source: String = "",
    val slug: String = "",
    val chapter: String? = null,
    val comicTitle: String = "",
    val chapterTitle: String = "",
    val coverUrl: String = "",
    val type: String? = null,
    val id: String? = null,
    val body: String = "",
    val isReply: Boolean = false,
    val likeCount: Int = 0,
    val replyCount: Int = 0,
    val at: Long = 0L,
    /** Jumlah chapter komik ini yang sudah dibaca pengguna (hanya untuk kind = history). */
    val chaptersRead: Int = 0
)

@Serializable
data class ActivityPageDto(
    val items: List<ActivityItemDto> = emptyList(),
    val hasMore: Boolean = false,
    val page: Int = 1
)

@Serializable
data class LikeStateDto(val liked: Boolean = false, val count: Int = 0)

@Serializable
data class AdminStatsDto(
    val users: Int = 0,
    val newUsers7d: Int = 0,
    val active24h: Int = 0,
    val banned: Int = 0,
    val owners: Int = 0,
    val moderators: Int = 0,
    val premiums: Int = 0,
    val comments: Int = 0,
    val comments24h: Int = 0,
    val openReports: Int = 0,
    val chaptersRead: Long = 0L
)

@Serializable
data class AdminUserPageDto(val items: List<CommunityUserDto> = emptyList(), val hasMore: Boolean = false, val page: Int = 1, val total: Int = 0)

@Serializable
data class AdminCommentDto(
    val id: String = "",
    val body: String = "",
    val deleted: Boolean = false,
    val targetKey: String = "",
    val createdAt: Long = 0L,
    val likeCount: Int = 0,
    val replyCount: Int = 0,
    val reportCount: Int = 0,
    val user: CommunityUserDto? = null
)

@Serializable
data class AdminCommentPageDto(val items: List<AdminCommentDto> = emptyList(), val hasMore: Boolean = false, val page: Int = 1, val total: Int = 0)
