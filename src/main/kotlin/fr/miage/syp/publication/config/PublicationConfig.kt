package fr.miage.syp.publication.config

import com.fasterxml.jackson.databind.ObjectMapper
import fr.miage.syp.publication.service.MessagingService
import jakarta.servlet.DispatcherType
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Declarables
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportRuntimeHints
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource


@Configuration
@ImportRuntimeHints(RegistrarHints::class)
@RegisterReflectionForBinding(
    MessagingService.PostMessage::class,
    MessagingService.EventContent::class,
    MessagingService.PublicationMessage::class,
    MessagingService.LikeMessage::class,
    MessagingService.DailyChallenge::class,
    MessagingService.Challenge::class,
)
class PublicationConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http.csrf { it.disable() }.authorizeHttpRequests { auth ->
            auth.requestMatchers(HttpMethod.OPTIONS, "/posts/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/posts/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/posts/").authenticated()
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll() // todo
                .anyRequest().denyAll()
        }.oauth2ResourceServer { oauth2ResourceServer -> oauth2ResourceServer.jwt(Customizer.withDefaults()) }
            .cors(Customizer.withDefaults()).build()

    @Bean
    protected fun corsConfigurationSource(): CorsConfigurationSource {
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", CorsConfiguration().applyPermitDefaultValues().apply {
            addAllowedMethod(HttpMethod.DELETE)
        })
        return source
    }

    @Bean
    fun timelineRabbitBindings(
        @Value("\${publish.timelineExchangeName}") timelineExchangeName: String,
        @Value("\${publish.timelineQueueName}") timelineQueueName: String,
        @Value("\${publish.timelineRoutingKey}") timelineRoutingKey: String,
        @Value("\${publish.getChallengeQueue}") getChallengeQueue: String,
    ): Declarables {
        val timeQueue = Queue(timelineQueueName, true)
        val challengeQueryQueue = Queue(getChallengeQueue, true)
        return if (timelineExchangeName != "") {
            val exchange = DirectExchange(timelineExchangeName, true, false)
            Declarables(
                timeQueue,
                exchange,
                BindingBuilder.bind(timeQueue).to(exchange).with(timelineRoutingKey),
                challengeQueryQueue,
            )
        } else {
            Declarables(
                timeQueue,
                challengeQueryQueue,
            )
        }
    }

    @Bean
    fun jsonMessageConverter(objectMapper: ObjectMapper): MessageConverter {
        return Jackson2JsonMessageConverter(objectMapper)
    }
}