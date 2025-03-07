package fr.miage.syp.publication.service

import fr.miage.syp.publication.data.model.Post
import fr.miage.syp.publication.data.repository.PostRepository
import fr.miage.syp.publication.services.SnowflakeIdGenerator
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class PostService private constructor(
    private val postRepository: PostRepository, private val snowflakeIdGenerator: SnowflakeIdGenerator
) {
    fun createDraftedPostForUser(userId: UUID, content: String?): Long {
        val draftedPost = postRepository.save(
            Post(
                snowflakeIdGenerator.nextId(0L, 0L), userId, content, Instant.now(), null, true, emptyList()
            )
        )
        return draftedPost.id
    }
}