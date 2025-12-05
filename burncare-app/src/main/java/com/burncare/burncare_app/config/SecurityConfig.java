package com.burncare.burncare_app.config;

import com.burncare.burncare_app.entities.Profession;
import com.burncare.burncare_app.entities.Role;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

        http.csrf(csrf -> csrf.disable());

        // Session stateless car on utilise des tokens JWT
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll() // Accessible à tous
                .requestMatchers("/api/auth/**").permitAll()

                // 1. Accès basés sur la PROFESSION (MEDECIN ou INFIRMIER)
                // On utilise Profession.MEDECIN.name() qui donne "MEDECIN"
                .requestMatchers("/medecin/**").hasRole(Profession.MEDECIN.name())
                .requestMatchers("/infirmier/**").hasRole(Profession.INFIRMIER.name())

                // 2. Accès pour les ADMINS (Gestion globale)
                .requestMatchers("/admin/**").hasRole(Role.ADMIN.name())

                // 3. (Optionnel) Si vous avez des endpoints pour n'importe quel professionnel de santé
                // Vous pouvez dire : "soit MEDECIN, soit INFIRMIER"
                .requestMatchers("/patients/**").hasAnyRole(Profession.MEDECIN.name(), Profession.INFIRMIER.name())

                // Tout le reste nécessite d'être authentifié
                .anyRequest().authenticated()
        );

        http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
        );

        return http.build();
    }

    // Convertisseur propre : Keycloak Roles -> Spring Security Authorities
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Récupère la map "realm_access" du token
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

            if (realmAccess == null || realmAccess.isEmpty()) {
                return List.of();
            }

            // Récupère la liste des rôles
            Collection<String> roles = (Collection<String>) realmAccess.getOrDefault("roles", List.of());

            // Transforme chaque rôle (ex: "MEDECIN") en Authority Spring (ex: "ROLE_MEDECIN")
            return roles.stream()
                    .map(roleName -> "ROLE_" + roleName) // Préfixe obligatoire pour .hasRole()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        });
        return converter;
    }
}