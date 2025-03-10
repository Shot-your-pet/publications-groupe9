package fr.miage.syp.publication.model

import com.fasterxml.jackson.annotation.JsonProperty

data class NewPost(
    val content: String?,
    @JsonProperty("challenge_id") val challengeId: Long,
    @JsonProperty("content_type") val imageId: Long,
)