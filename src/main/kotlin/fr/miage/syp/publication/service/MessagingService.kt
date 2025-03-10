package fr.miage.syp.publication.service

import com.fasterxml.jackson.annotation.JsonProperty
import fr.miage.syp.publication.model.Post
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class MessagingService(
    private val postService: PostService,
    private val rabbitTemplate: RabbitTemplate,
    @Value("\${publish.broadcastExchangeName}") private val broadcastExchangeName: String,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    data class PublishedImageMessage(
        @JsonProperty("post_id") val postId: Long,
        @JsonProperty("image_id") val imageId: Long,
    )

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

    @RabbitListener(queues = ["\${publish.imagePublishedQueueName}"])
    fun onImageUploads(message: PublishedImageMessage) {
        postService.setImageIdForPost(message.postId, message.imageId).mapCatching { published ->
            rabbitTemplate.convertAndSend(broadcastExchangeName, PUBLISH_ROUTING_KEY, PostMessage(published))
        }.onFailure {
            logger.error("failed to save image for post ${message.postId}", it)
        }
    }

    companion object {
        const val PUBLISH_ROUTING_KEY = "publish"
    }
}