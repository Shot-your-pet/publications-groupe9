package fr.miage.syp.publication.service

import fr.miage.syp.publication.data.exception.PostAlreadyPublishedException
import fr.miage.syp.publication.data.exception.PostNotFoundException
import fr.miage.syp.publication.model.Post
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.security.SecureRandom
import java.time.Instant
import java.util.*


@SpringBootTest
class MessagingServiceTest {

    @MockitoBean
    lateinit var postService: PostService

    private val random = SecureRandom()

    @Autowired
    lateinit var messageService: MessagingService


    @Test
    fun `onImageUploads for drafted post is successful`() {
        val postId = random.nextLong()
        val imageId = random.nextLong()
        val post = Post.PublishedPost(postId, UUID.randomUUID(), random.nextLong(), "foo", Instant.now(), imageId)
        doReturn(Result.success(post)).`when`(postService).setImageIdForPost(postId, imageId)
        messageService.onImageUploads(MessagingService.PublishedImageMessage(postId, imageId))
        verify(postService, times(1)).setImageIdForPost(postId, imageId)
    }

    @Test
    fun `onImageUploads for published post will fail`() {
        val postId = random.nextLong()
        val imageId = random.nextLong()
        doReturn(Result.failure<Post.PublishedPost>(PostAlreadyPublishedException())).`when`(postService)
            .setImageIdForPost(postId, imageId)
        messageService.onImageUploads(MessagingService.PublishedImageMessage(postId, imageId))
        verify(postService, times(1)).setImageIdForPost(postId, imageId)
    }

    @Test
    fun `onImageUploads for not found post will fail`() {
        val postId = random.nextLong()
        val imageId = random.nextLong()
        doReturn(Result.failure<Post.PublishedPost>(PostNotFoundException())).`when`(postService)
            .setImageIdForPost(postId, imageId)
        messageService.onImageUploads(MessagingService.PublishedImageMessage(postId, imageId))
        verify(postService, times(1)).setImageIdForPost(postId, imageId)
    }
}



