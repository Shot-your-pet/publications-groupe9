package fr.miage.syp.publication.service

import fr.miage.syp.publication.data.exception.ChallengeAlreadyCompletedException
import fr.miage.syp.publication.data.exception.NoChallengeException
import fr.miage.syp.publication.data.repository.PostRepository
import fr.miage.syp.publication.model.Post
import fr.miage.syp.publication.services.SnowflakeIdGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*
import fr.miage.syp.publication.data.model.Post as DataPost

@Service
class PostService private constructor(
    private val postRepository: PostRepository,
    private val snowflakeIdGenerator: SnowflakeIdGenerator,
    private val messagingService: MessagingService,
    private val timeProvider: TimeProvider
) {
    fun getPost(postId: Long): Post? = postRepository.findByIdOrNull(postId)?.let {
        Post(it.id, it.authorId, it.challengeId, it.content, it.publishedAt, it.imageId)
    }

    fun getPosts(page: Int, maxSize: Int): List<Post> = postRepository.findPostByImageIdNotNull(
        PageRequest.of(
            page, maxSize, Sort.by(Sort.Direction.DESC, "id")
        )
    ).map {
        val imageId = requireNotNull(it.imageId) {
            "imageId cannot be null"
        }
        Post(it.id, it.authorId, it.challengeId, it.content, it.publishedAt, imageId)
    }

    suspend fun createPostForUser(
        userId: UUID, content: String?, imageId: Long
    ): Result<Post> = withContext(Dispatchers.IO) {
        val challengeId =
            messagingService.getCurrentChallenge()?.id ?: return@withContext Result.failure(NoChallengeException())
        if (postRepository.existsPostByAuthorIdAndChallengeId(userId, challengeId)) {
            Result.failure(ChallengeAlreadyCompletedException())
        } else {
            val nextId = snowflakeIdGenerator.nextId(0L)
            val publishedAt = timeProvider.getNow()
            postRepository.save(
                DataPost(nextId, userId, challengeId, content, publishedAt, imageId, emptyList())
            )
            Result.success(Post(nextId, userId, challengeId, content, publishedAt, imageId))
        }
    }

    fun removePost(postId: Long) {
        postRepository.deleteById(postId)
    }
}