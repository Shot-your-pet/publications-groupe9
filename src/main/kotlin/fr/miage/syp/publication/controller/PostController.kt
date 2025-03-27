package fr.miage.syp.publication.controller

import fr.miage.syp.publication.data.exception.ChallengeAlreadyCompletedException
import fr.miage.syp.publication.model.NewPost
import fr.miage.syp.publication.model.Post
import fr.miage.syp.publication.model.ResponseApi
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
    fun getPost(@PathVariable postId: Long): ResponseEntity<ResponseApi<Post?>> =
        postService.getPost(postId)?.let { ResponseEntity.ok(ResponseApi(it)) }
            ?: ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ResponseApi(
                    code = HttpStatus.NOT_FOUND.value(), message = "La publication demandé n'existe pas", content = null
                )
            )

    @PostMapping
    suspend fun insertPost(
        @RequestBody newPost: NewPost, authentication: Authentication, httpRequest: HttpServletRequest
    ): ResponseEntity<ResponseApi<Post?>> {
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


            ResponseEntity.created(createdUri).body(ResponseApi(draftedPost))
        }, onFailure = {
            if (it is ChallengeAlreadyCompletedException) {
                ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ResponseApi(
                        code = HttpStatus.CONFLICT.value(),
                        message = "Il existe deja une publication pour ce challenge",
                        content = null
                    )
                )
            } else {
                logger.error("error while inserting post", it)
                ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                    ResponseApi(
                        code = HttpStatus.SERVICE_UNAVAILABLE.value(),
                        message = "Veuillez réessayer ultérieurement",
                        content = null
                    )
                )
            }
        })
    }
}