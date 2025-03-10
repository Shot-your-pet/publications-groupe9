package fr.miage.syp.publication.service

import com.fasterxml.jackson.annotation.JsonProperty
import fr.miage.syp.publication.model.Post
import org.springframework.amqp.AmqpException
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class MessagingService(
    private val rabbitTemplate: RabbitTemplate,
    @Value("\${publish.broadcastExchangeName}") private val broadcastExchangeName: String,
) {
    data class PostMessage(
        val id: Long,
        @JsonProperty("author_id") val authorId: UUID,
        @JsonProperty("challenge_id") val challengeId: Long,
        val content: String?,
        @JsonProperty("published_at") val publishedAt: Instant,
        @JsonProperty("image_id") val imageId: Long,
    ) {
        constructor(publishedPost: Post.PublishedPost) : this(
            publishedPost.id,
            publishedPost.authorId,
            publishedPost.challengeId,
            publishedPost.content,
            publishedPost.publishedAt,
            publishedPost.imageId
        )
    }

    @Throws(AmqpException::class)
    fun sendPostToBus(post: Post.PublishedPost) {
        rabbitTemplate.convertAndSend(broadcastExchangeName, PUBLISH_ROUTING_KEY, PostMessage(post))
    }

    companion object {
        const val PUBLISH_ROUTING_KEY = "publish"
    }
}