package fr.miage.syp.publication.service

import fr.miage.syp.publication.data.exception.ChallengeAlreadyCompletedException
import fr.miage.syp.publication.data.exception.PostAlreadyPublishedException
import fr.miage.syp.publication.data.exception.PostNotFoundException
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
import java.security.SecureRandom
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import fr.miage.syp.publication.model.Post as ModelPost


@SpringBootTest
class PublishedPostServiceTest {
    @MockitoBean
    internal lateinit var postRepository: PostRepository

    @MockitoBean
    internal lateinit var snowflakeIdGenerator: SnowflakeIdGenerator

    @Autowired
    lateinit var postService: PostService

    private val random = SecureRandom()

    @Test
    fun `getPost should return PublishedPost when imageId is not null`() {
        val postId = random.nextLong()
        val post = Post(
            id = postId,
            authorId = UUID.randomUUID(),
            challengeId = random.nextLong(),
            content = "Content",
            publishedAt = Instant.now(),
            imageId = random.nextLong(),
            likedBy = emptyList()
        )
        doReturn(Optional.of(post)).`when`(postRepository).findById(postId)
        val result = postService.getPost(postId)
        assertEquals(
            ModelPost.PublishedPost(
                postId, post.authorId, post.challengeId, post.content, post.publishedAt, post.imageId!!
            ),
            result,
        )
    }

    @Test
    fun `getPost should return DraftedPost when imageId is null`() {
        val postId = random.nextLong()
        val post = Post(
            id = postId,
            authorId = UUID.randomUUID(),
            challengeId = random.nextLong(),
            content = "Content",
            publishedAt = Instant.now(),
            imageId = null,
            likedBy = emptyList()
        )
        doReturn(Optional.of(post)).`when`(postRepository).findById(postId)
        val result = postService.getPost(postId)
        assertEquals(
            ModelPost.DraftedPost(postId, post.authorId, post.challengeId, post.content, post.publishedAt), result
        )
    }

    @Test
    fun `getPost should return null when post is not found`() {
        val postId = random.nextLong()
        doReturn(Optional.empty<Post>()).`when`(postRepository).findById(postId)
        val result = postService.getPost(postId)
        assertNull(result)
    }


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
            challengeId = Random().nextLong(),
            content = "content",
            publishedAt = Instant.now(),
            imageId = 50L,
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
        assertEquals(postEntity.challengeId, result[0].challengeId)
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
        val postEntity = Post(
            id = 1,
            authorId = authorId,
            challengeId = 0L,
            content = "content",
            publishedAt = Instant.now(),
            imageId = null,
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
                challengeId = it.toLong() + 150L,
                content = "content$it",
                publishedAt = Instant.now(),
                imageId = it.toLong(),
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
            val challengeId = random.nextLong()
            val imageId = random.nextLong()
            val createdPost = Post(
                newId, uuid, challengeId, null, now, imageId, emptyList()
            )

            doReturn(newId).`when`(snowflakeIdGenerator).nextId(anyLong())
            doReturn(createdPost).`when`(postRepository).save(createdPost)

            val createdId = postService.createPostForUser(uuid, challengeId, null, imageId)

            Assertions.assertEquals(newId, createdId.getOrThrow().id)
            verify(postRepository, times(1)).save(createdPost)
        }
    }

    @Test
    fun `create post with content should create post`() {
        withMockedInstant { now ->
            val newId = 125L
            val uuid = UUID.randomUUID()
            val challengeId = Random().nextLong()
            val imageId = random.nextLong()
            val createdPost = Post(
                newId, uuid, challengeId, "foo", now, imageId, emptyList()
            )

            doReturn(newId).`when`(snowflakeIdGenerator).nextId(anyLong())
            doReturn(createdPost).`when`(postRepository).save(createdPost)

            val createdId = postService.createPostForUser(uuid, challengeId, "foo", imageId)

            Assertions.assertEquals(newId, createdId.getOrThrow().id)
            verify(postRepository, times(1)).save(createdPost)
        }
    }

    @Test
    fun `create post with same challenge should fail`() {
        withMockedInstant { now ->
            val newId = 125L
            val uuid = UUID.randomUUID()
            val challengeId = Random().nextLong()
            val imageId = random.nextLong()
            val createdPost = Post(
                newId, uuid, challengeId, "foo", now, null, emptyList()
            )

            doReturn(true).`when`(postRepository).existsPostByAuthorIdAndChallengeId(uuid, challengeId)
            val createdId = postService.createPostForUser(uuid, challengeId, "foo", imageId)

            Assertions.assertTrue(createdId.exceptionOrNull() is ChallengeAlreadyCompletedException)
            verify(postRepository, times(0)).save(createdPost)
        }
    }

    @Test
    fun `remove post call repository with good id`() {
        val id = random.nextLong()
        doNothing().`when`(postRepository).deleteById(id)
        postService.removePost(id)
        verify(postRepository, times(1)).deleteById(id)
    }

    @Test
    fun `setImageIdForPost should return failure when post is not found`() {
        val postId = random.nextLong()
        val imageId = random.nextLong()
        doReturn(Optional.empty<Post>()).`when`(postRepository).findById(postId)
        val result = postService.setImageIdForPost(postId, imageId)
        assertTrue(result.isFailure)
        assertEquals(PostNotFoundException::class, result.exceptionOrNull()!!::class)
    }

    @Test
    fun `setImageIdForPost should return failure when post is already published`() {
        val postId = random.nextLong()
        val imageId = random.nextLong()
        val authorId = UUID.randomUUID()
        val post = Post(
            id = postId,
            authorId = authorId,
            challengeId = random.nextLong(),
            content = "Content",
            publishedAt = Instant.now(),
            imageId = imageId,
            likedBy = emptyList()
        )
        doReturn(Optional.of(post)).`when`(postRepository).findById(postId)
        val result = postService.setImageIdForPost(postId, imageId)
        assertTrue(result.isFailure)
        assertEquals(PostAlreadyPublishedException::class, result.exceptionOrNull()!!::class)
    }

    @Test
    fun `setImageIdForPost should return success when imageId is set successfully`() {
        val postId = random.nextLong()
        val imageId = random.nextLong()
        val authorId = UUID.randomUUID()
        val post = Post(
            id = postId,
            authorId = authorId,
            challengeId = random.nextLong(),
            content = "Content",
            publishedAt = Instant.now(),
            imageId = null,
            likedBy = emptyList()
        )
        doReturn(Optional.of(post)).`when`(postRepository).findById(postId)
        val newPost = post.copy(imageId = imageId)
        doReturn(newPost).`when`(postRepository).save(newPost)

        val result = postService.setImageIdForPost(postId, imageId)
        assertTrue(result.isSuccess)
        assertEquals(
            ModelPost.PublishedPost(
                newPost.id,
                newPost.authorId,
                newPost.challengeId,
                newPost.content,
                newPost.publishedAt,
                newPost.imageId!!
            ), result.getOrNull()
        )
        verify(postRepository).save(post.copy(imageId = imageId))
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