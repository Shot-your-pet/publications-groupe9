package fr.miage.syp.publication.service

import com.fasterxml.jackson.databind.ObjectMapper
import fr.miage.syp.publication.model.Post
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.amqp.AmqpException
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
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

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `sendImageToBus call rabbit template with good parameters`() {
        val postId = random.nextLong()
        val imageId = random.nextLong()
        val post = Post(postId, UUID.randomUUID(), UUID.randomUUID(), "foo", Instant.now(), imageId)
        val messagePost = MessagingService.PostMessage(post)
        doNothing().`when`(rabbitTemplate).convertAndSend(any(), any(), eq(messagePost))
        messageService.sendPostToBus(post)
        verify(rabbitTemplate, times(1)).convertAndSend(any(), any(), eq(MessagingService.PublicationMessage(messagePost)))
    }

    @Test
    fun `sendImageToBus on exception propagate said exception`() {
        val postId = random.nextLong()
        val imageId = random.nextLong()
        val post = Post(postId, UUID.randomUUID(), UUID.randomUUID(), "foo", Instant.now(), imageId)
        doThrow(AmqpException("foo")).`when`(rabbitTemplate)
            .convertAndSend(any(), any(), any<MessagingService.PublicationMessage>())
        val ex = assertThrows<AmqpException> {
            messageService.sendPostToBus(post)
        }
        assertEquals("foo", ex.message)
    }

    @Test
    fun `PostMessage event serialization match schema`() {
        val res = """
        {
            "content": {
                "id": 1,
                "author_id": "6eb6c444-fdf8-415d-b815-fb89469ad214",
                "challenge_id": "42b6c444-fdf8-415d-b815-fb89469ad214",
                "date": "2023-10-01T12:00:00Z",
                "content": "A new publication",
                "image_id": 123
            },
            "type": "new_publication"
        }"""

        val event = MessagingService.PublicationMessage(
            MessagingService.PostMessage(
                1L,
                UUID.fromString("6eb6c444-fdf8-415d-b815-fb89469ad214"),
                UUID.fromString("42b6c444-fdf8-415d-b815-fb89469ad214"),
                "A new publication",
                Instant.parse("2023-10-01T12:00:00Z"),
                123,
            )
        )

        val initTree = objectMapper.readTree(res)
        val resJson = objectMapper.readTree(objectMapper.writeValueAsString(event))

        assertEquals(initTree, resJson)
    }

    @Test
    fun `LikeMessage event serialization match schema`() {
        val res = """
        {
            "type": "like",
            "content": {
                "author_id": "6eb6c444-fdf8-415d-b815-fb89469ad214",
                "post_id": 1
            }
        }"""

        val event = MessagingService.PublicationMessage(
            MessagingService.LikeMessage(
                UUID.fromString("6eb6c444-fdf8-415d-b815-fb89469ad214"),
                1,
            )
        )

        val initTree = objectMapper.readTree(res)
        val resJson = objectMapper.readTree(objectMapper.writeValueAsString(event))

        assertEquals(initTree, resJson)
    }
}



