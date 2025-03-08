package fr.miage.syp.publication.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import fr.miage.syp.publication.model.NewPost
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
        doReturn(newId).`when`(postService).createDraftedPostForUser(UUID.fromString(USER_UUID), 0, null)
        val content = mapper.writeValueAsString(NewPost(null))
        mvc.perform(
            MockMvcRequestBuilders.post("/posts/").contentType(MediaType.APPLICATION_JSON).content(content)
        ).andExpect(MockMvcResultMatchers.status().isCreated).andExpect(jsonPath("$.id").value(newId))
    }

    @Test
    @WithMockUser(username = USER_UUID)
    fun `test create post with authentication with content create object`() {
        val newId = random.nextLong()
        val createPostContent = "bar"
        val challengeId = random.nextLong()
        doReturn(newId).`when`(postService)
            .createDraftedPostForUser(UUID.fromString(USER_UUID), challengeId, createPostContent)
        val content = mapper.writeValueAsString(NewPost(createPostContent))
        mvc.perform(
            MockMvcRequestBuilders.post("/posts/").contentType(MediaType.APPLICATION_JSON).content(content)
        ).andExpect(MockMvcResultMatchers.status().isCreated).andExpect(jsonPath("$.id").value(newId))
    }
}