package fr.miage.syp.publication.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.*

data class Post(
    val id: Long,
    @JsonProperty("author_id") val authorId: UUID,
    @JsonProperty("challenge_id") val challengeId: Long,
    val content: String?,
    @JsonProperty("published_at") val publishedAt: Instant,
    @JsonProperty("image_id") val imageId: Long,
)
