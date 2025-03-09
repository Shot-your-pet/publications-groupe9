package fr.miage.syp.publication.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import fr.miage.syp.publication.data.exception.ChallengeAlreadyCompletedException
import fr.miage.syp.publication.model.DraftedPost
import fr.miage.syp.publication.model.NewPost
import fr.miage.syp.publication.model.Post
import fr.miage.syp.publication.service.PostService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import java.time.Instant
import java.util.*


@SpringBootTest
@AutoConfigureMockMvc
class PublishedPostControllerTest {
    companion object {
        private const val USER_UUID = "cd4b40e4-f532-4acc-812e-9d5ed8ac1267"
    }

    @Autowired
    lateinit var mvc: MockMvc

    private val mapper = ObjectMapper().registerKotlinModule()

    @MockitoBean
    lateinit var postService: PostService

    private val random = Random()

    @Test
    fun `test create post without authentication is unauthorized`() {
        mvc.perform(MockMvcRequestBuilders.post("/posts/")).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = USER_UUID)
    fun `test create post with authentication no content create object`() {
        val newId = random.nextLong()
        val authorId = UUID.fromString(USER_UUID)
        val challengeId = random.nextLong()
        doReturn(
            Result.success(
                Post.DraftedPost(
                    newId, authorId, challengeId, null, Instant.now()
                )
            )
        ).`when`(postService).createDraftedPostForUser(authorId, challengeId, null)
        val content = mapper.writeValueAsString(NewPost(null, challengeId))
        mvc.perform(
            MockMvcRequestBuilders.post("/posts/").contentType(MediaType.APPLICATION_JSON).content(content)
        ).andExpect(MockMvcResultMatchers.status().isCreated).andExpect(jsonPath("$.drafted.id").value(newId))
    }

    @Test
    @WithMockUser(username = USER_UUID)
    fun `test create post with authentication with content create object`() {
        val newId = random.nextLong()
        val createPostContent = "bar"
        val challengeId = random.nextLong()
        val authorId = UUID.fromString(USER_UUID)
        doReturn(
            Result.success(
                Post.DraftedPost(
                    newId, authorId, challengeId, null, Instant.now()
                )
            )
        ).`when`(postService).createDraftedPostForUser(authorId, challengeId, createPostContent)
        val content = mapper.writeValueAsString(NewPost(createPostContent, challengeId))
        mvc.perform(
            MockMvcRequestBuilders.post("/posts/").contentType(MediaType.APPLICATION_JSON).content(content)
        ).andExpect(MockMvcResultMatchers.status().isCreated).andExpect(jsonPath("$.drafted.id").value(newId))
    }

    @Test
    @WithMockUser(username = USER_UUID)
    fun `test create post with authentication with already taken challenge should return conflict`() {
        val createPostContent = "bar"
        val challengeId = random.nextLong()
        doReturn(Result.failure<DraftedPost>(ChallengeAlreadyCompletedException())).`when`(postService)
            .createDraftedPostForUser(UUID.fromString(USER_UUID), challengeId, createPostContent)
        val content = mapper.writeValueAsString(NewPost(createPostContent, challengeId))
        mvc.perform(
            MockMvcRequestBuilders.post("/posts/").contentType(MediaType.APPLICATION_JSON).content(content)
        ).andExpect(MockMvcResultMatchers.status().isConflict)
    }
}