package fr.miage.syp.publication.model

import com.fasterxml.jackson.annotation.JsonProperty

data class NewPost(
    val content: String?,
    @JsonProperty("image_id") val imageId: Long,
)