package fr.miage.syp.publication.config

import jakarta.servlet.DispatcherType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
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
}