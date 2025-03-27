package fr.miage.syp.publication.model

import com.fasterxml.jackson.annotation.JsonProperty

data class ResponseApi<T>(
    @JsonProperty("contenu") val content: T,
    val code: Int = 200,
    val message: String? = null,
)