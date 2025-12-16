package com.burncare.burncare_app.config;

import com.burncare.burncare_app.entities.Profession;
import com.burncare.burncare_app.entities.Role;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // ✅ Import important
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthConverter) throws Exception {

        // SÉCURITÉ : On désactive CSRF car nous sommes en mode "Stateless" (API REST avec Token).
        // Ce n'est pas une faille de sécurité ici.
        http.csrf(csrf -> csrf.disable());

        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        http.authorizeHttpRequests(auth -> auth
                // ✅ 1. OUVERTURE TOTALE pour l'authentification
                // On autorise OPTIONS (pour CORS) et POST/PUT/GET sur /api/auth/**
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**", "/public/**").permitAll()

                // ✅ 2. ROUTES MÉDECIN
                .requestMatchers("/api/medecin/**").hasRole(Profession.MEDECIN.name())

                // ✅ 3. ROUTES USER (Profil, Password) - Authentifié requis
                .requestMatchers("/api/user/**").authenticated()

                // 4. AUTRES
                .requestMatchers("/api/infirmier/**").hasRole(Profession.INFIRMIER.name())
                .requestMatchers("/api/admin/**").hasRole(Role.ADMIN.name())

                .anyRequest().authenticated()
        );

        http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
        );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null || realmAccess.isEmpty()) {
                return List.of();
            }
            Collection<String> roles = (Collection<String>) realmAccess.getOrDefault("roles", List.of());
            return roles.stream()
                    .map(roleName -> "ROLE_" + roleName)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        });
        return converter;
    }
}