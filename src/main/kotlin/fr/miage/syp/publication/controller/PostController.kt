package fr.miage.syp.publication.controller

import fr.miage.syp.publication.data.exception.ChallengeAlreadyCompletedException
import fr.miage.syp.publication.model.NewPost
import fr.miage.syp.publication.model.Post
import fr.miage.syp.publication.service.MessagingService
import fr.miage.syp.publication.service.PostService
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.amqp.AmqpException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.*


@RestController
@RequestMapping("/posts")
class PostController private constructor(
    private val postService: PostService,
    private val messageService: MessagingService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/{postId}")
    fun getPost(@PathVariable postId: Long): ResponseEntity<Post> =
        postService.getPost(postId)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    @PostMapping("/")
    suspend fun insertPost(
        @RequestBody newPost: NewPost,
        authentication: Authentication,
        httpRequest: HttpServletRequest
    ): ResponseEntity<Post> {
        val userId = UUID.fromString(authentication.name)
        return postService.createPostForUser(
            userId, newPost.content, newPost.imageId
        ).mapCatching { published ->
            try {
                messageService.sendPostToBus(published)
                published
            } catch (e: AmqpException) {
                postService.removePost(published.id)
                throw e
            }
        }.fold(onSuccess = { draftedPost ->
            val createdUri =
                ServletUriComponentsBuilder.fromRequestUri(httpRequest).path("/${draftedPost.id}").build().toUri()
            ResponseEntity.created(createdUri).body(draftedPost)
        }, onFailure = {
            if (it is ChallengeAlreadyCompletedException) {
                ResponseEntity.status(HttpStatus.CONFLICT).build()
            } else {
                logger.error("error while inserting post", it)
                ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
            }
        })
    }
}