package fr.miage.syp.publication.service

import fr.miage.syp.publication.data.exception.ChallengeAlreadyCompletedException
import fr.miage.syp.publication.data.exception.PostAlreadyPublishedException
import fr.miage.syp.publication.data.exception.PostNotFoundException
import fr.miage.syp.publication.data.repository.PostRepository
import fr.miage.syp.publication.model.Post
import fr.miage.syp.publication.services.SnowflakeIdGenerator
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import fr.miage.syp.publication.data.model.Post as DataPost

@Service
class PostService private constructor(
    private val postRepository: PostRepository, private val snowflakeIdGenerator: SnowflakeIdGenerator
) {
    fun getPost(postId: Long): Post? = postRepository.findByIdOrNull(postId)?.let {
        if (it.imageId != null) {
            Post.PublishedPost(it.id, it.authorId, it.challengeId, it.content, it.publishedAt, it.imageId)
        } else {
            Post.DraftedPost(it.id, it.authorId, it.challengeId, it.content, it.publishedAt)
        }
    }

    fun getPosts(page: Int, maxSize: Int): List<Post.PublishedPost> = postRepository.findPostByImageIdNotNull(
        PageRequest.of(
            page, maxSize, Sort.by(Sort.Direction.DESC, "id")
        )
    ).map {
        val imageId = requireNotNull(it.imageId) {
            "imageId cannot be null"
        }
        Post.PublishedPost(it.id, it.authorId, it.challengeId, it.content, it.publishedAt, imageId)
    }

    fun createPostForUser(
        userId: UUID, challengeId: Long, content: String?, imageId: Long
    ): Result<Post.PublishedPost> {
        return if (postRepository.existsPostByAuthorIdAndChallengeId(userId, challengeId)) {
            Result.failure(ChallengeAlreadyCompletedException())
        } else {
            val nextId = snowflakeIdGenerator.nextId(0L)
            val publishedAt = Instant.now()
            postRepository.save(
                DataPost(nextId, userId, challengeId, content, publishedAt, imageId, emptyList())
            )
            Result.success(Post.PublishedPost(nextId, userId, challengeId, content, publishedAt, imageId))
        }
    }

    fun setImageIdForPost(postId: Long, imageId: Long): Result<Post.PublishedPost> {
        val post = postRepository.findByIdOrNull(postId)
        return if (post == null) {
            Result.failure(PostNotFoundException())
        } else if (post.imageId != null) {
            Result.failure(PostAlreadyPublishedException())
        } else {
            val newPost = postRepository.save(post.copy(imageId = imageId))
            Result.success(
                Post.PublishedPost(
                    newPost.id,
                    newPost.authorId,
                    newPost.challengeId,
                    newPost.content,
                    newPost.publishedAt,
                    imageId
                )
            )
        }
    }
}