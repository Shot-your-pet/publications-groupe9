package fr.miage.syp.publication.service

import fr.miage.syp.publication.data.exception.PostAlreadyPublishedException
import fr.miage.syp.publication.data.exception.PostNotFoundException
import fr.miage.syp.publication.model.Post
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.amqp.AmqpException
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.security.SecureRandom
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals


@SpringBootTest
class MessagingServiceTest {
    @MockitoBean
    lateinit var postService: PostService

    private val random = SecureRandom()

    @Autowired
    lateinit var messageService: MessagingService

    @MockitoBean
    lateinit var rabbitTemplate: RabbitTemplate

    @Value("\${publish.broadcastExchangeName}")
    lateinit var broadcastExchangeName: String

    @Test
    fun `onImageUploads for drafted post is successful`() {
        val postId = random.nextLong()
        val imageId = random.nextLong()
        val post = Post.PublishedPost(postId, UUID.randomUUID(), random.nextLong(), "foo", Instant.now(), imageId)
        val messagePost = MessagingService.PostMessage(post)
        doReturn(Result.success(post)).`when`(postService).setImageIdForPost(postId, imageId)
        doNothing().`when`(rabbitTemplate).convertAndSend(
            broadcastExchangeName, MessagingService.PUBLISH_ROUTING_KEY, messagePost
        )
        messageService.onImageUploads(MessagingService.PublishedImageMessage(postId, imageId))
        verify(postService, times(1)).setImageIdForPost(postId, imageId)
        verify(rabbitTemplate, times(1)).convertAndSend(
            broadcastExchangeName, MessagingService.PUBLISH_ROUTING_KEY, messagePost
        )
    }

    @Test
    fun `onImageUploads for published post will fail`() {
        val postId = random.nextLong()
        val imageId = random.nextLong()
        doReturn(Result.failure<Post.PublishedPost>(PostAlreadyPublishedException())).`when`(postService)
            .setImageIdForPost(postId, imageId)
        messageService.onImageUploads(MessagingService.PublishedImageMessage(postId, imageId))
        verify(postService, times(1)).setImageIdForPost(postId, imageId)
        verify(rabbitTemplate, times(0)).convertAndSend(
            any(), any(), any<Any>()
        )
    }

    @Test
    fun `onImageUploads for not found post will fail`() {
        val postId = random.nextLong()
        val imageId = random.nextLong()
        doReturn(Result.failure<Post.PublishedPost>(PostNotFoundException())).`when`(postService)
            .setImageIdForPost(postId, imageId)
        messageService.onImageUploads(MessagingService.PublishedImageMessage(postId, imageId))
        verify(postService, times(1)).setImageIdForPost(postId, imageId)
        verify(rabbitTemplate, times(0)).convertAndSend(
            any(), any(), any<Any>()
        )
    }

    @Test
    fun `sendImageToBus call rabbit template with good parameters`() {
        val postId = random.nextLong()
        val imageId = random.nextLong()
        val post = Post.PublishedPost(postId, UUID.randomUUID(), random.nextLong(), "foo", Instant.now(), imageId)
        val messagePost = MessagingService.PostMessage(post)
        doNothing().`when`(rabbitTemplate).convertAndSend(any(), any(), eq(messagePost))
        messageService.sendPostToBus(post)
        verify(rabbitTemplate, times(1)).convertAndSend(any(), any(), eq(messagePost))
    }

    @Test
    fun `sendImageToBus on exception propagate said exception`() {
        val postId = random.nextLong()
        val imageId = random.nextLong()
        val post = Post.PublishedPost(postId, UUID.randomUUID(), random.nextLong(), "foo", Instant.now(), imageId)
        doThrow(AmqpException("foo")).`when`(rabbitTemplate)
            .convertAndSend(any(), any(), any<MessagingService.PostMessage>())
        val ex = assertThrows<AmqpException> {
            messageService.sendPostToBus(post)
        }
        assertEquals("foo", ex.message)
    }
}



