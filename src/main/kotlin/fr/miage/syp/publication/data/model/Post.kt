package fr.miage.syp.publication.data.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.Instant
import java.util.*

@Document(collection = "posts")
data class Post(
    @Id val id: Long,
    @Field(name = "author_id") val authorId: UUID,
    @Field(name = "challenge_id") val challengeId: UUID,
    @Field(name = "content") val content: String?,
    @Field(name = "published_at") val publishedAt: Instant,
    @Field(name = "image_id") val imageId: Long,
    @Field(name = "liked_by") val likedBy: List<UUID>,
)
