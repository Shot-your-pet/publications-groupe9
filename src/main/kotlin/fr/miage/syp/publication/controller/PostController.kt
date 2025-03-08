package fr.miage.syp.publication.controller

import fr.miage.syp.publication.data.exception.ChallengeAlreadyCompletedException
import fr.miage.syp.publication.model.DraftedPost
import fr.miage.syp.publication.model.NewPost
import fr.miage.syp.publication.service.PostService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.*


@RestController
@RequestMapping("/posts")
class PostController private constructor(
    private val postService: PostService,
) {
    @PostMapping("/")
    fun insertPost(@RequestBody newPost: NewPost, authentication: Authentication): ResponseEntity<DraftedPost> {
        val userId = UUID.fromString(authentication.name)
        return postService.createDraftedPostForUser(
            userId, newPost.challengeId, newPost.content
        ).fold(onSuccess = { draftedPostId ->
            val createdUri =
                ServletUriComponentsBuilder.fromCurrentContextPath().path("/posts/${draftedPostId}").build().toUri()
            ResponseEntity.created(createdUri).body(DraftedPost(draftedPostId))
        }, onFailure = {
            if (it is ChallengeAlreadyCompletedException) {
                ResponseEntity.status(HttpStatus.CONFLICT).build()
            } else {
                ResponseEntity.internalServerError().build()
            }
        })
    }
}