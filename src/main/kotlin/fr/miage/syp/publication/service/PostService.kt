package fr.miage.syp.publication.service

import fr.miage.syp.publication.data.repository.PostRepository
import fr.miage.syp.publication.services.SnowflakeIdGenerator
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import fr.miage.syp.publication.data.model.Post as DataPost
import fr.miage.syp.publication.model.Post as ModelPost

@Service
class PostService private constructor(
    private val postRepository: PostRepository, private val snowflakeIdGenerator: SnowflakeIdGenerator
) {
    fun getPosts(page: Int, maxSize: Int): List<ModelPost> = postRepository.findPostByImageIdNotNull(
        PageRequest.of(
            page, maxSize, Sort.by(Sort.Direction.DESC, "id")
        )
    ).map {
        val imageId = requireNotNull(it.imageId) {
            "imageId cannot be null"
        }
        ModelPost(it.id, it.authorId, it.content, it.publishedAt, imageId)
    }

    fun createDraftedPostForUser(userId: UUID, content: String?): Long {
        val draftedPost = postRepository.save(
            DataPost(
                snowflakeIdGenerator.nextId(0L, 0L), userId, content, Instant.now(), null, true, emptyList()
            )
        )
        return draftedPost.id
    }
}