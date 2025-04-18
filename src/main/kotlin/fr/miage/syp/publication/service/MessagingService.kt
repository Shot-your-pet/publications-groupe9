package fr.miage.syp.publication.service

import com.fasterxml.jackson.annotation.*
import fr.miage.syp.publication.model.Post
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.amqp.AmqpException
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class MessagingService(
    private val rabbitTemplate: RabbitTemplate,
    @Value("\${publish.timelineExchangeName}") private val timelineExchangeName: String,
    @Value("\${publish.timelineRoutingKey}") private val timelineRoutingKey: String,
    @Value("\${publish.getChallengeQueue}") private val getChallengeQueue: String,
) {
    data class PublicationMessage(
        @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type", visible = true
        ) val content: EventContent
    )

    @JsonSubTypes(
        JsonSubTypes.Type(value = PostMessage::class, name = "new_publication"),
        JsonSubTypes.Type(value = LikeMessage::class, name = "like")
    )
    sealed interface EventContent

    @JsonRootName(value = "content")
    @JsonTypeName("new_publication")
    data class PostMessage(
        val id: Long,
        @JsonProperty("author_id") val authorId: UUID,
        @JsonProperty("challenge_id") val challengeId: UUID,
        val content: String?,
        @JsonProperty("date") val publishedAt: Instant,
        @JsonProperty("image_id") val imageId: Long,
    ) : EventContent {
        constructor(publishedPost: Post) : this(
            publishedPost.id,
            publishedPost.authorId,
            publishedPost.challengeId,
            publishedPost.content,
            publishedPost.publishedAt,
            publishedPost.imageId
        )
    }

    @JsonRootName(value = "content")
    @JsonTypeName("like")
    data class LikeMessage(@JsonProperty("author_id") val authorId: UUID, @JsonProperty("post_id") val postId: Long) :
        EventContent

    @Throws(AmqpException::class)
    fun sendPostToBus(post: Post) {
        rabbitTemplate.convertAndSend(timelineExchangeName, timelineRoutingKey, PublicationMessage(PostMessage(post)))
    }

    data class DailyChallenge(
        @JsonProperty("id") val id: UUID,
        @JsonProperty("dateDebut") val startDate: Instant,
        @JsonProperty("dateFin") val endDate: Instant,
        @JsonProperty("challenge") val challenge: Challenge,
    )

    data class Challenge(
        @JsonProperty("titre") val title: String,
        @JsonProperty("description") val description: String,
    )

    private val challengeMutex = Mutex()
    private var currentChallenge: DailyChallenge? = null

    suspend fun getCurrentChallenge(): DailyChallenge? {
        return challengeMutex.withLock {
            val now = Instant.now()
            val thisChallenge = currentChallenge
            if (thisChallenge == null || thisChallenge.endDate < now) {
                currentChallenge = getNextChallenge()
            }
            currentChallenge
        }
    }

    private fun getNextChallenge(): DailyChallenge? {
        val resp = rabbitTemplate.convertSendAndReceiveAsType(
            getChallengeQueue,
            Instant.now(),
            ParameterizedTypeReference.forType<DailyChallenge>(DailyChallenge::class.java)
        )
        return resp
    }
}