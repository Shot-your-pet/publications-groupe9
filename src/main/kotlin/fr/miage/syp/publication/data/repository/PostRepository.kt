package fr.miage.syp.publication.data.repository

import fr.miage.syp.publication.data.model.Post
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface PostRepository : MongoRepository<Post, Long> {
    fun findPostByImageIdNotNull(pageable: Pageable): List<Post>
}