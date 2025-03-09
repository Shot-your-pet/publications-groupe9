package fr.miage.syp.publication.controller

import fr.miage.syp.publication.data.exception.ChallengeAlreadyCompletedException
import fr.miage.syp.publication.model.NewPost
import fr.miage.syp.publication.model.Post
import fr.miage.syp.publication.service.PostService
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
) {
    @GetMapping("/{postId}")
    fun getPost(@PathVariable postId: Long): ResponseEntity<Post> =
        postService.getPost(postId)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    @PostMapping("/")
    fun insertPost(@RequestBody newPost: NewPost, authentication: Authentication): ResponseEntity<Post.DraftedPost> {
        val userId = UUID.fromString(authentication.name)
        return postService.createDraftedPostForUser(
            userId, newPost.challengeId, newPost.content
        ).fold(onSuccess = { draftedPost ->
            val createdUri =
                ServletUriComponentsBuilder.fromCurrentContextPath().path("/posts/${draftedPost.id}").build().toUri()
            ResponseEntity.created(createdUri).body(draftedPost)
        }, onFailure = {
            if (it is ChallengeAlreadyCompletedException) {
                ResponseEntity.status(HttpStatus.CONFLICT).build()
            } else {
                ResponseEntity.internalServerError().build()
            }
        })
    }
}