package fr.miage.syp.publication.service

import fr.miage.syp.publication.data.model.Post
import fr.miage.syp.publication.data.repository.PostRepository
import fr.miage.syp.publication.services.SnowflakeIdGenerator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Instant
import java.util.*


@SpringBootTest
class PostServiceTest {
    @MockitoBean
    internal lateinit var postRepository: PostRepository

    @MockitoBean
    internal lateinit var snowflakeIdGenerator: SnowflakeIdGenerator

    @Autowired
    lateinit var postService: PostService

    @Test
    fun `create post without content should create post`() {
        withMockedInstant { now ->
            val newId = 125L
            val uuid = UUID.randomUUID()
            val createdPost = Post(
                newId, uuid, null, now, null, true, emptyList()
            )

            doReturn(newId).`when`(snowflakeIdGenerator).nextId(anyLong(), anyLong())
            doReturn(createdPost).`when`(postRepository).save(createdPost)

            val createdId = postService.createDraftedPostForUser(uuid, null)

            Assertions.assertEquals(newId, createdId)
            verify(postRepository, times(1)).save(createdPost)
        }
    }

    @Test
    fun `create post with content should create post`() {
        withMockedInstant { now ->
            val newId = 125L
            val uuid = UUID.randomUUID()
            val createdPost = Post(
                newId, uuid, "foo", now, null, true, emptyList()
            )

            doReturn(newId).`when`(snowflakeIdGenerator).nextId(anyLong(), anyLong())
            doReturn(createdPost).`when`(postRepository).save(createdPost)

            val createdId = postService.createDraftedPostForUser(uuid, "foo")

            Assertions.assertEquals(newId, createdId)
            verify(postRepository, times(1)).save(createdPost)
        }
    }

    fun withMockedInstant(block: (now: Instant) -> Unit) {
        val now = Instant.now()
        mockStatic(
            Instant::class.java, withSettings().defaultAnswer { invocation -> invocation.callRealMethod() }).use {
            it.`when`<Instant> {
                Instant.now()
            }.thenReturn(now)

            block(now)
        }
    }
}