package fr.miage.syp.publication.service

import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
class MessagingService(
    private val postService: PostService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    data class PublishedImageMessage(
        @JsonProperty("post_id") val postId: Long,
        @JsonProperty("image_id") val imageId: Long,
    )

    @RabbitListener(queues = ["\${publish.imagePublishedQueueName}"])
    fun onImageUploads(message: PublishedImageMessage) {
        postService.setImageIdForPost(message.postId, message.imageId).onSuccess { published ->
            // TODO: send published to the bus
        }.onFailure {
            logger.error("failed to save image for post ${message.postId}", it)
        }
    }
}