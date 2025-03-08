package fr.miage.syp.publication.data.repository

import fr.miage.syp.publication.data.model.Post
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PostRepository : MongoRepository<Post, Long> {
    fun existsPostByAuthorIdAndChallengeId(authorId: UUID, challengeId: Long): Boolean
    fun findPostByImageIdNotNull(pageable: Pageable): List<Post>
}