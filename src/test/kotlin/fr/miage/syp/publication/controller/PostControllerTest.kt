package fr.miage.syp.publication.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import fr.miage.syp.publication.data.exception.ChallengeAlreadyCompletedException
import fr.miage.syp.publication.model.NewPost
import fr.miage.syp.publication.model.Post
import fr.miage.syp.publication.service.MessagingService
import fr.miage.syp.publication.service.PostService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.amqp.AmqpException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.util.*


@SpringBootTest
@AutoConfigureMockMvc
class PostControllerTest {
    companion object {
        private const val USER_UUID = "cd4b40e4-f532-4acc-812e-9d5ed8ac1267"
    }

    @Autowired
    lateinit var mvc: MockMvc

    private val mapper = ObjectMapper().registerKotlinModule()

    @MockitoBean
    lateinit var messagingService: MessagingService

    @MockitoBean
    lateinit var postService: PostService

    private val random = Random()

    @Test
    fun `getPost without authentication is unauthorized`() {
        mvc.perform(MockMvcRequestBuilders.post("/posts/${random.nextLong()}")).andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = USER_UUID)
    fun `getPost should return 200 OK when published post is found`() {
        val postId = random.nextLong()
        val authorId = UUID.randomUUID()
        val challengeId = UUID.randomUUID()
        val imageId = random.nextLong()
        val post = Post(postId, authorId, challengeId, "Content", Instant.now(), imageId)
        doReturn(post).`when`(postService).getPost(postId)
        mvc.perform(
            get("/posts/$postId").contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk).andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.contenu.id").value(postId)).andExpect(jsonPath("$.contenu.content").value("Content"))
    }

    @Test
    @WithMockUser(username = USER_UUID)
    fun `getPost should return 404 Not Found when post is not found`() {
        val postId = random.nextLong()
        doReturn(null).`when`(postService).getPost(postId)
        mvc.perform(
            get("/posts/$postId").contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `test create post without authentication is unauthorized`() {
        mvc.perform(MockMvcRequestBuilders.post("/posts/")).andExpect(status().isUnauthorized)
        verify(messagingService, times(0)).sendPostToBus(any())
    }

    @Test
    @WithMockUser(username = USER_UUID)
    fun `test create post with authentication no content create object`() {

        val newId = random.nextLong()
        val authorId = UUID.fromString(USER_UUID)
        val challengeId = UUID.randomUUID()
        val imageId = random.nextLong()
        val post = Post(
            newId, authorId, challengeId, null, Instant.now(), imageId
        )
        doNothing().`when`(messagingService).sendPostToBus(post)
        wheneverBlocking { postService.createPostForUser(authorId, null, imageId) }.doReturn(
            Result.success(
                post
            )
        )
        val content = mapper.writeValueAsString(NewPost(null, imageId))
        val result = mvc.perform(
            MockMvcRequestBuilders.post("/posts").contentType(MediaType.APPLICATION_JSON).content(content)
        ).andReturn()
        mvc.perform(
            asyncDispatch(result)
        ).andExpect(status().isCreated).andExpect(jsonPath("$.contenu.id").value(newId))
        verify(messagingService, times(1)).sendPostToBus(post)
    }

    @Test
    @WithMockUser(username = USER_UUID)
    fun `test create post with authentication with content create object`() {
        val newId = random.nextLong()
        val createPostContent = "bar"
        val challengeId = UUID.randomUUID()
        val authorId = UUID.fromString(USER_UUID)
        val imageId = random.nextLong()
        val post = Post(
            newId, authorId, challengeId, createPostContent, Instant.now(), imageId
        )
        wheneverBlocking { postService.createPostForUser(authorId, createPostContent, imageId) }.doReturn(Result.success(post))
        doNothing().`when`(messagingService).sendPostToBus(post)
        val content = mapper.writeValueAsString(NewPost(createPostContent, imageId))
        val results = mvc.perform(
            MockMvcRequestBuilders.post("/posts").contentType(MediaType.APPLICATION_JSON).content(content)
        ).andReturn()
        mvc.perform(
            asyncDispatch(results)
        ).andExpect(status().isCreated).andExpect(jsonPath("$.contenu.id").value(newId))
        verify(messagingService, times(1)).sendPostToBus(post)
    }

    @Test
    @WithMockUser(username = USER_UUID)
    fun `test create post with authentication with already taken challenge should return conflict`() {
        val createPostContent = "bar"
        val imageId = random.nextLong()
        wheneverBlocking {
            postService.createPostForUser(
                UUID.fromString(USER_UUID), createPostContent, imageId
            )
        }.thenReturn(
            Result.failure(ChallengeAlreadyCompletedException())
        )

        val content = mapper.writeValueAsString(NewPost(createPostContent, imageId))
        val result = mvc.perform(
            MockMvcRequestBuilders.post("/posts").contentType(MediaType.APPLICATION_JSON).content(content)
        ).andReturn()
        mvc.perform(asyncDispatch(result)).andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value(409))

        verify(messagingService, times(0)).sendPostToBus(any<Post>())
    }

    @Test
    @WithMockUser(username = USER_UUID)
    fun `test create post with authentication when rabbit error should service unavailable`() {
        val newId = random.nextLong()
        val createPostContent = "bar"
        val challengeId = UUID.randomUUID()
        val authorId = UUID.fromString(USER_UUID)
        val imageId = random.nextLong()
        val post = Post(
            newId, authorId, challengeId, null, Instant.now(), imageId
        )
        wheneverBlocking {
            postService.createPostForUser(authorId, createPostContent, imageId)
        }.doReturn(Result.success(post))
        doThrow(AmqpException("foo")).`when`(messagingService).sendPostToBus(post)
        doNothing().`when`(postService).removePost(post.id)
        val content = mapper.writeValueAsString(NewPost(createPostContent, imageId))
        val result = mvc.perform(
            MockMvcRequestBuilders.post("/posts").contentType(MediaType.APPLICATION_JSON).content(content)
        ).andReturn()
        mvc.perform(asyncDispatch(result)).andExpect(status().isServiceUnavailable)
        verify(postService, times(1)).removePost(post.id)
    }
}