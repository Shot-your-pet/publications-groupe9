package fr.miage.syp.publication.service

import org.springframework.stereotype.Component
import java.time.Instant

@Component
class TimeProvider {
    fun getNow() : Instant = Instant.now()
}