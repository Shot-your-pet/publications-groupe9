package fr.miage.syp.publication.service

import fr.miage.syp.publication.data.model.Post
import fr.miage.syp.publication.data.repository.PostRepository
import fr.miage.syp.publication.services.SnowflakeIdGenerator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue


@SpringBootTest
class PostServiceTest {
    @MockitoBean
    internal lateinit var postRepository: PostRepository

    @MockitoBean
    internal lateinit var snowflakeIdGenerator: SnowflakeIdGenerator

    @Autowired
    lateinit var postService: PostService

    @Test
    fun `should return list of ModelPost when posts are found`() {
        // Arrange
        val page = 0
        val maxSize = 10
        val authorId = UUID.randomUUID()
        val imageId = 50L
        val postEntity = Post(
            id = 1,
            authorId = authorId,
            content = "content",
            publishedAt = Instant.now(),
            imageId = 50L,
            isDraft = false,
            likedBy = emptyList()
        )
        val postEntityList = listOf(postEntity)

        `when`(
            postRepository.findPostByImageIdNotNull(
                PageRequest.of(
                    page, maxSize, Sort.by(Sort.Direction.DESC, "id")
                )
            )
        ).thenReturn(postEntityList)

        // Act
        val result = postService.getPosts(page, maxSize)

        // Assert
        assertEquals(1, result.size)
        assertEquals(authorId, result[0].authorId)
        assertEquals("content", result[0].content)
        assertEquals(postEntity.publishedAt, result[0].publishedAt)
        assertEquals(imageId, result[0].imageId)
    }

    @Test
    fun `should throw exception when imageId is null`() {
        // Arrange
        val page = 0
        val maxSize = 10
        val authorId = UUID.randomUUID()
        val imageId = 50L
        val postEntity = Post(
            id = 1,
            authorId = authorId,
            content = "content",
            publishedAt = Instant.now(),
            imageId = null,
            isDraft = true,
            likedBy = emptyList()
        )
        val postEntityList = listOf(postEntity)

        `when`(
            postRepository.findPostByImageIdNotNull(
                PageRequest.of(
                    page, maxSize, Sort.by(Sort.Direction.DESC, "id")
                )
            )
        ).thenReturn(postEntityList)

        // Act & Assert
        val exception = assertThrows<IllegalArgumentException> {
            postService.getPosts(page, maxSize)
        }
        assertEquals("imageId cannot be null", exception.message)
    }

    @Test
    fun `should return empty list when no posts are found`() {
        // Arrange
        val page = 0
        val maxSize = 10
        val pageImpl = emptyList<Post>()

        `when`(
            postRepository.findPostByImageIdNotNull(
                PageRequest.of(
                    page, maxSize, Sort.by(Sort.Direction.DESC, "id")
                )
            )
        ).thenReturn(pageImpl)

        // Act
        val result = postService.getPosts(page, maxSize)

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should handle pagination correctly`() {
        // Arrange
        val page = 1
        val maxSize = 5
        val postEntityList = (1..10).map {
            Post(
                id = it.toLong(),
                authorId = UUID.randomUUID(),
                content = "content$it",
                publishedAt = Instant.now(),
                imageId = it.toLong(),
                isDraft = false,
                likedBy = emptyList()
            )
        }

        `when`(
            postRepository.findPostByImageIdNotNull(
                PageRequest.of(
                    page, maxSize, Sort.by(Sort.Direction.DESC, "id")
                )
            )
        ).thenReturn(postEntityList.subList(5, 10))

        // Act
        val result = postService.getPosts(page, maxSize)

        // Assert
        assertEquals(5, result.size) // Assuming there are 10 posts, and we are requesting page 1 with size 5
        result.forEachIndexed { index, post ->
            assertEquals(postEntityList[5 + index].id, post.id)
        }
    }

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