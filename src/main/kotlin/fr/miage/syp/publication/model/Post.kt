package fr.miage.syp.publication.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.Instant
import java.util.*

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT, property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(Post.PublishedPost::class, name = "published")
)
sealed interface Post {
    val id: Long
    val authorId: UUID
    val challengeId: Long
    val content: String?
    val publishedAt: Instant?

    @JsonTypeName("published")
    data class PublishedPost(
        override val id: Long,
        @JsonProperty("author_id") override val authorId: UUID,
        @JsonProperty("challenge_id") override val challengeId: Long,
        override val content: String?,
        @JsonProperty("published_at") override val publishedAt: Instant,
        @JsonProperty("image_id") val imageId: Long,
    ) : Post
}